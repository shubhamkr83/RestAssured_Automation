package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Auth Validate API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthValidateResponse {

    private String statusCode;
    private String message;
    private AuthValidateData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AuthValidateData {
        private String _id;
        private String phoneNumber;
        private String name;
        private String businessName;
        private String createdAt;
        private String updatedAt;
        private Location location;
        private String address;
        private Boolean sellOnBizup;
        private Boolean businessVerified;
        private Boolean activated;
        private Boolean isDeleted;
        private List<String> category;
        private List<String> businessCard;
        private List<String> termsCondition;
        private List<String> fo;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Location {
        private String country;
        private String city;
        private String state;
        private String pincode;
        private Double lat;
        private Double lng;
        private String name;
    }
}
