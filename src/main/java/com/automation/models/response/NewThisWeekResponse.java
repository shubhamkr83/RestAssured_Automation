package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for New This Week API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewThisWeekResponse {

    private String statusCode;
    private String message;
    private NewThisWeekData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NewThisWeekData {
        private List<NewThisWeekItem> result;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NewThisWeekItem {
        private String _id;
        private String name;
        private String description;
        private String image;
        private Integer addedThisWeek;
        private String brandingImage;
        private String brandingCatId;
        private String type;
    }
}
