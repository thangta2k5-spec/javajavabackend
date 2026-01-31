package com.tathang.example304.payload.request;

public class MomoPaymentRequest {
    private Long orderId;
    private Double amount;
    private String orderInfo;
    private String extraData;
    private String tableName;
    private java.util.List<OrderItem> items;

    // Constructors
    public MomoPaymentRequest() {
    }

    public MomoPaymentRequest(Long orderId, Double amount, String orderInfo, String extraData) {
        this.orderId = orderId;
        this.amount = amount;
        this.orderInfo = orderInfo;
        this.extraData = extraData;
    }

    // Getters and Setters
    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getOrderInfo() {
        return orderInfo;
    }

    public void setOrderInfo(String orderInfo) {
        this.orderInfo = orderInfo;
    }

    public String getExtraData() {
        return extraData;
    }

    public void setExtraData(String extraData) {
        this.extraData = extraData;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public java.util.List<OrderItem> getItems() {
        return items;
    }

    public void setItems(java.util.List<OrderItem> items) {
        this.items = items;
    }

    // Inner class for items
    public static class OrderItem {
        private String name;
        private Integer quantity;
        private Double price;

        // Getters and Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public Double getPrice() {
            return price;
        }

        public void setPrice(Double price) {
            this.price = price;
        }
    }
}