package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Catalog Edit API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogEditResponse {

    private String statusCode;
    private String message;
    private CatalogEditData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogEditData {
        private Boolean available;
        private String contentType;
        private Boolean isDeleted;
        private String _id;
        private List<String> productTags;
        private String title;
        private Double priceText;
    }
}
