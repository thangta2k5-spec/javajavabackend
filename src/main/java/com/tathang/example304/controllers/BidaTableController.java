package com.tathang.example304.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tathang.example304.model.BidaTable;
import com.tathang.example304.model.BidaTable.TableStatus;
import com.tathang.example304.security.services.BidaTableService;

@RestController
@RequestMapping("/api/tables")
@CrossOrigin(origins = "*")
public class BidaTableController {

    private final BidaTableService tableService;

    public BidaTableController(BidaTableService tableService) {
        this.tableService = tableService;
    }

    // Xem tất cả bàn
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<List<BidaTable>> getAllTables() {
        return ResponseEntity.ok(tableService.getAllTables());
    }

    // Xem bàn trống
    @GetMapping("/free")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<List<BidaTable>> getFreeTables() {
        return ResponseEntity.ok(tableService.getFreeTables());
    }

    // Tạo bàn mới
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<BidaTable> createTable(@RequestBody BidaTable table) {
        return ResponseEntity.ok(tableService.createTable(table));
    }

    // Đổi trạng thái bàn
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestParam TableStatus status) {
        tableService.updateTableStatus(id, status);
        return ResponseEntity.ok().build();
    }
}
