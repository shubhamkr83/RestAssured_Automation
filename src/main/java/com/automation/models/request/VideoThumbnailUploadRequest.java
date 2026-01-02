package com.automation.models.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request POJO for Video Thumbnail Upload API.
 * Endpoint: POST /v1/admin/editor/upload/videos/{seller_id}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoThumbnailUploadRequest {

    private Seller seller;
    private Object market; // Can be null
    private String videoLink;
    private String thumbnailLink;
    private Boolean introVideo;
    private String uploadId;
    private String fabricText;
    private String priceText;
    private String description;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Seller {
        private String _id;
        private String phoneNumber;
        private String name;
        private String businessName;
        private String marketId;
    }
}
