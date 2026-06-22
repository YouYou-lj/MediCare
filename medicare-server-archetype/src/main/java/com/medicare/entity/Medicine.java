package com.medicare.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 药品实体
 */
@Data
@Entity
@Table(name = "medicine")
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String spec;

    @Column(length = 20)
    private String unit;

    @Column(nullable = false)
    private Integer stock = 0;

    @Column(name = "safety_stock", nullable = false)
    private Integer safetyStock = 10;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "batch_no", length = 50)
    private String batchNo;

    @Column(name = "pinyin_code", length = 50)
    private String pinyinCode;

    @Column(precision = 10, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(length = 200)
    private String manufacturer;

    @Column(nullable = false)
    private Integer status = 1;

    @Column(name = "create_time", updatable = false, insertable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;
}
