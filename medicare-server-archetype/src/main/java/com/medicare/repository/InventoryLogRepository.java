package com.medicare.repository;

import com.medicare.dto.InventoryLogVO;
import com.medicare.entity.InventoryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {

    List<InventoryLog> findByMedicineIdOrderByLogTimeDesc(Long medicineId);

    /**
     * 查询库存日志（带药品名称）
     */
    @Query(value = "SELECT il.id, il.medicine_id AS medicineId, il.type, il.quantity, "
            + "il.batch_no AS batchNo, il.expiry_date AS expiryDate, il.operator, il.remark, "
            + "il.log_time AS logTime, m.name AS medicineName "
            + "FROM inventory_log il LEFT JOIN medicine m ON il.medicine_id = m.id "
            + "WHERE (:medicineId IS NULL OR il.medicine_id = :medicineId) "
            + "ORDER BY il.log_time DESC",
            nativeQuery = true)
    List<InventoryLogVO> findLogVOList(@Param("medicineId") Long medicineId);
}
