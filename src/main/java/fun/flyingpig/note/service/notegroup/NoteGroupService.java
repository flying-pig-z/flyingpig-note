package fun.flyingpig.note.service.notegroup;

import com.baomidou.mybatisplus.extension.service.IService;
import fun.flyingpig.note.dto.NoteGroupDTO;
import fun.flyingpig.note.entity.NoteGroup;

import java.util.List;

public interface NoteGroupService extends IService<NoteGroup> {

    List<NoteGroup> getKnowledgeBaseGroups(Long knowledgeBaseId, Long userId);

    List<NoteGroup> getKnowledgeBaseGroups(Long knowledgeBaseId);

    NoteGroup createGroup(Long userId, NoteGroupDTO dto);

    NoteGroup createGroup(NoteGroupDTO dto);

    NoteGroup updateGroup(Long userId, Long id, NoteGroupDTO dto);

    NoteGroup updateGroup(Long id, NoteGroupDTO dto);

    NoteGroup moveGroup(Long userId, Long id, Long parentId);

    NoteGroup moveGroup(Long id, Long parentId);

    boolean deleteGroupCascade(Long userId, Long id);

    boolean deleteGroupCascade(Long id);
}
