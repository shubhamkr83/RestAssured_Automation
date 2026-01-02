package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Video Assign Upload API.
 * Endpoint: GET /v1/admin/editor/assign/videos/{seller_id}?limit=50
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoAssignUploadResponse {

    private String statusCode;
    private String message;
    private VideoAssignUploadData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VideoAssignUploadData {
        private List<VideoItem> videos;
        private List<VideoItem> data; // Alternative field name
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VideoItem {
        private String _id;
        private String videoLink;
        private Integer status;
        private Seller seller;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Seller {
        private String _id;
    }
}
