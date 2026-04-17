package fun.flyingpig.note.config;

import fun.flyingpig.note.util.jwt.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.TimeUnit;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private static final long ASYNC_REQUEST_TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(10);

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        // 索引流式更新和流式问答都可能超过默认 30 秒，这里统一放宽。
        configurer.setDefaultTimeout(ASYNC_REQUEST_TIMEOUT_MILLIS);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")  // 拦截所有路径
                .excludePathPatterns(    // 排除不需要拦截的路径
                        "/api/auth/login",    // 登录接口
                        "/api/auth/logout", // 登出接口
                        "/api/rag/answer", // RAG问答接口，允许前端直接调用
                        "/api/auth/register"    // 注册接口
                );
    }
}
