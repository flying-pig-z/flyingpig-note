package fun.flyingpig.note.service.impl;

import fun.flyingpig.note.dto.IndexProgressDTO;
import fun.flyingpig.note.dto.RagAnswerDTO;
import fun.flyingpig.note.dto.RagQueryDTO;
import fun.flyingpig.note.dto.UpdateIndexDTO;
import fun.flyingpig.note.dto.UpdateIndexResultDTO;
import fun.flyingpig.note.service.RagService;
import fun.flyingpig.note.service.answer.RagAnswerService;
import fun.flyingpig.note.service.index.RagIndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

    private final RagAnswerService ragAnswerService;
    private final RagIndexService ragIndexService;

    @Override
    public RagAnswerDTO answer(RagQueryDTO queryDTO) {
        return ragAnswerService.answer(queryDTO);
    }

    @Override
    public RagAnswerDTO answerStream(RagQueryDTO queryDTO, Consumer<String> deltaConsumer) {
        return ragAnswerService.answerStream(queryDTO, deltaConsumer);
    }

    @Override
    public UpdateIndexResultDTO updateIndex(UpdateIndexDTO updateIndexDTO) {
        return ragIndexService.updateIndex(updateIndexDTO);
    }

    @Override
    public UpdateIndexResultDTO updateIndex(UpdateIndexDTO updateIndexDTO, Consumer<IndexProgressDTO> progressConsumer) {
        return ragIndexService.updateIndex(updateIndexDTO, progressConsumer);
    }

    @Override
    public UpdateIndexResultDTO forceUpdateIndex(UpdateIndexDTO updateIndexDTO) {
        return ragIndexService.forceUpdateIndex(updateIndexDTO);
    }

    @Override
    public UpdateIndexResultDTO forceUpdateIndex(UpdateIndexDTO updateIndexDTO, Consumer<IndexProgressDTO> progressConsumer) {
        return ragIndexService.forceUpdateIndex(updateIndexDTO, progressConsumer);
    }
}
