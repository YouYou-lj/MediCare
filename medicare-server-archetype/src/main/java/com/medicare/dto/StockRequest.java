package com.medicare.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 出入库请求参数
 */
@Data
public class StockRequest {

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须大于0")
    private Integer quantity;

    private String batchNo;
    private String expiryDate;
    private String operator;
    private String remark;
}
