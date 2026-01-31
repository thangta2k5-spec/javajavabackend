package com.tathang.example304.security.services;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.text.NumberFormat;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MomoService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // 🔐 Cấu hình MoMo - THAY ĐỔI THEO THÔNG TIN CỦA BẠN
    private final String MOMO_ENDPOINT = "https://test-payment.momo.vn/v2/gateway/api/create";
    private final String PARTNER_CODE = "MOMO";
    private final String ACCESS_KEY = "F8BBA842ECF85";
    private final String SECRET_KEY = "K951B6PE1waDMi640xX08PD3vg6EkVlz";
    private final String RETURN_URL = "http://localhost:3000/payment/success";
    private final String NOTIFY_URL = "http://localhost:8080/api/payment/momo/ipn";
    private final String REQUEST_TYPE = "captureWallet";

    // ✅ Sử dụng ConcurrentHashMap để thread-safe
    private final Map<Long, PaymentInfo> paymentTransactions = new ConcurrentHashMap<>();

    public MomoService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    // 📊 Inner class để lưu thông tin payment
    private static class PaymentInfo {
        String status;
        Map<String, Object> data;
        long createTime;
        String transactionId;

        PaymentInfo(String status, Map<String, Object> data, String transactionId) {
            this.status = status;
            this.data = data;
            this.createTime = System.currentTimeMillis();
            this.transactionId = transactionId;
        }
    }

    /**
     * 🆕 PHƯƠNG THỨC CHÍNH: Tạo thanh toán MoMo (CHỈ CHUYỂN KHOẢN THỦ CÔNG)
     */
    public Map<String, Object> createPayment(Long orderId, double amount, String orderInfo, String extraData) {
        try {
            System.out.println("💰 Creating MANUAL transfer for order: " + orderId);
            // 🚫 LOẠI BỎ OFFICIAL API, CHỈ DÙNG CHUYỂN KHOẢN THỦ CÔNG
            return createManualTransferInfo(orderId, amount, orderInfo);

        } catch (Exception e) {
            System.out.println("❌ Manual transfer creation failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create manual transfer: " + e.getMessage());
        }
    }

    /**
     * 🚫 ĐÃ LOẠI BỎ: Tạo thanh toán MoMo Official API
     */

    /**
     * ✅ CHỈ DÙNG: Tạo thông tin chuyển khoản thủ công
     */
    private Map<String, Object> createManualTransferInfo(Long orderId, double amount, String orderInfo) {
        try {
            System.out.println("📱 Creating MANUAL transfer info for order: " + orderId);

            // ✅ SỬ DỤNG SỐ ĐIỆN THOẠI MỚI
            String momoPhoneNumber = "0357310548";
            String momoAccountName = "win";
            String momoBeneficiary = "win";
            String transactionId = "MANUAL_" + orderId + "_" + System.currentTimeMillis();

            Map<String, Object> result = new HashMap<>();
            result.put("phoneNumber", momoPhoneNumber);
            result.put("accountName", momoAccountName);
            result.put("beneficiary", momoBeneficiary);
            result.put("amount", (long) amount);
            result.put("orderInfo", orderInfo);
            result.put("transactionId", transactionId);
            result.put("paymentType", "MANUAL_TRANSFER");
            result.put("message", "Vui lòng chuyển khoản thủ công qua app MoMo");
            result.put("success", true);

            // ✅ Tạo deeplink với SĐT mới
            String deeplink = String.format("momo://transfer?phone=%s&amount=%d&note=%s",
                    momoPhoneNumber, (long) amount, orderInfo.replace(" ", "%20"));
            result.put("deeplink", deeplink);

            result.put("instructions", Arrays.asList(
                    "1. Mở ứng dụng MoMo trên điện thoại",
                    "2. Chọn 'Chuyển tiền'",
                    "3. Nhập số điện thoại: " + momoPhoneNumber,
                    "4. Nhập số tiền: " + formatCurrency(amount),
                    "5. Nội dung chuyển khoản: " + orderInfo,
                    "6. Xác nhận và hoàn tất chuyển khoản",
                    "7. Thông báo cho nhân viên sau khi chuyển thành công"));

            // Lưu transaction info
            PaymentInfo paymentInfo = new PaymentInfo("PENDING", result, transactionId);
            paymentTransactions.put(orderId, paymentInfo);

            System.out.println("✅ Manual transfer info created: " + result);
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to create manual transfer info: " + e.getMessage());
        }
    }

    /**
     * 🔍 Kiểm tra trạng thái thanh toán (CHỈ TRẢ VỀ PENDING)
     */
    public Map<String, String> checkPaymentStatus(Long orderId) {
        Map<String, String> result = new HashMap<>();

        PaymentInfo paymentInfo = paymentTransactions.get(orderId);

        if (paymentInfo == null) {
            result.put("status", "NOT_FOUND");
            result.put("orderId", orderId.toString());
            result.put("message", "Không tìm thấy thông tin thanh toán");
            return result;
        }

        // 🚫 LOẠI BỎ TỰ ĐỘNG THANH TOÁN: Luôn trả về PENDING
        String currentStatus = paymentInfo.status;

        result.put("status", currentStatus);
        result.put("orderId", orderId.toString());
        result.put("transactionId", paymentInfo.transactionId);
        result.put("message", getStatusMessage(currentStatus));
        result.put("timestamp", new Date().toString());

        return result;
    }

    /**
     * 📱 Xử lý IPN từ MoMo (KHÔNG DÙNG NỮA)
     */
    public Map<String, String> processIPN(Map<String, Object> momoResponse) {
        Map<String, String> result = new HashMap<>();
        result.put("status", "MANUAL_CONFIRMATION_REQUIRED");
        result.put("message", "Vui lòng xác nhận thanh toán thủ công");
        return result;
    }

    /**
     * ✅ Cập nhật trạng thái thanh toán thủ công (dùng cho xác nhận bằng tay)
     */
    public void updatePaymentStatus(Long orderId, String status) {
        PaymentInfo paymentInfo = paymentTransactions.get(orderId);
        if (paymentInfo != null) {
            paymentInfo.status = status;
            System.out.println("🔄 Manually updated payment status: Order " + orderId + " -> " + status);
        }
    }

    /**
     * 📊 Lấy thông tin transaction
     */
    public Map<String, Object> getTransactionInfo(Long orderId) {
        PaymentInfo paymentInfo = paymentTransactions.get(orderId);
        return paymentInfo != null ? paymentInfo.data : null;
    }

    // ============= HELPER METHODS =============

    private String generateSignature(String data, String secretKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(), "HmacSHA256"));
            byte[] rawHmac = mac.doFinal(data.getBytes());
            return bytesToHex(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate signature: " + e.getMessage());
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String formatCurrency(double amount) {
        NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        return formatter.format(amount) + " VND";
    }

    private Long extractOrderIdFromMomoOrderId(String momoOrderId) {
        try {
            if (momoOrderId != null && momoOrderId.startsWith("ORDER_")) {
                String[] parts = momoOrderId.split("_");
                if (parts.length >= 2) {
                    return Long.parseLong(parts[1]);
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getStatusMessage(String status) {
        switch (status) {
            case "PENDING":
                return "Đang chờ thanh toán";
            case "SUCCESS":
                return "Thanh toán thành công! Cảm ơn quý khách!";
            case "FAILED":
                return "Thanh toán thất bại. Vui lòng thử lại!";
            case "TIMEOUT":
                return "Hết thời gian thanh toán. Vui lòng thử lại!";
            case "NOT_FOUND":
                return "Không tìm thấy thông tin thanh toán";
            default:
                return "Trạng thái không xác định";
        }
    }
}