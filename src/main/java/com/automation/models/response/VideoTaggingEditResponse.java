package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Video Tagging Edit API.
 * Endpoint: GET /v1/admin/editor/edit/videos/{seller_id}?limit=100
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoTaggingEditResponse {

    private String statusCode;
    private String message;
    private VideoTaggingEditData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VideoTaggingEditData {
        private List<VideoItem> data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VideoItem {
        private String _id;
        private List<Object> displayTags;
        private List<Integer> language;
        private String contentType;
        private String uploadDate;
        private String editor;
        private String seller;
        private String driveLink;
        private String thubmbnailDriveLink;
        private String phoneNumber;
        private String marketNumber;
        private String uploadId;
        private Integer status;
        private Object meta;
        private String fabricText;
        private String priceText;
        private String description;
        private String createdAt;
        private String updatedAt;
    }
}
