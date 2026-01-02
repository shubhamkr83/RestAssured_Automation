package com.automation.models.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request POJO for Catalog Edit API.
 * Endpoint: PUT /v1/admin/catalog/{catalog_id}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogEditRequest {

    @JsonProperty("product_id")
    private String productId;

    private List<String> tags;

    private List<String> suggested;

    private List<Image> images;

    private String title;

    private Integer price;

    private Boolean isQc;

    private Boolean isSet;

    /**
     * Nested Image class for catalog images
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Image {
        private String description;

        @JsonProperty("_id")
        private String id;

        private String image;

        private Integer order;

        private Boolean isDeleted;

        private String createdAt;

        private String updatedAt;

        @JsonProperty("__v")
        private Integer version;

        private String original;

        private String thumbnail;

        private Integer thumbnailHeight;

        private Integer originalHeight;
    }
}
