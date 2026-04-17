package fun.flyingpig.note.service.rag;

import fun.flyingpig.note.dto.RagAnswerDTO;
import fun.flyingpig.note.dto.RagQueryDTO;

import java.util.function.Consumer;

public interface RagAnswerService {

    RagAnswerDTO answer(RagQueryDTO queryDTO);

    RagAnswerDTO answerStream(RagQueryDTO queryDTO, Consumer<String> deltaConsumer);
}
