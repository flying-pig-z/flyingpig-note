package fun.flyingpig.note.service.notegroup.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import fun.flyingpig.note.dto.NoteGroupDTO;
import fun.flyingpig.note.entity.Note;
import fun.flyingpig.note.entity.NoteGroup;
import fun.flyingpig.note.entity.NoteVectorIndex;
import fun.flyingpig.note.exception.BusinessException;
import fun.flyingpig.note.mapper.NoteGroupMapper;
import fun.flyingpig.note.mapper.NoteMapper;
import fun.flyingpig.note.service.knowledgebase.KnowledgeBaseService;
import fun.flyingpig.note.service.notegroup.NoteGroupService;
import fun.flyingpig.note.service.vectorindex.INoteVectorIndexService;
import fun.flyingpig.note.qdrant.QdrantClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class NoteGroupServiceImpl extends ServiceImpl<NoteGroupMapper, NoteGroup> implements NoteGroupService {

    @Autowired
    private NoteMapper noteMapper;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private INoteVectorIndexService noteVectorIndexService;

    @Autowired
    private QdrantClient qdrantClient;

    @Override
    public List<NoteGroup> getKnowledgeBaseGroups(Long knowledgeBaseId) {
        LambdaQueryWrapper<NoteGroup> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NoteGroup::getKnowledgeBaseId, knowledgeBaseId)
                .orderByAsc(NoteGroup::getCreateTime, NoteGroup::getId);
        return this.list(queryWrapper);
    }

    @Override
    public NoteGroup createGroup(NoteGroupDTO dto) {
        if (dto.getParentId() != null) {
            NoteGroup parent = this.getById(dto.getParentId());
            if (parent == null || !parent.getKnowledgeBaseId().equals(dto.getKnowledgeBaseId())) {
                throw new BusinessException(400, "父分组不存在或不在同一知识库");
            }
        }

        NoteGroup group = NoteGroup.builder()
                .name(dto.getName().trim())
                .knowledgeBaseId(dto.getKnowledgeBaseId())
                .parentId(dto.getParentId())
                .build();
        this.save(group);
        return group;
    }

    @Override
    public NoteGroup updateGroup(Long id, NoteGroupDTO dto) {
        NoteGroup group = this.getById(id);
        if (group != null) {
            if (!group.getKnowledgeBaseId().equals(dto.getKnowledgeBaseId())) {
                throw new BusinessException(400, "分组不能切换到其他知识库");
            }
            group.setName(dto.getName().trim());
            this.updateById(group);
        }
        return group;
    }

    @Override
    public NoteGroup moveGroup(Long id, Long parentId) {
        NoteGroup group = this.getById(id);
        if (group == null) {
            return null;
        }
        if (Objects.equals(group.getParentId(), parentId)) {
            return group;
        }
        if (parentId != null) {
            if (parentId.equals(id)) {
                return group;
            }
            NoteGroup parent = this.getById(parentId);
            if (parent == null || !parent.getKnowledgeBaseId().equals(group.getKnowledgeBaseId())) {
                throw new BusinessException(400, "目标分组不存在或不在同一知识库");
            }

            List<NoteGroup> groups = getKnowledgeBaseGroups(group.getKnowledgeBaseId());
            Map<Long, List<Long>> childrenMap = buildChildrenMap(groups);
            if (isDescendant(id, parentId, childrenMap)) {
                throw new BusinessException(400, "不能移动到子分组");
            }
        }

        this.baseMapper.updateParentId(id, parentId);
        group.setParentId(parentId);
        return group;
    }

    @Override
    @Transactional
    public boolean deleteGroupCascade(Long id) {
        NoteGroup group = this.getById(id);
        if (group == null) {
            return false;
        }

        List<NoteGroup> groups = getKnowledgeBaseGroups(group.getKnowledgeBaseId());
        Map<Long, List<Long>> childrenMap = buildChildrenMap(groups);
        Set<Long> groupIdsToDelete = collectDescendantGroupIds(id, childrenMap);

        LambdaQueryWrapper<Note> noteQueryWrapper = new LambdaQueryWrapper<>();
        noteQueryWrapper.in(Note::getGroupId, groupIdsToDelete);
        List<Note> notesToDelete = noteMapper.selectList(noteQueryWrapper);

        if (!notesToDelete.isEmpty()) {
            List<Long> noteIdsToDelete = notesToDelete.stream()
                    .map(Note::getId)
                    .collect(Collectors.toList());

            LambdaQueryWrapper<NoteVectorIndex> vectorIndexDeleteWrapper = new LambdaQueryWrapper<>();
            vectorIndexDeleteWrapper.in(NoteVectorIndex::getNoteId, noteIdsToDelete);
            noteVectorIndexService.remove(vectorIndexDeleteWrapper);

            noteIdsToDelete.forEach(qdrantClient::deleteByNoteId);

            LambdaQueryWrapper<Note> noteDeleteWrapper = new LambdaQueryWrapper<>();
            noteDeleteWrapper.in(Note::getId, noteIdsToDelete);
            noteMapper.delete(noteDeleteWrapper);

            knowledgeBaseService.updateNoteCount(group.getKnowledgeBaseId(), -noteIdsToDelete.size());
        }

        return this.removeByIds(groupIdsToDelete);
    }

    private Map<Long, List<Long>> buildChildrenMap(List<NoteGroup> groups) {
        Map<Long, List<Long>> childrenMap = new HashMap<>();
        for (NoteGroup group : groups) {
            if (group.getParentId() != null) {
                childrenMap.computeIfAbsent(group.getParentId(), key -> new ArrayList<>()).add(group.getId());
            }
        }
        return childrenMap;
    }

    private boolean isDescendant(Long groupId, Long potentialDescendantId, Map<Long, List<Long>> childrenMap) {
        Deque<Long> stack = new ArrayDeque<>();
        stack.push(groupId);
        while (!stack.isEmpty()) {
            Long currentId = stack.pop();
            List<Long> children = childrenMap.getOrDefault(currentId, Collections.emptyList());
            for (Long childId : children) {
                if (childId.equals(potentialDescendantId)) {
                    return true;
                }
                stack.push(childId);
            }
        }
        return false;
    }

    private Set<Long> collectDescendantGroupIds(Long rootGroupId, Map<Long, List<Long>> childrenMap) {
        Set<Long> groupIds = new LinkedHashSet<>();
        Deque<Long> stack = new ArrayDeque<>();
        stack.push(rootGroupId);

        while (!stack.isEmpty()) {
            Long currentId = stack.pop();
            if (!groupIds.add(currentId)) {
                continue;
            }

            for (Long childId : childrenMap.getOrDefault(currentId, Collections.emptyList())) {
                stack.push(childId);
            }
        }

        return groupIds;
    }
}
