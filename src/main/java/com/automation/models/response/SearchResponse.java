package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Response model for User Search API.
 * Represents search results with sellers/users matching the query.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchResponse {
    private String statusCode;
    private String message;
    private SearchData data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchData {
        private List<UserItem> items;
        private Integer totalCount;
        private Integer page;
        private Integer pageSize;
        private List<Bucket> buckets;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserItem {
        private String _id;
        private String name;
        private String phoneNumber;
        private List<String> tags;
        private BusinessInfo businessInfo;
        private Seller seller;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BusinessInfo {
        private String businessName;
        private String description;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Seller {
        private String _id;
        private String phoneNumber;
        private String address;
        private String businessName;
        private String name;
        private Boolean deprioritisation_status;
        private Boolean isCatalogAvailable;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Bucket {
        private String _id;
        private String name;
        private Integer count;
    }
}
