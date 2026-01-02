package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response POJO for Video Tagging API.
 * Endpoint: PUT /v1/admin/editor/edit/videos/{seller_id}/{video_id}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoTaggingResponse {

    private String statusCode;
    private String message;
    private VideoTaggingData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VideoTaggingData {
        private String _id;
        private String seller;
        private String title;
        private String priceText;
    }
}
