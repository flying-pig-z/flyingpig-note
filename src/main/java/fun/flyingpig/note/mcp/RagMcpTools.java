package fun.flyingpig.note.mcp;

import fun.flyingpig.note.dto.KnowledgeBaseDTO;
import fun.flyingpig.note.dto.NoteDTO;
import fun.flyingpig.note.dto.NoteGroupDTO;
import fun.flyingpig.note.dto.OperationResultDTO;
import fun.flyingpig.note.entity.KnowledgeBase;
import fun.flyingpig.note.entity.Note;
import fun.flyingpig.note.entity.NoteGroup;
import fun.flyingpig.note.service.knowledgebase.KnowledgeBaseService;
import fun.flyingpig.note.service.note.NoteService;
import fun.flyingpig.note.service.notegroup.NoteGroupService;
import fun.flyingpig.note.service.security.NoteSecurityService;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RagMcpTools {

    private final NoteSecurityService noteSecurityService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final NoteService noteService;
    private final NoteGroupService noteGroupService;

    @McpTool(name = "list_knowledge_bases", description = "列出当前用户拥有的所有知识库。")
    public List<KnowledgeBase> listKnowledgeBases(McpSyncServerExchange exchange) {
        Long userId = noteSecurityService.requireUserIdFromMcpExchange(exchange);
        log.info("MCP 工具调用: list_knowledge_bases, userId={}", userId);
        return knowledgeBaseService.getUserKnowledgeBases(userId);
    }

    @McpTool(name = "get_knowledge_base", description = "获取单个知识库的详细信息。")
    public KnowledgeBase getKnowledgeBase(
            McpSyncServerExchange exchange,
            @McpToolParam(description = "知识库ID", required = true) Long knowledgeBaseId
    ) {
        Long userId = noteSecurityService.requireUserIdFromMcpExchange(exchange);
        log.info("MCP 工具调用: get_knowledge_base, userId={}, knowledgeBaseId={}", userId, knowledgeBaseId);
        return noteSecurityService.requireKnowledgeBaseOwner(knowledgeBaseId, userId);
    }

    @McpTool(name = "search_knowledge_bases", description = "按关键词搜索知识库。")
    public List<KnowledgeBase> searchKnowledgeBases(
            McpSyncServerExchange exchange,
            @McpToolParam(description = "搜索关键词", required = true) String keyword
    ) {
        Long userId = noteSecurityService.requireUserIdFromMcpExchange(exchange);
        log.info("MCP 工具调用: search_knowledge_bases, userId={}, keyword={}", userId, keyword);
        return knowledgeBaseService.searchKnowledgeBases(userId, keyword);
    }

    @McpTool(name = "create_knowledge_base", description = "创建一个新的知识库。")
    public KnowledgeBase createKnowledgeBase(
            McpSyncServerExchange exchange,
            @McpToolParam(description = "知识库标题", required = true) String title,
            @McpToolParam(description = "知识库描述", required = false) String description
    ) {
        Long userId = noteSecurityService.requireUserIdFromMcpExchange(exchange);
        log.info("MCP 工具调用: create_knowledge_base, userId={}, title={}", userId, title);
        return knowledgeBaseService.createKnowledgeBase(userId, new KnowledgeBaseDTO(title, description, null));
    }

    @McpTool(name = "update_knowledge_base", description = "更新知识库的标题或描述。")
    public KnowledgeBase updateKnowledgeBase(
            McpSyncServerExchange exchange,
            @McpToolParam(description = "知识库ID", required = true) Long knowledgeBaseId,
            @McpToolParam(description = "知识库标题", required = true) String title,
            @McpToolParam(description = "知识库描述", required = false) String description
    ) {
        Long userId = noteSecurityService.requireUserIdFromMcpExchange(exchange);
        log.info("MCP 工具调用: update_knowledge_base, userId={}, knowledgeBaseId={}", userId, knowledgeBaseId);
        return knowledgeBaseService.updateKnowledgeBase(
                knowledgeBaseId,
                userId,
                new KnowledgeBaseDTO(title, description, null)
        );
    }

    @McpTool(name = "delete_knowledge_base", description = "删除知识库及其关联的所有笔记。")
    public OperationResultDTO deleteKnowledgeBase(
            McpSyncServerExchange exchange,
            @McpToolParam(description = "知识库ID", required = true) Long knowledgeBaseId
    ) {
        Long userId = noteSecurityService.requireUserIdFromMcpExchange(exchange);
        log.info("MCP 工具调用: delete_knowledge_base, userId={}, knowledgeBaseId={}", userId, knowledgeBaseId);
        knowledgeBaseService.deleteKnowledgeBaseWithPermissionCheck(knowledgeBaseId, userId);
        return OperationResultDTO.success("知识库已删除。", "knowledge_base", knowledgeBaseId);
    }

    @McpTool(name = "list_notes", description = "列出知识库下的笔记摘要。")
    public List<Note> listNotes(
            McpSyncServerExchange exchange,
            @McpToolParam(description = "知识库ID", required = true) Long knowledgeBaseId
    ) {
        Long userId = noteSecurityService.requireUserIdFromMcpExchange(exchange);
        log.info("MCP 工具调用: list_notes, userId={}, knowledgeBaseId={}", userId, knowledgeBaseId);
        return noteService.getKnowledgeBaseNotes(knowledgeBaseId, userId);
    }

    @McpTool(name = "get_note", description = "获取笔记的完整内容。")
    public Note getNote(
            McpSyncServerExchange exchange,
            @McpToolParam(description = "笔记ID", required = true) Long noteId
    ) {
        Long userId = noteSecurityService.requireUserIdFromMcpExchange(exchange);
        log.info("MCP 工具调用: get_note, userId={}, noteId={}", userId, noteId);
        return noteService.getOwnedNoteById(noteId, userId);
    }

    @McpTool(name = "search_notes", description = "在知识库中搜索笔记。")
    public List<Note> searchNotes(
            McpSyncServerExchange exchange,
            @McpToolParam(description = "知识库ID", required = true) Long knowledgeBaseId,
            @McpToolParam(description = "搜索关键词", required = true) String keyword
    ) {
        Long userId = noteSecurityService.requireUserIdFromMcpExchange(exchange);
        log.info("MCP 工具调用: search_notes, userId={}, knowledgeBaseId={}", userId, knowledgeBaseId);
        return noteService.searchNotes(knowledgeBaseId, keyword, userId);
    }

    @McpTool(name = "create_note", description = "在知识库中创建笔记。")
    public Note createNote(
            McpSyncServerExchange exchange,
            @McpToolParam(description = "知识库ID", required = true) Long knowledgeBaseId,
            @McpToolParam(description = "笔记标题", required = true) String title,
            @McpToolParam(description = "笔记内容", required = false) String content,
            @McpToolParam(description = "分组ID，可选", required = false) Long groupId
    ) {
        Long userId = noteSecurityService.requireUserIdFromMcpExchange(exchange);
        log.info("MCP 工具调用: create_note, userId={}, knowledgeBaseId={}", userId, knowledgeBaseId);
        return noteService.createNote(userId, buildNoteDto(title, content, knowledgeBaseId, groupId));
    }

    @McpTool(name = "update_note", description = "更新笔记标题、内容或所属分组。")
    public Note updateNote(
            McpSyncServerExchange exchange,
            @McpToolParam(description = "笔记ID", required = true) Long noteId,
            @McpToolParam(description = "笔记标题", required = true) String title,
            @McpToolParam(description = "笔记内容", required = false) String content,
            @McpToolParam(description = "分组ID，可选", required = false) Long groupId
    ) {
        Long userId = noteSecurityService.requireUserIdFromMcpExchange(exchange);
        Note note = noteService.getOwnedNoteById(noteId, userId);
        log.info("MCP 工具调用: update_note, userId={}, noteId={}", userId, noteId);
        return noteService.updateNote(userId, noteId, buildNoteDto(title, content, note.getKnowledgeBaseId(), groupId));
    }

    @McpTool(name = "move_note_to_group", description = "将笔记移动到其他分组，或清空其分组。")
    public Note moveNoteToGroup(
            McpSyncServerExchange exchange,
            @McpToolParam(description = "笔记ID", required = true) Long noteId,
            @McpToolParam(description = "目标分组ID，可选", required = false) Long groupId
    ) {
        Long userId = noteSecurityService.requireUserIdFromMcpExchange(exchange);
        log.info("MCP 工具调用: move_note_to_group, userId={}, noteId={}", userId, noteId);
        return noteService.updateNoteGroup(userId, noteId, groupId);
    }

    @McpTool(name = "delete_note", description = "删除笔记。")
    public OperationResultDTO deleteNote(
            McpSyncServerExchange exchange,
            @McpToolParam(description = "笔记ID", required = true) Long noteId
    ) {
        Long userId = noteSecurityService.requireUserIdFromMcpExchange(exchange);
        log.info("MCP 工具调用: delete_note, userId={}, noteId={}", userId, noteId);
        noteService.deleteNoteAndUpdateCount(userId, noteId);
        return OperationResultDTO.success("笔记已删除。", "note", noteId);
    }

    @McpTool(name = "list_note_groups", description = "列出知识库下的笔记分组。")
    public List<NoteGroup> listNoteGroups(
            McpSyncServerExchange exchange,
            @McpToolParam(description = "知识库ID", required = true) Long knowledgeBaseId
    ) {
        Long userId = noteSecurityService.requireUserIdFromMcpExchange(exchange);
        log.info("MCP 工具调用: list_note_groups, userId={}, knowledgeBaseId={}", userId, knowledgeBaseId);
        return noteGroupService.getKnowledgeBaseGroups(knowledgeBaseId, userId);
    }

    @McpTool(name = "create_note_group", description = "创建笔记分组。")
    public NoteGroup createNoteGroup(
            McpSyncServerExchange exchange,
            @McpToolParam(description = "知识库ID", required = true) Long knowledgeBaseId,
            @McpToolParam(description = "分组名称", required = true) String name,
            @McpToolParam(description = "父分组ID，可选", required = false) Long parentId
    ) {
        Long userId = noteSecurityService.requireUserIdFromMcpExchange(exchange);
        log.info("MCP 工具调用: create_note_group, userId={}, knowledgeBaseId={}", userId, knowledgeBaseId);
        return noteGroupService.createGroup(userId, buildNoteGroupDto(name, knowledgeBaseId, parentId));
    }

    @McpTool(name = "update_note_group", description = "重命名笔记分组。")
    public NoteGroup updateNoteGroup(
            McpSyncServerExchange exchange,
            @McpToolParam(description = "分组ID", required = true) Long groupId,
            @McpToolParam(description = "分组名称", required = true) String name
    ) {
        Long userId = noteSecurityService.requireUserIdFromMcpExchange(exchange);
        NoteGroup group = noteSecurityService.requireNoteGroupOwner(groupId, userId);
        log.info("MCP 工具调用: update_note_group, userId={}, groupId={}", userId, groupId);
        return noteGroupService.updateGroup(userId, groupId, buildNoteGroupDto(name, group.getKnowledgeBaseId(), group.getParentId()));
    }

    @McpTool(name = "move_note_group", description = "将笔记分组移动到其他父分组下。")
    public NoteGroup moveNoteGroup(
            McpSyncServerExchange exchange,
            @McpToolParam(description = "分组ID", required = true) Long groupId,
            @McpToolParam(description = "父分组ID，可选", required = false) Long parentId
    ) {
        Long userId = noteSecurityService.requireUserIdFromMcpExchange(exchange);
        log.info("MCP 工具调用: move_note_group, userId={}, groupId={}", userId, groupId);
        return noteGroupService.moveGroup(userId, groupId, parentId);
    }

    @McpTool(name = "delete_note_group", description = "递归删除笔记分组及其中的所有笔记。")
    public OperationResultDTO deleteNoteGroup(
            McpSyncServerExchange exchange,
            @McpToolParam(description = "分组ID", required = true) Long groupId
    ) {
        Long userId = noteSecurityService.requireUserIdFromMcpExchange(exchange);
        log.info("MCP 工具调用: delete_note_group, userId={}, groupId={}", userId, groupId);
        noteGroupService.deleteGroupCascade(userId, groupId);
        return OperationResultDTO.success("笔记分组已删除。", "note_group", groupId);
    }

    private NoteDTO buildNoteDto(String title, String content, Long knowledgeBaseId, Long groupId) {
        return NoteDTO.builder()
                .title(title)
                .content(content)
                .knowledgeBaseId(knowledgeBaseId)
                .groupId(groupId)
                .build();
    }

    private NoteGroupDTO buildNoteGroupDto(String name, Long knowledgeBaseId, Long parentId) {
        return NoteGroupDTO.builder()
                .name(name)
                .knowledgeBaseId(knowledgeBaseId)
                .parentId(parentId)
                .build();
    }
}
