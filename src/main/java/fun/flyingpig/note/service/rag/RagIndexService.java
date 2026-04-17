package fun.flyingpig.note.service.rag;

import fun.flyingpig.note.dto.IndexProgressDTO;
import fun.flyingpig.note.dto.UpdateIndexDTO;
import fun.flyingpig.note.dto.UpdateIndexResultDTO;

import java.util.function.Consumer;

public interface RagIndexService {

    UpdateIndexResultDTO updateIndex(UpdateIndexDTO updateIndexDTO);

    UpdateIndexResultDTO updateIndex(UpdateIndexDTO updateIndexDTO, Consumer<IndexProgressDTO> progressConsumer);

    UpdateIndexResultDTO forceUpdateIndex(UpdateIndexDTO updateIndexDTO);

    UpdateIndexResultDTO forceUpdateIndex(UpdateIndexDTO updateIndexDTO, Consumer<IndexProgressDTO> progressConsumer);
}
