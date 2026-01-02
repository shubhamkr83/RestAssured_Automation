package com.automation.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response POJO for Update Cart API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateCartResponse {

    private String statusCode;
    private String message;
    private CartData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CartData {
        private String _id;
        private List<String> catalogs;
        private String status;
        private List<CartItem> cartItems;
        private String buyerId;
        private Double cartTotal;
        private Double shippingCharges;
        private Double tax;
        private Double totalPayable;
        private String createdAt;
        private String updatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CartItem {
        private String title;
        private Double priceText;
        private Integer quantity;
        private String sellerId;
        private Integer setQuantity;
        private Double totalPrice;
        private Boolean isDeleted;
        private Boolean available;
    }
}
