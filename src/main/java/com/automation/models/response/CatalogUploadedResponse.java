package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Catalog Uploaded API.
 * Endpoint: /v1/admin/catalog?limit=20
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogUploadedResponse {

    private String statusCode;
    private String message;
    private List<CatalogUploadedGroup> data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogUploadedGroup {
        private List<CatalogUploadedItem> data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogUploadedItem {
        private String _id;
        private String source;
        private String videoType;
        private String sellerId;
        private String phoneNumber;
        private String name;
    }
}
