package com.automation.models.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request POJO for Catalog Tagging API.
 * Endpoint: PUT /v1/admin/catalog/{catalog_id}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogTaggingRequest {

    @JsonProperty("product_id")
    private String productId;
    
    private List<String> tags;
    
    private List<String> suggested;
    
    private List<String> images;
    
    private String title;
    
    private Double price;
    
    private Boolean isQc;
    
    private Boolean isSet;
}
