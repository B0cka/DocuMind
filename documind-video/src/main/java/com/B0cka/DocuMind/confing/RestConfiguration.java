package com.B0cka.DocuMind.confing;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class RestConfiguration {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        RestTemplate restTemplate = builder.build();

        // создаём Jackson-конвертер, который умеет обрабатывать и text/plain, и application/json
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        List<MediaType> types = new ArrayList<>(converter.getSupportedMediaTypes());
        types.add(MediaType.TEXT_PLAIN);
        converter.setSupportedMediaTypes(types);

        restTemplate.getMessageConverters().add(0, converter);

        return restTemplate;
    }
}
