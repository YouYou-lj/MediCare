package com.medicare.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 处方视图对象 — 包含明细列表
 */
@Data
public class PrescriptionVO {

    private Long id;
    private Long recordId;
    private Long patientId;
    private Long doctorId;
    private BigDecimal totalAmount;
    private Integer status;
    private LocalDateTime createTime;

    // 关联字段
    private String patientName;
    private String doctorName;

    // 明细列表
    private List<PrescriptionItemVO> items;

    public static String getStatusText(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "待缴费";
            case 1 -> "已缴费";
            case 2 -> "已取药";
            case 3 -> "已作废";
            default -> "未知";
        };
    }
}
