package com.medicare.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 排班号源实体类
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class Schedule {

    private Long id;
    private Long doctorId;
    private LocalDate workDate;
    private String timeSlot;
    private Integer totalSlots;
    private Integer remainSlots;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // 关联字段
    private String doctorName;
    private String departmentName;

    public Schedule() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public void setWorkDate(LocalDate workDate) {
        this.workDate = workDate;
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }

    public Integer getTotalSlots() {
        return totalSlots;
    }

    public void setTotalSlots(Integer totalSlots) {
        this.totalSlots = totalSlots;
    }

    public Integer getRemainSlots() {
        return remainSlots;
    }

    public void setRemainSlots(Integer remainSlots) {
        this.remainSlots = remainSlots;
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
}
