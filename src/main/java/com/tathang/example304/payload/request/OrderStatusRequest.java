package com.tathang.example304.payload.request;

import com.tathang.example304.model.Order;

public class OrderStatusRequest {
    private Order.OrderStatus status;

    public Order.OrderStatus getStatus() {
        return status;
    }

    public void setStatus(Order.OrderStatus status) {
        this.status = status;
    }
}
