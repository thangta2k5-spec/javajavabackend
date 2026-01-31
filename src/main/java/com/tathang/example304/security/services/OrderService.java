package com.tathang.example304.security.services;

import com.tathang.example304.model.*;
import com.tathang.example304.model.Order.OrderStatus;
import com.tathang.example304.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BidaTableRepository tableRepository;
    private final ProductRepository productRepository;
    private final WebSocketService webSocketService;

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            BidaTableRepository tableRepository,
            ProductRepository productRepository,
            WebSocketService webSocketService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.tableRepository = tableRepository;
        this.productRepository = productRepository;
        this.webSocketService = webSocketService;
    }

    // =====================================================
    // PRIVATE COMMON LOGIC (CHỐNG PHÌNH SERVICE)
    // =====================================================

    private void updateStock(Product product, int diff) {
        if (diff > 0 && product.getStockQuantity() < diff) {
            throw new RuntimeException("Insufficient stock");
        }
        product.setStockQuantity(product.getStockQuantity() - diff);
        productRepository.save(product);
    }

    private void recalcAndNotify(Order order) {
        updateOrderTotal(order.getId());
        webSocketService.notifyOrderUpdate(order);
    }

    private OrderItem getOrderItem(Long orderId, Long productId) {
        return orderItemRepository
                .findByOrderIdAndProductId(orderId, productId)
                .orElseThrow(() -> new RuntimeException("Order item not found"));
    }

    // =====================================================
    // BASIC QUERY
    // =====================================================

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderById(Long orderId) {
        return orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    public List<Order> getPendingOrders() {
        return orderRepository.findByStatus(Order.OrderStatus.OPEN);
    }

    public List<Order> getOrdersByTable(Long tableId) {
        return orderRepository.findByTableIdWithItems(tableId);
    }

    // =====================================================
    // ORDER LIFECYCLE
    // =====================================================

    public Order openOrder(Long tableId, Long employeeId) {
        BidaTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new RuntimeException("Table not found"));

        if (table.getStatus() != BidaTable.TableStatus.FREE) {
            throw new RuntimeException("Bàn không trống");
        }

        Order order = new Order(table, employeeId);
        order.setStatus(OrderStatus.OPEN);
        order.setStartTime(LocalDateTime.now());

        Order saved = orderRepository.save(order);

        return saved;
    }

    public Order updateOrderStatus(Long orderId, Order.OrderStatus status) {
        Order order = getOrderById(orderId);
        order.setStatus(status);
        Order saved = orderRepository.save(order);
        webSocketService.notifyOrderUpdate(saved);
        return saved;
    }

    public void closeOrder(Long orderId) {
        Order order = getOrderById(orderId);

        // CHO PHÉP đóng từ cả OPEN và WAITING_PAYMENT
        if (order.getStatus() != OrderStatus.OPEN && order.getStatus() != OrderStatus.WAITING_PAYMENT) {
            throw new RuntimeException("Order không ở trạng thái đang chơi hoặc chờ thanh toán");
        }

        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        BidaTable table = order.getTable();
        table.setStatus(BidaTable.TableStatus.FREE);
        tableRepository.save(table);

        webSocketService.notifyTableStatus(table.getId(), "FREE");
        webSocketService.notifyOrderUpdate(order);
    }

    // =====================================================
    // ORDER ITEM ACTIONS
    // =====================================================

    public Order addItemToOrder(Long orderId, Long productId, Integer quantity) {
        Order order = getOrderById(orderId);

        if (order.getStatus() != OrderStatus.OPEN) {
            throw new RuntimeException("Không thể thêm món khi đã kết thúc chơi");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        OrderItem item = orderItemRepository
                .findByOrderIdAndProductId(orderId, productId)
                .orElse(new OrderItem(order, product, 0, product.getPrice()));

        item.setQuantity(item.getQuantity() + quantity);
        updateStock(product, quantity);

        orderItemRepository.save(item);
        recalcAndNotify(order);

        return order;
    }

    public Order updateOrderItemQuantity(Long orderId, Long productId, Integer newQuantity) {
        Order order = getOrderById(orderId);
        OrderItem item = getOrderItem(orderId, productId);

        int diff = newQuantity - item.getQuantity();
        updateStock(item.getProduct(), diff);

        item.setQuantity(newQuantity);
        orderItemRepository.save(item);

        recalcAndNotify(order);
        return order;
    }

    public Order removeItemFromOrder(Long orderId, Long productId) {
        Order order = getOrderById(orderId);
        OrderItem item = getOrderItem(orderId, productId);

        updateStock(item.getProduct(), -item.getQuantity());
        orderItemRepository.delete(item);

        recalcAndNotify(order);
        return order;
    }

    // =====================================================
    // ITEM ACTIONS BY ITEM ID (CHO CONTROLLER CŨ)
    // =====================================================

    public OrderItem addItem(Long orderId, Long productId, Integer quantity) {
        addItemToOrder(orderId, productId, quantity);
        return getOrderItem(orderId, productId);
    }

    public void updateItemQuantity(Long orderId, Long itemId, Integer quantity) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        updateOrderItemQuantity(orderId, item.getProduct().getId(), quantity);
    }

    public void removeItem(Long orderId, Long itemId) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        removeItemFromOrder(orderId, item.getProduct().getId());
    }

    // =====================================================
    // TOTAL MONEY (BIGDECIMAL ONLY)
    // =====================================================

    public void updateOrderTotal(Long orderId) {
        Order order = getOrderById(orderId);
        BigDecimal total = orderItemRepository.getTotalAmountByOrderId(orderId);
        order.setTotalAmount(total);
        orderRepository.save(order);
    }

    // =====================================================
    // DELETE
    // =====================================================

    public void deleteOrder(Long orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new RuntimeException("Order not found");
        }
        orderRepository.deleteById(orderId);
    }

    public Order getOrderByTable(Long tableId) {
        // Sử dụng method mới để fetch items
        List<Order> orders = orderRepository.findByTableIdWithItems(tableId);

        if (orders.isEmpty()) {
            throw new RuntimeException("Không tìm thấy order cho bàn này");
        }

        // Tìm order đang active (OPEN hoặc WAITING_PAYMENT)
        Order activeOrder = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.OPEN ||
                        o.getStatus() == OrderStatus.WAITING_PAYMENT)
                .reduce((first, second) -> second) // lấy order mới nhất
                .orElseThrow(() -> new RuntimeException("Không có order đang hoạt động"));

        // Debug: in ra số lượng items
        System.out.println("🔄 DEBUG: Order " + activeOrder.getId() +
                " has " + (activeOrder.getItems() != null ? activeOrder.getItems().size() : 0) + " items");

        return activeOrder;
    }

    public Order finishPlaying(Long orderId) {
        Order order = getOrderById(orderId);

        if (order.getStatus() != OrderStatus.OPEN) {
            throw new RuntimeException("Order không ở trạng thái đang chơi");
        }

        order.setStatus(OrderStatus.WAITING_PAYMENT);
        order.setEndTime(LocalDateTime.now());

        Order saved = orderRepository.save(order);
        webSocketService.notifyOrderUpdate(saved);
        return saved;
    }

    public void cancelOrder(Long orderId) {
        Order order = getOrderById(orderId);

        if (order.getStatus() == OrderStatus.PAID) {
            throw new RuntimeException("Order đã thanh toán, không thể hủy");
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        BidaTable table = order.getTable();
        table.setStatus(BidaTable.TableStatus.FREE);
        tableRepository.save(table);

        webSocketService.notifyTableStatus(table.getId(), "FREE");
    }
}