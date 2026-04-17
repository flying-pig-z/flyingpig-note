package fun.flyingpig.note.service;

import fun.flyingpig.note.dto.IndexProgressDTO;
import fun.flyingpig.note.dto.RagAnswerDTO;
import fun.flyingpig.note.dto.RagQueryDTO;
import fun.flyingpig.note.dto.UpdateIndexDTO;
import fun.flyingpig.note.dto.UpdateIndexResultDTO;

import java.util.function.Consumer;

public interface RagService {

    RagAnswerDTO answer(RagQueryDTO queryDTO);

    RagAnswerDTO answerStream(RagQueryDTO queryDTO, Consumer<String> deltaConsumer);

    UpdateIndexResultDTO updateIndex(UpdateIndexDTO updateIndexDTO);

    UpdateIndexResultDTO updateIndex(UpdateIndexDTO updateIndexDTO, Consumer<IndexProgressDTO> progressConsumer);

    UpdateIndexResultDTO forceUpdateIndex(UpdateIndexDTO updateIndexDTO);

    UpdateIndexResultDTO forceUpdateIndex(UpdateIndexDTO updateIndexDTO, Consumer<IndexProgressDTO> progressConsumer);
}
