package com.automation.models.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request POJO for Video Title Generation API.
 * Endpoint: POST https://bomb.bizup.app/api/chat/title/tags
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoTitleGenerationRequest {

    private List<String> tags;
}
