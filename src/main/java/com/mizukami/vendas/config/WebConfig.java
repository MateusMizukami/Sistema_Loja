package com.mizukami.vendas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/** Serve as imagens enviadas pelo gerente (pasta fora do projeto compilado) em /uploads/**. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String pastaUploads;

    public WebConfig(@Value("${app.upload.dir:uploads}") String pastaUploads) {
        this.pastaUploads = pastaUploads;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path pasta = Path.of(pastaUploads).toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + pasta + "/");
    }
}
