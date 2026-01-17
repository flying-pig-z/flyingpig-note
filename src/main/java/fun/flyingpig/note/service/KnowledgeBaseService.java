package fun.flyingpig.note.service;

import com.baomidou.mybatisplus.extension.service.IService;
import fun.flyingpig.note.dto.KnowledgeBaseDTO;
import fun.flyingpig.note.entity.KnowledgeBase;

import java.util.List;

public interface KnowledgeBaseService extends IService<KnowledgeBase> {

    /**
     * 获取用户的知识库列表
     */
    List<KnowledgeBase> getUserKnowledgeBases(Long userId);

    /**
     * 搜索知识库
     */
    List<KnowledgeBase> searchKnowledgeBases(Long userId, String keyword);

    /**
     * 创建知识库
     */
    KnowledgeBase createKnowledgeBase(Long userId, KnowledgeBaseDTO dto);

    /**
     * 更新知识库笔记数量
     */
    void updateNoteCount(Long kbId, Integer delta);
    
    /**
     * 删除知识库及其关联的笔记和索引
     */
    void deleteKnowledgeBaseCascade(Long kbId);
    
    /**
     * 更新知识库（包含权限检查）
     */
    KnowledgeBase updateKnowledgeBase(Long kbId, Long userId, KnowledgeBaseDTO dto);
    
    /**
     * 删除知识库（包含权限检查）
     */
    void deleteKnowledgeBaseWithPermissionCheck(Long kbId, Long userId);
}