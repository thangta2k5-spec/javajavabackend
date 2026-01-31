package com.tathang.example304.payload.request;

import com.tathang.example304.model.BidaTable;

public class TableStatusRequest {
    private BidaTable.TableStatus status;

    // 🆕 THÊM CONSTRUCTOR MẶC ĐỊNH
    public TableStatusRequest() {
    }

    // Constructor có tham số (tùy chọn)
    public TableStatusRequest(BidaTable.TableStatus status) {
        this.status = status;
    }

    public BidaTable.TableStatus getStatus() {
        return status;
    }

    public void setStatus(BidaTable.TableStatus status) {
        this.status = status;
    }
}