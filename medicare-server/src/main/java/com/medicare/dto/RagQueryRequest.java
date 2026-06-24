package com.medicare.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "RAG 文档知识库问答请求")
public class RagQueryRequest {

    @NotBlank(message = "请输入要检索的问题")
    @Size(max = 2000, message = "问题内容不能超过2000个字符")
    @Schema(description = "用户问题", example = "患者管理接口有哪些？")
    private String question;

    @Schema(description = "最多引用片段数", example = "5")
    private Integer topK = 5;
}
