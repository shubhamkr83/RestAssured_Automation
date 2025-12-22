package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Feed Filter Save API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeedFilterSaveResponse {

    private String statusCode;
    private String message;
    private FilterSaveData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FilterSaveData {
        private List<Object> suitable_for;  // Changed from List<String> to handle object structures
        private List<Object> productTags;  // Changed from List<String> to handle object structures
        private List<Object> city;  // Changed from List<String> to handle object structures
        private Integer price_min;
        private Integer price_max;
        private String lastSelectedFilter;
    }
}
