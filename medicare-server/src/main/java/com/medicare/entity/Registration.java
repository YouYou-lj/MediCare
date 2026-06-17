package com.medicare.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 挂号实体
 * 修复：添加 doctorId 字段，原表缺少此字段，需 ALTER TABLE 添加
 */
@Data
@Entity
@Table(name = "registration")
public class Registration {

    public static final int STATUS_WAITING = 0;
    public static final int STATUS_IN_PROGRESS = 1;
    public static final int STATUS_COMPLETED = 2;
    public static final int STATUS_CANCELLED = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "schedule_id", nullable = false)
    private Long scheduleId;

    /** 修复：添加 doctorId，原 registration 表无此列，需 DDL: ALTER TABLE registration ADD COLUMN doctor_id BIGINT UNSIGNED COMMENT '医生ID' AFTER schedule_id; */
    @Column(name = "doctor_id")
    private Long doctorId;

    @Column(name = "reg_time", updatable = false, insertable = false)
    private LocalDateTime regTime;

    @Column(nullable = false)
    private Integer status = 0;

    @Column(name = "seq_no")
    private Integer seqNo;

    @Column(precision = 10, scale = 2)
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(name = "create_time", updatable = false, insertable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    public static String getStatusText(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case STATUS_WAITING -> "候诊";
            case STATUS_IN_PROGRESS -> "就诊中";
            case STATUS_COMPLETED -> "已完成";
            case STATUS_CANCELLED -> "已取消";
            default -> "未知";
        };
    }
}
