package com.sky.config;

import com.sky.common.properties.MinioProperties;
import com.sky.interceptor.JwtAdminInterceptor;
import com.sky.interceptor.JwtUserInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * 说明（B 模块）：相对 A 的原版，这里只多了 C 端浏览类的 4 条放行规则。
 * 不加的话，未登录打开 H5 首页会被 401 踢去 #/login。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtAdminInterceptor jwtAdminInterceptor;
    private final JwtUserInterceptor jwtUserInterceptor;
    private final MinioProperties minioProperties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAdminInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/employee/login");

        registry.addInterceptor(jwtUserInterceptor)
                .addPathPatterns("/user/**")
                .excludePathPatterns(
                        "/user/user/login",
                        "/user/category/list",
                        "/user/dish/list",
                        "/user/setmeal/list",
                        "/user/setmeal/dish/**"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (!minioProperties.isEnabled()) {
            String location = Paths.get(minioProperties.getLocalDir())
                    .toAbsolutePath().normalize().toUri().toString();
            if (!location.endsWith("/")) {
                location = location + "/";
            }
            registry.addResourceHandler("/uploads/**").addResourceLocations(location);
        }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
