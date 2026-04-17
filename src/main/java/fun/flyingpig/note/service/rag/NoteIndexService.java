package fun.flyingpig.note.service.rag;

import fun.flyingpig.note.entity.Note;

public interface NoteIndexService {

    void writeIndexForNote(Note note);
}
