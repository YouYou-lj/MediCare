package com.medicare.repository;

import com.medicare.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {

    List<Medicine> findByStatus(Integer status);

    /**
     * 安全扣减库存 — WHERE stock >= :qty 防止库存负数
     * @return 影响行数，0 表示库存不足
     */
    @Modifying
    @Query("UPDATE Medicine m SET m.stock = m.stock - :qty WHERE m.id = :id AND m.stock >= :qty")
    int safeDecrementStock(@Param("id") Long id, @Param("qty") Integer qty);

    /**
     * 回增库存（处方作废时）
     */
    @Modifying
    @Query("UPDATE Medicine m SET m.stock = m.stock + :qty WHERE m.id = :id")
    int incrementStock(@Param("id") Long id, @Param("qty") Integer qty);

    /**
     * 低库存预警列表
     */
    @Query("SELECT m FROM Medicine m WHERE m.stock <= m.safetyStock AND m.status = 1")
    List<Medicine> findLowStockMedicines();

    /**
     * 按关键字搜索（名称/拼音码）
     */
    @Query("SELECT m FROM Medicine m WHERE m.status = 1 AND (m.name LIKE %:keyword% OR m.pinyinCode LIKE %:keyword%)")
    List<Medicine> searchByKeyword(@Param("keyword") String keyword);

    boolean existsByNameAndSpec(String name, String spec);
    boolean existsByNameAndSpecAndIdNot(String name, String spec, Long id);
}
