package fun.flyingpig.note.service.notegroup;

import com.baomidou.mybatisplus.extension.service.IService;
import fun.flyingpig.note.dto.NoteGroupDTO;
import fun.flyingpig.note.entity.NoteGroup;

import java.util.List;

public interface NoteGroupService extends IService<NoteGroup> {

    List<NoteGroup> getKnowledgeBaseGroups(Long knowledgeBaseId);

    NoteGroup createGroup(NoteGroupDTO dto);

    NoteGroup updateGroup(Long id, NoteGroupDTO dto);

    NoteGroup moveGroup(Long id, Long parentId);

    boolean deleteGroupCascade(Long id);
}
