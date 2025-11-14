package com.B0cka.DocuMind;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class DocuMindCoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(DocuMindCoreApplication.class);
    }
}