package com.medicare.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 挂号视图对象 — 4 表 LEFT JOIN 投影
 */
@Data
public class RegistrationVO {

    private Long id;
    private Long patientId;
    private Long scheduleId;
    private Long doctorId;
    private LocalDateTime regTime;
    private Integer status;
    private Integer seqNo;
    private BigDecimal fee;
    private LocalDateTime createTime;

    // 关联字段（4 表 JOIN）
    private String patientName;
    private String doctorName;
    private String departmentName;
    private String timeSlot;
}
