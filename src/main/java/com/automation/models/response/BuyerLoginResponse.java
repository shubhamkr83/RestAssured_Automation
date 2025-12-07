package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Buyer App Login API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BuyerLoginResponse {

    private String statusCode;
    private String message;
    private BuyerLoginData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BuyerLoginData {
        private String phoneNumber;
        private String name;
        private String businessName;
        private List<String> category;
        private Boolean businessVerified;
        private String segment;
        private String deviceId;
        private Boolean activated;
        private Boolean isDeleted;
        private String introVideo;
        private String tagLine;
        private String businessType;
        private String dealsIn;
        private Boolean bizupProtected;
        private Boolean isSeller;
        private String introVideoThumbnail;
        private Boolean isClaimed;
        private String accountManager;
        private Boolean isCatalogAvailable;
        private String profileShareLink;
        private String persona;
        private Location location;
        private String address;
        private String installReferrer;
        private Boolean isWhatsappOptIn;
        private Boolean sellOnBizup;
        private String createdAt;
        private String updatedAt;
        private String accessToken;
        private String refreshToken;
        private Boolean isProfileComplete;
        private Boolean isFirstTime;
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
        private String lat;
        private String lng;
        private String name;
    }
}
