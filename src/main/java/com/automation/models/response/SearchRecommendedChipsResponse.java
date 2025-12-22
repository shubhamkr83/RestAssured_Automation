package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Search Recommended Chips API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchRecommendedChipsResponse {

    private String statusCode;
    private String message;
    private SearchRecommendedData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchRecommendedData {
        private List<SearchUserItem> items;
        private Integer totalCount;
        private List<Bucket> buckets;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchUserItem {
        private String _id;
        private String name;
        private String phoneNumber;
        private BusinessInfo businessInfo;
        private List<Object> tags;  // Changed from List<String> to handle object structures
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BusinessInfo {
        private String businessName;
        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Bucket {
        private String _id;
        private String name;
    }
}
