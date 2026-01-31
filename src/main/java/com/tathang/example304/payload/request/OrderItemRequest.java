package com.tathang.example304.payload.request;

public class OrderItemRequest {
    private Long productId;
    private Integer quantity;

    // Đảm bảo có constructor mặc định
    public OrderItemRequest() {
    }

    // Constructor có tham số
    public OrderItemRequest(Long productId, Integer quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}