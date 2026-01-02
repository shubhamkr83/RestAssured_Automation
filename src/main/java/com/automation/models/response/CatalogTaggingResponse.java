package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Catalog Tagging API.
 * Endpoint: PUT /v1/admin/catalog/{catalog_id}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogTaggingResponse {

    private String statusCode;
    private String message;
    private CatalogTaggingData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogTaggingData {
        private Boolean available;
        private String contentType;
        private Boolean isDeleted;
        private String _id;
        private List<String> productTags;
        private String title;
        private Double priceText;
        private String catalogId;
    }
}
