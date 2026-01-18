package com.B0cka.DocuMind.service.summary;

import com.B0cka.DocuMind.dto.VideoSummaryResponseDto;

public interface SummaryService {
    VideoSummaryResponseDto summarize(String link);
}
