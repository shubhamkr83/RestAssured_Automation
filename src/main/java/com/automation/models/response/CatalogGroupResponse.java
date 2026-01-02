package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Catalog Group API (Elasticsearch-style response).
 * Endpoint: /v1/admin/catalog/upload/{catalog_id}?limit=20&mode=all
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogGroupResponse {

    private String statusCode;
    private String message;
    private CatalogGroupData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogGroupData {
        private Integer total;
        private List<CatalogGroupItem> data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogGroupItem {
        private String _index;
        private String _id;
        private Double _score;
        private CatalogSource _source;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogSource {
        private Seller seller;
        private String thubmbnailDriveLink;
        private String uploadId;
        private String displaySetting;
        private Boolean available;
        private List<String> priceTags;
        private String sellerId;
        private Boolean isDeleted;
        private List<String> displayTags;
        private String driveLink;
        private String id;
        private String contentType;
        private String uploadedBy;
        private List<String> segmentTags;
        private String updatedAt;
        private Thumbnail thumbnail;
        private Boolean visible;
        private Boolean isNew;
        private List<String> setType;
        private String url;
        private List<Object> tags;
        private Boolean processed;
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
        private Boolean isSuper;
        private Integer mov;
        private String businessName;
        private String name;
        private Integer cod;
        private String _id;
        private Boolean deprioritisation_status;
        private Boolean isCatalogAvailable;
        private Boolean ordersEnabled;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Thumbnail {
        // Add thumbnail fields if needed
        // Empty class to handle thumbnail object in response
    }
}
