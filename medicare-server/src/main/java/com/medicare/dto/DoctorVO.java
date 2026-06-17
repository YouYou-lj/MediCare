package com.medicare.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 医生视图对象 — 关联科室名称
 */
@Data
public class DoctorVO {

    private Long id;
    private String name;
    private Long departmentId;
    private String title;
    private Integer status;
    private LocalDateTime createTime;

    // 关联字段
    private String departmentName;
}
