package za.co.mawa.bes.configuration.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
import za.co.mawa.bes.configuration.spring.TenantRequestInterceptor;

@Configuration
@EnableWebMvc
public class WebConfiguration  implements WebMvcConfigurer    {
//public class WebConfiguration extends WebMvcConfigurerAdapter  {
    @Autowired
    private TenantRequestInterceptor tenantInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor).addPathPatterns("/**").excludePathPatterns(WebSecurityConfig.AUTH_WHITELIST);
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
