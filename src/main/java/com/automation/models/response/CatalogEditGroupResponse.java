package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Catalog Edit Group API.
 * Endpoint: GET /v1/admin/catalog/group/upload/{catalog_id}?limit=20&mode=edit
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogEditGroupResponse {

    private String statusCode;
    private String message;
    private CatalogEditGroupData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogEditGroupData {
        private String _id;
        private String name;
        private Integer total;
        private List<Object> data; // Can be empty or contain catalog items
        private String error;
    }
}
