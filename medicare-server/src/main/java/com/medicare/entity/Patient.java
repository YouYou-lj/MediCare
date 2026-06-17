package com.medicare.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 患者实体
 */
@Data
@Entity
@Table(name = "patient")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_card", nullable = false, length = 18)
    private String idCard;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private Integer gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(length = 20)
    private String phone;

    @Column(length = 200)
    private String address;

    @Column(name = "allergy_info", length = 500)
    private String allergyInfo;

    @Column(name = "create_time", updatable = false, insertable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;
}
