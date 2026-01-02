package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Catalog By ID API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogByIdDetailResponse {

    private String statusCode;
    private String message;
    private CatalogDetailData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogDetailData {
        private Seller seller;
        private String taggedBy;
        private Thumbnail thumbnail;
        private List<Product> product;
        private Market market;
        private List<Object> images;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Seller {
        private String phoneNumber;
        private String address;
        private String businessName;
        private String name;
        private Boolean isSuper;
        private Boolean deprioritisation_status;
        private Boolean isCatalogAvailable;
        private Boolean ordersEnabled;
        private Integer mov;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Product {
        private String name;
        private String id;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Thumbnail {
        private String fileName;
        private String mimeType;
        private String _id;
        private String url;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Market {
        private String country;
        private String city;
        private String state;
        private String name;
        private String marketNumber;
        private String createdAt;
        private String updatedAt;
    }
}
