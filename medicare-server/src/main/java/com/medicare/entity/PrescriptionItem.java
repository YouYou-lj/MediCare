package com.medicare.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 处方明细实体
 */
@Data
@Entity
@Table(name = "prescription_item")
public class PrescriptionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prescription_id", nullable = false)
    private Long prescriptionId;

    @Column(name = "medicine_id", nullable = false)
    private Long medicineId;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(length = 200)
    private String dosage;

    @Column(name = "usage_desc", length = 200)
    private String usageDesc;

    @Column(name = "unit_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "create_time", updatable = false, insertable = false)
    private LocalDateTime createTime;
}
