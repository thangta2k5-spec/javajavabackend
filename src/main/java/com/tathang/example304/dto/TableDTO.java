package com.tathang.example304.dto;

import java.time.LocalDateTime;

import com.tathang.example304.model.BidaTable;

import lombok.Data;

@Data
public class TableDTO {
    private Long id;
    private Integer number;
    private String tableName;
    private Integer capacity;
    private BidaTable.TableStatus status;
    private Long currentOrderId;
    private LocalDateTime startTime;

    // getter / setter
}
