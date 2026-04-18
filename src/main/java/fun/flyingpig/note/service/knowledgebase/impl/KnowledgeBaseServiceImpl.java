package fun.flyingpig.note.service.knowledgebase.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import fun.flyingpig.note.dto.KnowledgeBaseDTO;
import fun.flyingpig.note.entity.KnowledgeBase;
import fun.flyingpig.note.exception.BusinessException;
import fun.flyingpig.note.mapper.KnowledgeBaseMapper;
import fun.flyingpig.note.mapper.NoteGroupMapper;
import fun.flyingpig.note.mapper.NoteMapper;
import fun.flyingpig.note.qdrant.QdrantClient;
import fun.flyingpig.note.service.knowledgebase.KnowledgeBaseService;
import fun.flyingpig.note.service.vectorindex.INoteVectorIndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase>
        implements KnowledgeBaseService {

    private final NoteMapper noteMapper;
    private final NoteGroupMapper noteGroupMapper;
    private final INoteVectorIndexService noteVectorIndexService;
    private final QdrantClient qdrantClient;

    @Override
    public List<KnowledgeBase> getUserKnowledgeBases(Long userId) {
        LambdaQueryWrapper<KnowledgeBase> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KnowledgeBase::getUserId, userId)
                .orderByDesc(KnowledgeBase::getUpdateTime);
        return this.list(queryWrapper);
    }

    @Override
    public List<KnowledgeBase> searchKnowledgeBases(Long userId, String keyword) {
        LambdaQueryWrapper<KnowledgeBase> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KnowledgeBase::getUserId, userId)
                .and(wrapper -> wrapper.like(KnowledgeBase::getTitle, keyword)
                        .or()
                        .like(KnowledgeBase::getDescription, keyword))
                .orderByDesc(KnowledgeBase::getUpdateTime);
        return this.list(queryWrapper);
    }

    @Override
    public KnowledgeBase createKnowledgeBase(Long userId, KnowledgeBaseDTO dto) {
        KnowledgeBase knowledgeBase = KnowledgeBase.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .userId(userId)
                .indexUpdateTime(dto.getIndexUpdateTime())
                .build();
        this.save(knowledgeBase);
        return knowledgeBase;
    }

    @Override
    public void updateNoteCount(Long kbId, Integer delta) {
        this.baseMapper.updateNoteCount(kbId, delta);
    }

    @Override
    @Transactional
    public void deleteKnowledgeBaseCascade(Long kbId) {
        noteVectorIndexService.deleteByKnowledgeBaseId(kbId);
        qdrantClient.deleteByKnowledgeBaseId(kbId);
        noteMapper.deleteByKnowledgeBaseId(kbId);
        noteGroupMapper.deleteByKnowledgeBaseId(kbId);
        this.removeById(kbId);
    }

    @Override
    public KnowledgeBase updateKnowledgeBase(Long kbId, Long userId, KnowledgeBaseDTO dto) {
        KnowledgeBase knowledgeBase = this.getById(kbId);
        if (knowledgeBase == null) {
            throw new BusinessException(404, "知识库不存在");
        }
        if (!knowledgeBase.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该知识库");
        }
        knowledgeBase.setTitle(dto.getTitle());
        knowledgeBase.setDescription(dto.getDescription());
        this.updateById(knowledgeBase);
        return knowledgeBase;
    }

    @Override
    public void deleteKnowledgeBaseWithPermissionCheck(Long kbId, Long userId) {
        KnowledgeBase knowledgeBase = this.getById(kbId);
        if (knowledgeBase == null) {
            throw new BusinessException(404, "知识库不存在");
        }
        if (!knowledgeBase.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该知识库");
        }
        deleteKnowledgeBaseCascade(kbId);
    }
}
