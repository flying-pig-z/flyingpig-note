package fun.flyingpig.note.service.security;

import fun.flyingpig.note.dto.LoginDTO;
import fun.flyingpig.note.dto.LoginResponseDTO;
import fun.flyingpig.note.dto.RegisterDTO;
import fun.flyingpig.note.dto.TokenValidationDTO;
import fun.flyingpig.note.entity.KnowledgeBase;
import fun.flyingpig.note.entity.Note;
import fun.flyingpig.note.entity.NoteGroup;
import fun.flyingpig.note.entity.User;
import fun.flyingpig.note.exception.BusinessException;
import fun.flyingpig.note.mapper.KnowledgeBaseMapper;
import fun.flyingpig.note.mapper.NoteGroupMapper;
import fun.flyingpig.note.mapper.NoteMapper;
import fun.flyingpig.note.service.user.UserService;
import fun.flyingpig.note.util.jwt.JwtUtil;
import fun.flyingpig.note.util.jwt.UserContext;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NoteSecurityService {

    public static final String MCP_AUTHORIZATION_HEADER_CONTEXT_KEY = HttpHeaders.AUTHORIZATION;

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final NoteMapper noteMapper;
    private final NoteGroupMapper noteGroupMapper;

    public LoginResponseDTO login(LoginDTO loginDTO) {
        User user = userService.login(loginDTO);
        if (user == null) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        return buildLoginResponse(user);
    }

    public LoginResponseDTO register(RegisterDTO registerDTO) {
        User user = userService.register(registerDTO);
        return buildLoginResponse(user);
    }

    public Long requireCurrentUserId() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        return userId;
    }

    public User requireCurrentUser() {
        Long userId = requireCurrentUserId();
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在或已被删除");
        }
        return user;
    }

    public Long requireUserIdByToken(String accessToken) {
        return authenticateToken(accessToken).getId();
    }

    public Long requireUserIdFromMcpExchange(McpSyncServerExchange exchange) {
        return authenticateMcpExchange(exchange).getId();
    }

    public User authenticateMcpExchange(McpSyncServerExchange exchange) {
        if (exchange == null) {
            throw new BusinessException(401, "缺少 MCP 请求上下文");
        }
        return authenticateTransportContext(exchange.transportContext());
    }

    public User authenticateTransportContext(McpTransportContext transportContext) {
        Object authorizationHeader = transportContext == null
                ? null
                : transportContext.get(MCP_AUTHORIZATION_HEADER_CONTEXT_KEY);

        if (!(authorizationHeader instanceof String headerValue) || !StringUtils.hasText(headerValue)) {
            throw new BusinessException(401, "缺少 Authorization 请求头");
        }
        return authenticateToken(headerValue);
    }

    public User authenticateToken(String accessToken) {
        String normalizedToken = normalizeToken(accessToken);
        if (!jwtUtil.validateToken(normalizedToken)) {
            throw new BusinessException(401, "accessToken 无效或已过期");
        }

        Long userId = jwtUtil.getUserIdFromToken(normalizedToken);
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在或已被删除");
        }
        return user;
    }

    public TokenValidationDTO validateToken(String accessToken) {
        try {
            String normalizedToken = normalizeToken(accessToken);
            if (!jwtUtil.validateToken(normalizedToken)) {
                return TokenValidationDTO.invalid();
            }

            return TokenValidationDTO.builder()
                    .valid(true)
                    .userId(jwtUtil.getUserIdFromToken(normalizedToken))
                    .username(jwtUtil.getUsernameFromToken(normalizedToken))
                    .expiresAt(jwtUtil.getExpirationDateFromToken(normalizedToken))
                    .build();
        } catch (BusinessException ex) {
            return TokenValidationDTO.invalid();
        }
    }

    public KnowledgeBase requireKnowledgeBaseOwner(Long knowledgeBaseId, Long userId) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (knowledgeBase == null) {
            throw new BusinessException(404, "知识库不存在");
        }
        if (!knowledgeBase.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权访问该知识库");
        }
        return knowledgeBase;
    }

    public void requireKnowledgeBaseOwnership(List<Long> knowledgeBaseIds, Long userId) {
        if (CollectionUtils.isEmpty(knowledgeBaseIds)) {
            throw new BusinessException(400, "至少需要指定一个知识库");
        }

        Set<Long> uniqueKnowledgeBaseIds = new LinkedHashSet<>(knowledgeBaseIds);
        for (Long knowledgeBaseId : uniqueKnowledgeBaseIds) {
            requireKnowledgeBaseOwner(knowledgeBaseId, userId);
        }
    }

    public Note requireNoteOwner(Long noteId, Long userId) {
        Note note = noteMapper.selectById(noteId);
        if (note == null) {
            throw new BusinessException(404, "笔记不存在");
        }
        requireKnowledgeBaseOwner(note.getKnowledgeBaseId(), userId);
        return note;
    }

    public NoteGroup requireNoteGroupOwner(Long groupId, Long userId) {
        NoteGroup group = noteGroupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(404, "分组不存在");
        }
        requireKnowledgeBaseOwner(group.getKnowledgeBaseId(), userId);
        return group;
    }

    private LoginResponseDTO buildLoginResponse(User user) {
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return new LoginResponseDTO(token, user);
    }

    private String normalizeToken(String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            throw new BusinessException(401, "缺少 accessToken");
        }

        String tokenFromHeader = jwtUtil.getTokenFromHeader(accessToken);
        return StringUtils.hasText(tokenFromHeader) ? tokenFromHeader : accessToken.trim();
    }
}
