package fun.flyingpig.note.service.answer;

import fun.flyingpig.note.config.RagProperties;
import fun.flyingpig.note.dto.QdrantSearchResult;
import fun.flyingpig.note.dto.RagAnswerDTO;
import fun.flyingpig.note.dto.RagQueryDTO;
import fun.flyingpig.note.entity.Note;
import fun.flyingpig.note.mapper.NoteMapper;
import fun.flyingpig.note.service.qdrant.QdrantService;
import fun.flyingpig.note.util.ChatUtil;
import fun.flyingpig.note.util.embedding.ZhiPuEmbedding3Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 负责执行单次 RAG 问答的完整流程。
 *
 * <p>处理链路包括：
 * 1. 规范化前端传入的临时会话历史。
 * 2. 必要时将当前追问改写成可独立检索的问题。
 * 3. 调用 Qdrant 执行向量检索。
 * 4. 将检索到的笔记分片组装成模型上下文。
 * 5. 结合历史消息和检索上下文生成最终回答。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagAnswerService {

    private static final int MAX_HISTORY_MESSAGES = 10;
    private static final String EMPTY_RESULT_MESSAGE = "抱歉，指定的知识库中暂无可用索引数据，请先更新索引。";

    private final ZhiPuEmbedding3Util zhiPuEmbedding3Util;
    private final RagProperties ragProperties;
    private final ChatUtil chatUtil;
    private final QdrantService qdrantService;
    private final NoteMapper noteMapper;

    /**
     * 普通问答入口，一次性返回完整回答。
     */
    public RagAnswerDTO answer(RagQueryDTO queryDTO) {
        long startTime = System.currentTimeMillis();
        PreparedAnswerContext preparedContext = prepareAnswerContext(queryDTO);

        if (preparedContext.hasFallbackAnswer()) {
            return buildAnswerResult(preparedContext.fallbackAnswer(), preparedContext.relevantDocuments());
        }

        String answer = chatUtil.generateAnswer(
                preparedContext.originalQuestion(),
                preparedContext.standaloneQuestion(),
                preparedContext.context(),
                preparedContext.history()
        );
        log.info("RAG普通问答完成，耗时: {}ms", System.currentTimeMillis() - startTime);
        return buildAnswerResult(answer, preparedContext.relevantDocuments());
    }

    /**
     * 流式问答入口，边生成边把增量文本回调给上层。
     */
    public RagAnswerDTO answerStream(RagQueryDTO queryDTO, Consumer<String> deltaConsumer) {
        long startTime = System.currentTimeMillis();
        Consumer<String> safeDeltaConsumer = deltaConsumer != null ? deltaConsumer : chunk -> { };
        PreparedAnswerContext preparedContext = prepareAnswerContext(queryDTO);

        if (preparedContext.hasFallbackAnswer()) {
            safeDeltaConsumer.accept(preparedContext.fallbackAnswer());
            return buildAnswerResult(preparedContext.fallbackAnswer(), preparedContext.relevantDocuments());
        }

        String answer = chatUtil.streamAnswer(
                preparedContext.originalQuestion(),
                preparedContext.standaloneQuestion(),
                preparedContext.context(),
                preparedContext.history(),
                safeDeltaConsumer
        );
        log.info("RAG流式问答完成，耗时: {}ms", System.currentTimeMillis() - startTime);
        return buildAnswerResult(answer, preparedContext.relevantDocuments());
    }

    /**
     * 在真正调用模型回答之前，先完成检索侧的准备工作。
     */
    private PreparedAnswerContext prepareAnswerContext(RagQueryDTO queryDTO) {
        List<RagQueryDTO.HistoryMessage> history = normalizeHistory(queryDTO.getHistory());
        log.info(
                "RAG prepare started: question={}, knowledgeBaseIds={}, historySize={}",
                queryDTO.getQuestion(),
                queryDTO.getKnowledgeBaseIds(),
                history.size()
        );

        // 将“这个呢”“继续上一个”这类追问改写成完整问题，提升检索质量。
        String standaloneQuestion = resolveStandaloneQuestion(queryDTO.getQuestion(), history);

        long embeddingStartTime = System.currentTimeMillis();
        // 检索阶段使用改写后的问题生成向量。
        List<Double> queryEmbedding = zhiPuEmbedding3Util.getEmbedding(standaloneQuestion);
        if (queryEmbedding == null || queryEmbedding.isEmpty()) {
            throw new RuntimeException("获取问题向量失败");
        }
        log.info("问题向量化完成，耗时: {}ms", System.currentTimeMillis() - embeddingStartTime);

        long qdrantSearchStartTime = System.currentTimeMillis();
        // 根据问题向量和知识库范围执行向量检索。
        List<QdrantSearchResult> searchResults = qdrantService.search(
                queryEmbedding,
                queryDTO.getKnowledgeBaseIds(),
                ragProperties.getTopK()
        );
        log.info(
                "Qdrant检索完成，耗时: {}ms，结果数: {}",
                System.currentTimeMillis() - qdrantSearchStartTime,
                searchResults.size()
        );

        // 检索不到内容时直接返回兜底文案，不再继续调用大模型。
        if (searchResults.isEmpty()) {
            return new PreparedAnswerContext(
                    queryDTO.getQuestion(),
                    standaloneQuestion,
                    history,
                    "",
                    new ArrayList<>(),
                    EMPTY_RESULT_MESSAGE
            );
        }

        long contextStartTime = System.currentTimeMillis();
        // 先收集命中的笔记 ID，后面再批量回表查询标题等元数据。
        Set<Long> noteIds = searchResults.stream()
                .map(QdrantSearchResult::getNoteId)
                .collect(Collectors.toSet());

        // 批量查出命中的笔记标题，用于构造引用来源。
        Map<Long, Note> noteMap = new HashMap<>();
        if (!noteIds.isEmpty()) {
            List<Note> notes = noteMapper.selectBatchIds(noteIds);
            noteMap = notes.stream().collect(Collectors.toMap(Note::getId, note -> note));
        }

        // 这里开始同时构造模型上下文和前端引用列表。
        StringBuilder contextBuilder = new StringBuilder();
        List<RagAnswerDTO.RelevantDocument> relevantDocs = new ArrayList<>();
        // 同时准备两份结果：
        // 1. 提供给模型的扁平化上下文字符串。
        // 2. 返回给前端展示的结构化引用列表。
        for (QdrantSearchResult searchResult : searchResults) {
            Note note = noteMap.get(searchResult.getNoteId());
            String noteTitle = note != null ? note.getTitle() : "未知笔记";

            contextBuilder.append("【来源: ").append(noteTitle).append("】\n");
            contextBuilder.append(searchResult.getChunkContent()).append("\n\n");

            RagAnswerDTO.RelevantDocument doc = new RagAnswerDTO.RelevantDocument();
            doc.setNoteId(searchResult.getNoteId());
            doc.setNoteTitle(noteTitle);
            doc.setContent(searchResult.getChunkContent());
            doc.setScore(searchResult.getScore());
            relevantDocs.add(doc);
        }
        log.info("上下文组装完成，耗时: {}ms", System.currentTimeMillis() - contextStartTime);

        return new PreparedAnswerContext(
                queryDTO.getQuestion(),
                standaloneQuestion,
                history,
                contextBuilder.toString(),
                relevantDocs,
                null
        );
    }

    /**
     * “这个呢”“那上一个方案呢”这类追问本身不适合直接检索，
     * 先改写成信息完整的问题，再生成向量。
     */
    private String resolveStandaloneQuestion(String question, List<RagQueryDTO.HistoryMessage> history) {
        // 没有历史消息时，当前问题本身就作为检索输入。
        if (history.isEmpty()) {
            return question;
        }

        // 先调用改写能力补全指代信息，再把结果作为检索输入。
        String rewrittenQuestion = chatUtil.rewriteQuestion(question, history);
        if (!StringUtils.hasText(rewrittenQuestion)) {
            return question;
        }

        String normalizedQuestion = rewrittenQuestion.trim();
        if (!normalizedQuestion.equals(question)) {
            log.info("Question rewritten from [{}] to [{}]", question, normalizedQuestion);
        }
        return normalizedQuestion;
    }

    /**
     * 只保留合法的 user/assistant 消息，并限制历史窗口长度，
     * 避免异常角色混入和 prompt 无限制膨胀。
     */
    private List<RagQueryDTO.HistoryMessage> normalizeHistory(List<RagQueryDTO.HistoryMessage> history) {
        if (history == null || history.isEmpty()) {
            return Collections.emptyList();
        }

        List<RagQueryDTO.HistoryMessage> normalizedHistory = new ArrayList<>();
        // 逐条过滤历史消息，避免把空消息或非法角色带入模型上下文。
        for (RagQueryDTO.HistoryMessage message : history) {
            if (message == null || !StringUtils.hasText(message.getContent())) {
                continue;
            }

            String role = normalizeRole(message.getRole());
            if (!"user".equals(role) && !"assistant".equals(role)) {
                continue;
            }

            RagQueryDTO.HistoryMessage historyMessage = new RagQueryDTO.HistoryMessage();
            historyMessage.setRole(role);
            historyMessage.setContent(message.getContent().trim());
            normalizedHistory.add(historyMessage);
        }

        // 未超过窗口上限时直接返回全部有效历史。
        if (normalizedHistory.size() <= MAX_HISTORY_MESSAGES) {
            return normalizedHistory;
        }

        // 超出窗口时只保留最近几条，控制 prompt 长度。
        return new ArrayList<>(
                normalizedHistory.subList(normalizedHistory.size() - MAX_HISTORY_MESSAGES, normalizedHistory.size())
        );
    }

    /**
     * 统一角色字段格式，避免前端传参大小写不一致导致匹配失败。
     */
    private String normalizeRole(String role) {
        if (!StringUtils.hasText(role)) {
            return "";
        }
        return role.trim().toLowerCase(Locale.ROOT);
    }

    private RagAnswerDTO buildAnswerResult(String answer, List<RagAnswerDTO.RelevantDocument> relevantDocuments) {
        RagAnswerDTO result = new RagAnswerDTO();
        result.setAnswer(answer);
        result.setRelevantDocuments(relevantDocuments);
        return result;
    }

    private record PreparedAnswerContext(
            String originalQuestion,
            String standaloneQuestion,
            List<RagQueryDTO.HistoryMessage> history,
            String context,
            List<RagAnswerDTO.RelevantDocument> relevantDocuments,
            String fallbackAnswer
    ) {
        private boolean hasFallbackAnswer() {
            return StringUtils.hasText(fallbackAnswer);
        }
    }
}
