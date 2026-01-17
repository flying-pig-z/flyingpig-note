package fun.flyingpig.note.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import fun.flyingpig.note.dto.KnowledgeBaseDTO;
import fun.flyingpig.note.entity.KnowledgeBase;
import fun.flyingpig.note.mapper.KnowledgeBaseMapper;
import fun.flyingpig.note.mapper.NoteMapper;
import fun.flyingpig.note.service.INoteVectorIndexService;
import fun.flyingpig.note.service.KnowledgeBaseService;
import fun.flyingpig.note.service.QdrantService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase>
        implements KnowledgeBaseService {
    
    @Autowired
    private NoteMapper noteMapper;
    
    @Autowired
    private INoteVectorIndexService noteVectorIndexService;

    @Autowired
    QdrantService qdrantService;

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
        // 索引创建时会设置索引更新时间
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
        // 删除该知识库下的所有向量索引
        noteVectorIndexService.deleteByKnowledgeBaseId(kbId);
        qdrantService.deleteByKnowledgeBaseId(kbId);
        // 删除该知识库下的所有笔记
        noteMapper.deleteByKnowledgeBaseId(kbId);
        // 删除知识库本身
        this.removeById(kbId);
    }

    @Override
    public KnowledgeBase updateKnowledgeBase(Long kbId, Long userId, KnowledgeBaseDTO dto) {
        KnowledgeBase knowledgeBase = this.getById(kbId);
        if (knowledgeBase == null) {
            throw new IllegalArgumentException("知识库不存在");
        }
        if (!knowledgeBase.getUserId().equals(userId)) {
            throw new SecurityException("无权限操作");
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
            throw new IllegalArgumentException("知识库不存在");
        }
        if (!knowledgeBase.getUserId().equals(userId)) {
            throw new SecurityException("无权限操作");
        }
        deleteKnowledgeBaseCascade(kbId);
    }
}