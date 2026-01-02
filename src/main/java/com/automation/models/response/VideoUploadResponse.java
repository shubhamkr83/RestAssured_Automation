package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response POJO for Video Upload API.
 * Endpoint: POST /v1/admin/pipeline/video
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoUploadResponse {

    private String statusCode;
    private String message;
    private VideoUploadData data;
    private String videoLink; // Alternative field at root level

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VideoUploadData {
        private String _id;
        private String videoLink;
        private String videoType;
        private Integer status;
        private String url;
        private Seller seller;
        private Editor editor;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Seller {
        private String _id;
        private String phoneNumber;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Editor {
        private String _id;
        private String name;
    }
}
