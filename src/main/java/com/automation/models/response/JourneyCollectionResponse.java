package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Continue Your Journey (Journey Collection) API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JourneyCollectionResponse {

    private String statusCode;
    private String message;
    private JourneyCollectionData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JourneyCollectionData {
        private List<JourneyItem> result;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JourneyItem {
        private String id;
        private String thubmbnailDriveLink;
        private String thumbnail_url;
        private String thumbnailDriveLink;
        private CollectionInfo collection;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CollectionInfo {
        private String _id;
        private String name;
        private String description;
        private String image;
    }
}
