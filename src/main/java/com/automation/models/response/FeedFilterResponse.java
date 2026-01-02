package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Feed Filter API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeedFilterResponse {

    private String statusCode;
    private String message;
    private FilterData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FilterData {
        private List<ProductTag> productTags;
        private List<Object> suitable_for;  // Changed from List<String> to handle object structures
        private List<Object> city;  // Changed from List<String> to handle object structures
        private List<PriceFilter> priceFilters;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProductTag {
        private String name;
        private String image;
        private Object translation;
        private Boolean visible;
        private Boolean selected;
        private String displayName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PriceFilter {
        private String type;
        private List<PriceRange> ranges;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PriceRange {
        private Integer price_min;
        private Integer price_max;
        private String label;
    }
}
