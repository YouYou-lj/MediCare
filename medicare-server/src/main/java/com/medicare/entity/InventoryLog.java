package com.medicare.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 库存变动日志实体
 */
@Data
@Entity
@Table(name = "inventory_log")
public class InventoryLog {

    public static final int TYPE_STOCK_IN = 1;   // 入库
    public static final int TYPE_STOCK_OUT = 2;   // 出库
    public static final int TYPE_SURPLUS = 3;     // 盘盈
    public static final int TYPE_LOSS = 4;        // 盘亏

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "medicine_id", nullable = false)
    private Long medicineId;

    @Column(nullable = false)
    private Integer type;

    @Column(nullable = false)
    private Integer quantity = 0;

    @Column(name = "batch_no", length = 50)
    private String batchNo;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(length = 50)
    private String operator;

    @Column(length = 500)
    private String remark;

    @Column(name = "log_time", updatable = false, insertable = false)
    private LocalDateTime logTime;
}
