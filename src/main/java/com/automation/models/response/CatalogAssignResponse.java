package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response POJO for Catalog Assign to Editor API.
 * Endpoint: /v1/admin/catalog/assign/{catalog_id}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogAssignResponse {

    private String statusCode;
    private String message;
    private CatalogAssignData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogAssignData {
        private String _id;
        private String videoType;
        private String sellerId;
        private String editorId;
        private String createdAt;
        private String updatedAt;

        @JsonProperty("__v")
        private Integer version;
    }
}
