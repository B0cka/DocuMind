package com.B0cka.DocuMind.dto;

import lombok.Data;

import java.util.List;

@Data
public class OpenRouterResponse {

    private String id;
    private String provider;
    private String model;
    private String object;
    private long created;
    private List<Choice> choices;
    private Usage usage;

    @Data
    public static class Choice {
        private Integer index;
        private Message message;
        private String finish_reason;
        private String native_finish_reason;
    }

    @Data
    public static class Message {
        private String role;
        private String content;
    }

    @Data
    public static class Usage {
        private Integer prompt_tokens;
        private Integer completion_tokens;
        private Integer total_tokens;
    }

    public String extractContent() {
        if (choices == null || choices.isEmpty()) return null;
        Choice first = choices.get(0);
        if (first.getMessage() == null) return null;
        return first.getMessage().getContent();
    }
}
