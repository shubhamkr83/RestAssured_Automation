package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response POJO for Video Thumbnail Upload API.
 * Endpoint: POST /v1/admin/editor/upload/videos/{seller_id}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoThumbnailUploadResponse {

    private String message;
    private VideoThumbnailData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VideoThumbnailData {
        private String _id;
        private String seller;
        private String sellerId;
        private String driveLink;
        private String thubmbnailDriveLink;
        private String contentType;
        private Object status; // Can be null
        private String createdAt;
        private String updatedAt;
    }
}
