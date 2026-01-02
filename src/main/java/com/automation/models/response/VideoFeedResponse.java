package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Video Feed API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoFeedResponse {

    private String statusCode;
    private String message;
    private VideoFeedData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VideoFeedData {
        private List<VideoFeedItem> result;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VideoFeedItem {
        private Seller seller;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Seller {
        private String businessName;
        private String phoneNumber;
        private String name;
        private String id;
        private Boolean isSuper;
        private Boolean isDeleted;
        private Boolean ordersEnabled;
        private Boolean isCatalogAvailable;
        private Integer mov;
    }
}
