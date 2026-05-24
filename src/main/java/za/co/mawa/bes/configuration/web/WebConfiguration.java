package za.co.mawa.bes.configuration.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.config.annotation.*;
import za.co.mawa.bes.configuration.spring.ApiEndpointLoggingInterceptor;
import za.co.mawa.bes.configuration.spring.TenantRequestInterceptor;

@Configuration
@EnableWebMvc
public class WebConfiguration  implements WebMvcConfigurer    {
    private static final String EMAIL_TEMPLATE_ENCODING = "UTF-8";


    @Autowired
    private TenantRequestInterceptor tenantInterceptor;
    @Autowired
    private ApiEndpointLoggingInterceptor apiEndpointLoggingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor).addPathPatterns("/**").excludePathPatterns(WebSecurityConfig.SWAGGER_WHITELIST);
        registry.addInterceptor(apiEndpointLoggingInterceptor).addPathPatterns("/**").excludePathPatterns(WebSecurityConfig.SWAGGER_WHITELIST);
    }

    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        WebMvcConfigurer.super.configureDefaultServletHandling(configurer);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        WebMvcConfigurer.super.addResourceHandlers(registry);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        WebMvcConfigurer.super.addCorsMappings(registry);
    }
}
