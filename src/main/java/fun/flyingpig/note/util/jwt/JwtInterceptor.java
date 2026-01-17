package fun.flyingpig.note.util.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JwtProperties jwtProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 预检请求直接放行
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        // 获取token
        String authHeader = request.getHeader(jwtProperties.getHeader());
        String token = jwtUtil.getTokenFromHeader(authHeader);

        if (!StringUtils.hasText(token)) {
            writeErrorResponse(response, 401, "缺少认证令牌");
            return false;
        }

        try {
            // 验证token
            if (!jwtUtil.validateToken(token)) {
                writeErrorResponse(response, 401, "认证令牌无效或已过期");
                return false;
            }

            // 将用户信息存储到请求属性中
            request.setAttribute("userId", jwtUtil.getUserIdFromToken(token));
            request.setAttribute("username", jwtUtil.getUsernameFromToken(token));

            return true;
        } catch (Exception e) {
            log.error("JWT验证失败: {}", e.getMessage());
            writeErrorResponse(response, 401, "认证令牌验证失败");
            return false;
        }
    }

    /**
     * 写入错误响应
     */
    private void writeErrorResponse(HttpServletResponse response, int code, String message) throws Exception {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("message", message);
        result.put("data", null);

        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}