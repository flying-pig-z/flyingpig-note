package fun.flyingpig.note.util.jwt;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


public class UserContext {

    /**
     * 获取当前用户ID
     */
    public static Long getCurrentUserId() {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            Object userId = request.getAttribute("userId");
            if (userId != null) {
                return (Long) userId;
            }
        }
        return null;
    }

    /**
     * 获取当前用户名
     */
    public static String getCurrentUsername() {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            Object username = request.getAttribute("username");
            if (username != null) {
                return (String) username;
            }
        }
        return null;
    }

    /**
     * 获取当前请求对象
     */
    private static HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}