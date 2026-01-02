package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response POJO for Mark As Done API.
 * Endpoint: PUT /v1/admin/editor/assign/videos/done/{seller_id}/{catalog_id}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarkAsDoneResponse {

    private String statusCode;
    private String message;
    private Object data; // Can be any type based on API response
}
