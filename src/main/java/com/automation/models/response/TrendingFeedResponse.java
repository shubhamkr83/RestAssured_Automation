package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Trending Feed API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrendingFeedResponse {

    private String statusCode;
    private String message;
    private TrendingData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TrendingData {
        private List<TrendingItem> result;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TrendingItem {
        private String _id;
        private String id;
        private Integer priceText;
        private Integer price;
        private String thumbnail_url;
        private Thumbnail thumbnail;
        private Integer popular;
        private String contentType;
        private String driveLink;
        private List<Object> tags;  // Changed from List<String> to handle object structures
        private List<Product> product;
        private Boolean isDeleted;
        private Boolean available;
        private Seller seller;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Thumbnail {
        private String url;
        private String fileName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Product {
        private String _id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Seller {
        private Boolean deprioritisation_status;
        private String name;
        private String _id;
    }
}
