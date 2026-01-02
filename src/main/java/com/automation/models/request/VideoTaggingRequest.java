package com.automation.models.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request POJO for Video Tagging API.
 * Endpoint: PUT /v1/admin/editor/edit/videos/{seller_id}/{video_id}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoTaggingRequest {

    private String product_id;
    private List<String> tags;
    private List<String> suggested;
    private String title;
    private String price;
}
