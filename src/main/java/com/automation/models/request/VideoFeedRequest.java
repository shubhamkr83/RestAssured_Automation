package com.automation.models.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request POJO for Video Feed API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoFeedRequest {

    private List<String> excluded_videos;
    private Filters filters;
    private Integer from;
    private String liquidity;
    private String sort_by;
    private String sort_order;
    private String video_id;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Filters {
        private List<String> city;
        private List<String> productTags;
        private List<String> suitable_for;
        private String testData;
    }
}
