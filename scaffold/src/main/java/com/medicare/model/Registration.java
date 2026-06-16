package com.medicare.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 挂号实体类
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class Registration {

    private Long id;
    private Long patientId;
    private Long scheduleId;
    private LocalDateTime regTime;
    private Integer status;
    private Integer seqNo;
    private BigDecimal fee;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // 关联字段
    private String patientName;
    private String doctorName;
    private String departmentName;
    private String timeSlot;

    public static final int STATUS_WAITING = 0;   // 候诊
    public static final int STATUS_IN_PROGRESS = 1; // 就诊中
    public static final int STATUS_COMPLETED = 2;   // 已完成
    public static final int STATUS_CANCELLED = 3;   // 已取消

    public Registration() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }

    public LocalDateTime getRegTime() {
        return regTime;
    }

    public void setRegTime(LocalDateTime regTime) {
        this.regTime = regTime;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getSeqNo() {
        return seqNo;
    }

    public void setSeqNo(Integer seqNo) {
        this.seqNo = seqNo;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }

    public String getStatusText() {
        return getStatusText(this.status);
    }

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
