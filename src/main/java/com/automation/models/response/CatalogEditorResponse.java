package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Catalog Editor API.
 * Endpoint: /v1/admin/catalog?limit=20&editor={editor_id}&sort=status
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogEditorResponse {

    private String statusCode;
    private String message;
    private List<CatalogEditorGroup> data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogEditorGroup {
        private List<Pagination> pagination;
        private List<CatalogEditorItem> data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Pagination {
        private Integer total;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogEditorItem {
        private String _id;
        private String source;
        private Integer status;
        private Integer priority;
        private String videoType;
        private String sellerId;
        private String phoneNumber;
        private String name;
        private String createdAt;
        private String updatedAt;
        private String editorId;
        private Integer total;
        private Integer tagged;
        private Integer active;
    }
}
