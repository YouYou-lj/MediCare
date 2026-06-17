package com.medicare.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统用户实体
 */
@Data
@Entity
@Table(name = "sys_user")
public class SysUser {

    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_DOCTOR = "doctor";
    public static final String ROLE_PHARMACIST = "pharmacist";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(name = "real_name", length = 50)
    private String realName;

    @Column(length = 20)
    private String role = ROLE_DOCTOR;

    @Column(nullable = false)
    private Integer status = 1;

    @Column(name = "doctor_id")
    private Long doctorId;

    @Column(name = "create_time", updatable = false, insertable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    public boolean isAdmin() { return ROLE_ADMIN.equals(role); }
    public boolean isDoctor() { return ROLE_DOCTOR.equals(role); }
    public boolean isPharmacist() { return ROLE_PHARMACIST.equals(role); }
}
