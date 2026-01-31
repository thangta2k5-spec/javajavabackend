package com.tathang.example304.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tathang.example304.model.BidaTable;

import java.util.List;

@Repository
public interface BidaTableRepository extends JpaRepository<BidaTable, Long> {
    List<BidaTable> findByStatus(BidaTable.TableStatus status);

    BidaTable findByNumber(Integer number);

    List<BidaTable> findByCapacityGreaterThanEqual(Integer capacity);

    Long countByStatus(BidaTable.TableStatus status);
}