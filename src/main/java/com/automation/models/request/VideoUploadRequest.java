package com.automation.models.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request POJO for Video Upload API.
 * Endpoint: POST /v1/admin/pipeline/video
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoUploadRequest {

    private String videoLink;
    private String videoType;
    private String description;
    private Object market; // Can be null
    private Seller seller;
    private Editor editor;

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
        private String createdAt;
        private String marketId;
        private Integer smell_test;
        private Integer assigned_score;
        private Boolean deprioritisation_status;
        private String label;
        private String value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Editor {
        private String _id;
        private String name;
        private List<String> role;
        private Integer status;
        private String phoneNumber;
        private String user_id;
        private String label;
        private String value;
    }
}
