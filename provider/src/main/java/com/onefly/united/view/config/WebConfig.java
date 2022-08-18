package com.onefly.united.view.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.io.File;

/**
 * @author: chenjh
 * @since: 2019/4/16 20:04
 */
@Slf4j
@Configuration
public class WebConfig extends WebMvcConfigurerAdapter {
    @Autowired
    private KkViewProperties kkViewProperties;

    /**
     * 访问外部文件配置
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String filePath = kkViewProperties.getFileSaveDir();
        File file = new File(filePath);
        if (!file.exists()) {
            file.mkdirs();
        }
        log.info("Add resource locations: {}", filePath);
        registry.addResourceHandler("/preview/**").addResourceLocations("file:" + filePath);
        registry.addResourceHandler("/**").addResourceLocations("classpath:/META-INF/resources/", "classpath:/resources/", "classpath:/static/", "classpath:/public/", "file:" + filePath);
        super.addResourceHandlers(registry);
    }
}
