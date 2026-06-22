package com.medicare.exception;

import com.medicare.dto.Result;
import jakarta.persistence.OptimisticLockException;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器 — 统一异常响应格式
 * <p>
 * 处理顺序（由具体到通用）：
 * 1. BusinessException — 业务异常（400，携带业务错误信息）
 * 2. MethodArgumentNotValidException — @Valid 校验失败（400，拼接字段错误）
 * 3. HttpMessageNotReadableException — 请求体解析失败（400）
 * 4. MissingServletRequestParameterException — 缺少必要参数（400）
 * 5. MethodArgumentTypeMismatchException — 参数类型错误（400）
 * 6. DataIntegrityViolationException — 数据约束冲突（409）
 * 7. OptimisticLockException — 乐观锁冲突（409）
 * 8. Exception — 兜底处理（500，信息脱敏，仅记录日志）
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常 — 返回 400 + 业务错误信息 */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBusinessException(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    /** @Valid 校验失败 — 拼接所有字段错误信息 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return Result.error(400, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        return Result.error(400, "请求体格式错误");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingParamException(MissingServletRequestParameterException e) {
        return Result.error(400, "缺少必要参数: " + e.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        return Result.error(400, "参数类型错误: " + e.getName());
    }

    /** 数据约束冲突 — 唯一键冲突、外键约束等，返回 409 */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Result<Void> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        String message = "数据操作冲突";
        Throwable cause = e.getCause();
        if (cause instanceof ConstraintViolationException cve) {
            message = "数据约束冲突: " + cve.getConstraintName();
        }
        return Result.error(409, message);
    }

    /** 乐观锁冲突 — 数据已被其他请求修改，返回 409 */
    @ExceptionHandler(OptimisticLockException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Result<Void> handleOptimisticLockException(OptimisticLockException e) {
        return Result.error(409, "数据已被修改，请刷新后重试");
    }

    /** 兜底处理 — 记录日志但返回脱敏信息，防止泄露堆栈细节 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("未处理异常", e);
        return Result.error(500, "服务器内部错误");
    }
}