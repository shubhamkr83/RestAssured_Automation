package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Catalog Tag API.
 * Endpoint: /v1/admin/catalog/group/upload/{catalog_id}?limit=20&mode=add
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogTagResponse {

    private String statusCode;
    private String message;
    private CatalogTagData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogTagData {
        private String _id;
        private String name;
        private Integer total;
        private List<CatalogTagItem> data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogTagItem {
        private String _index;
        private String _id;
        private Double _score;
        private CatalogTagSource _source;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogTagSource {
        private Seller seller;
        private String thubmbnailDriveLink;
        private String uploadId;
        private List<String> priceTags;
        private String description;
        private String uploadedDate;
        private String createdAt;
        private String sellerId;
        private Boolean isDeleted;
        private Thumbnail thumbnail;
        private String phoneNumber;
        private String uploadDate;
        private String original_upload;
        private List<String> productTags;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Seller {
        private String phoneNumber;
        private Integer smell_test;
        private String address;
        private String businessName;
        private String name;
        private Integer cod;
        private String _id;
        private Boolean isCatalogAvailable;
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
}
