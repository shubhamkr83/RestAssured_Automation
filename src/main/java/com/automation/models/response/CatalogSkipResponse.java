package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response POJO for Catalog Skip API.
 * Endpoint: PUT /v1/admin/editor/assign/videos/skip/{seller_id}/{catalog_id}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogSkipResponse {

    private String statusCode;
    private String message;
    private CatalogSkipData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogSkipData {
        private String _id;
        private String assignedBy;
        private String editorId;
        private Integer status;
        private String videoType;
        private String sellerId;
    }
}
