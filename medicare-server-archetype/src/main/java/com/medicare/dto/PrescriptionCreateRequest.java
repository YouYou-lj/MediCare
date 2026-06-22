package com.medicare.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建处方请求 DTO
 */
@Data
public class PrescriptionCreateRequest {

    @Data
    public static class PrescriptionInfo {
        @NotNull(message = "病历ID不能为空")
        private Long recordId;

        @NotNull(message = "患者ID不能为空")
        private Long patientId;

        @NotNull(message = "医生ID不能为空")
        private Long doctorId;
    }

    @Data
    public static class ItemInfo {
        @NotNull(message = "药品ID不能为空")
        private Long medicineId;

        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量至少为1")
        private Integer quantity;

        private String dosage;

        private String usageDesc;

        @NotNull(message = "单价不能为空")
        private BigDecimal unitPrice;
    }

    @NotNull(message = "处方信息不能为空")
    @Valid
    private PrescriptionInfo prescription;

    @NotEmpty(message = "处方明细不能为空")
    @Valid
    private List<@Valid ItemInfo> items;
}
