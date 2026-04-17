package fun.flyingpig.note.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import fun.flyingpig.note.dto.NoteIndexLatestUpdateDTO;
import fun.flyingpig.note.entity.NoteVectorIndex;
import fun.flyingpig.note.mapper.NoteVectorIndexMapper;
import fun.flyingpig.note.service.INoteVectorIndexService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 绗旇鍚戦噺绱㈠紩鏈嶅姟瀹炵幇绫?
 */
@Service
public class NoteVectorIndexServiceImpl extends ServiceImpl<NoteVectorIndexMapper, NoteVectorIndex> implements INoteVectorIndexService {

    @Override
    public Map<Long, LocalDateTime> getLatestUpdateTimeMapByKnowledgeBaseId(Long knowledgeBaseId) {
        List<NoteIndexLatestUpdateDTO> latestUpdates =
                this.baseMapper.selectLatestUpdateTimesByKnowledgeBaseId(knowledgeBaseId);
        if (latestUpdates == null || latestUpdates.isEmpty()) {
            return Collections.emptyMap();
        }

        return latestUpdates.stream().collect(Collectors.toMap(
                NoteIndexLatestUpdateDTO::getNoteId,
                NoteIndexLatestUpdateDTO::getLatestUpdateTime
        ));
    }

    @Override
    public void deleteByKnowledgeBaseId(Long knowledgeBaseId) {
        this.baseMapper.deleteByKnowledgeBaseId(knowledgeBaseId);
    }
}
