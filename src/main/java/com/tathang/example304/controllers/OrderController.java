package com.tathang.example304.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tathang.example304.model.Order;
import com.tathang.example304.model.OrderItem;
import com.tathang.example304.security.services.OrderService;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // Mở bàn (tạo order mới)
    @PostMapping("/open")
    @PreAuthorize("hasAnyRole('STAFF','MANAGER')")
    public ResponseEntity<Order> openOrder(
            @RequestParam Long tableId,
            @RequestParam Long employeeId) {
        return ResponseEntity.ok(orderService.openOrder(tableId, employeeId));
    }

    // Xem order theo bàn
    @GetMapping("/table/{tableId}")
    @PreAuthorize("hasAnyRole('STAFF','MANAGER')")
    public ResponseEntity<Order> getOrderByTable(@PathVariable Long tableId) {
        return ResponseEntity.ok(orderService.getOrderByTable(tableId));
    }

    // Thêm món
    @PostMapping("/{orderId}/items")
    @PreAuthorize("hasAnyRole('STAFF','MANAGER')")
    public ResponseEntity<OrderItem> addItem(
            @PathVariable Long orderId,
            @RequestParam Long productId,
            @RequestParam Integer quantity) {
        return ResponseEntity.ok(
                orderService.addItem(orderId, productId, quantity));
    }

    // Cập nhật số lượng
    @PutMapping("/{orderId}/items/{itemId}")
    @PreAuthorize("hasAnyRole('STAFF','MANAGER')")
    public ResponseEntity<?> updateItem(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @RequestParam Integer quantity) {
        orderService.updateItemQuantity(orderId, itemId, quantity);
        return ResponseEntity.ok().build();
    }

    // Xóa món
    @DeleteMapping("/{orderId}/items/{itemId}")
    @PreAuthorize("hasAnyRole('STAFF','MANAGER')")
    public ResponseEntity<?> removeItem(
            @PathVariable Long orderId,
            @PathVariable Long itemId) {
        orderService.removeItem(orderId, itemId);
        return ResponseEntity.ok().build();
    }

    // Đóng order
    @PatchMapping("/{orderId}/close")
    @PreAuthorize("hasAnyRole('STAFF','MANAGER')")
    public ResponseEntity<?> closeOrder(@PathVariable Long orderId) {
        orderService.closeOrder(orderId);
        return ResponseEntity.ok().build();
    }
}
