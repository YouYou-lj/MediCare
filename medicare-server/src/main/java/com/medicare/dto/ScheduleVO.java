package com.medicare.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 排班视图对象 — 关联医生+科室名称
 */
@Data
public class ScheduleVO {

    private Long id;
    private Long doctorId;
    private LocalDate workDate;
    private String timeSlot;
    private Integer totalSlots;
    private Integer remainSlots;
    private LocalDateTime createTime;

    // 关联字段
    private String doctorName;
    private String departmentName;
}
