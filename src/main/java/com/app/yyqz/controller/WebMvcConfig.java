package com.app.yyqz.controller;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

@Configuration
public class WebMvcConfig extends WebMvcConfigurationSupport {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        //  /home/file/**为前端URL访问路径  后面 file:xxxx为本地磁盘映射//
        registry.addResourceHandler("/images/**").addResourceLocations("file:C://app/yyqz/images/");
        registry.addResourceHandler("/foods/images/**").addResourceLocations("file:C://app/yyqz/foodImage/");
        registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
    }
}