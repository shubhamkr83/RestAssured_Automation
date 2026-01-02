package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response POJO for Video Upload Mark As Done API.
 * Endpoint: PUT /v1/admin/editor/assign/videos/done/{seller_id}/{upload_id}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoUploadMarkAsDoneResponse {

    private String message;
    private VideoUploadDoneData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VideoUploadDoneData {
        private String _id;
        private String sellerId;
        private String phoneNumber;
        private String url;
        private String assignedBy;
        private String editorId;
        private String uploadedBy;
        private Integer status;
        private String videoType;
        private String createdAt;
        private String updatedAt;
    }
}
