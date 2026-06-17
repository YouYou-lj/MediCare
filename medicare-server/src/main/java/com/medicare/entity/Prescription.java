package com.medicare.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 处方实体
 */
@Data
@Entity
@Table(name = "prescription")
public class Prescription {

    public static final int STATUS_PENDING = 0;   // 待缴费
    public static final int STATUS_PAID = 1;       // 已缴费
    public static final int STATUS_DISPENSED = 2;  // 已取药
    public static final int STATUS_CANCELLED = 3;  // 已作废

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "record_id", nullable = false)
    private Long recordId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer status = 0;

    @Column(name = "create_time", updatable = false, insertable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;
}
