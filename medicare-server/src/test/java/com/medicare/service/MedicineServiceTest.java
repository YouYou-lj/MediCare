package com.medicare.service;

import com.medicare.entity.Medicine;
import com.medicare.exception.BusinessException;
import com.medicare.repository.InventoryLogRepository;
import com.medicare.repository.MedicineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * MedicineService 单元测试 — 覆盖药品 CRUD + 搜索 + 出入库 + 唯一性校验
 */
@ExtendWith(MockitoExtension.class)
class MedicineServiceTest {

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private InventoryLogRepository inventoryLogRepository;

    @InjectMocks
    private MedicineService medicineService;

    private Medicine sampleMedicine;

    @BeforeEach
    void setUp() {
        sampleMedicine = new Medicine();
        sampleMedicine.setId(1L);
        sampleMedicine.setName("阿莫西林胶囊");
        sampleMedicine.setSpec("0.25g*24粒");
        sampleMedicine.setUnit("盒");
        sampleMedicine.setStock(100);
        sampleMedicine.setSafetyStock(20);
        sampleMedicine.setPrice(java.math.BigDecimal.valueOf(12.50));
        sampleMedicine.setPinyinCode("AMX");
        sampleMedicine.setManufacturer("测试药厂");
        sampleMedicine.setStatus(1);
    }

    @Test
    void findAll_shouldReturnActiveList() {
        when(medicineRepository.findByStatus(1)).thenReturn(Collections.singletonList(sampleMedicine));
        List<Medicine> result = medicineService.findAll();
        assertEquals(1, result.size());
        assertEquals("阿莫西林胶囊", result.get(0).getName());
    }

    @Test
    void search_shouldReturnList() {
        when(medicineRepository.searchByKeyword("阿莫")).thenReturn(Collections.singletonList(sampleMedicine));
        List<Medicine> result = medicineService.search("阿莫");
        assertEquals(1, result.size());
    }

    @Test
    void findLowStock_shouldReturnList() {
        when(medicineRepository.findLowStockMedicines()).thenReturn(Collections.singletonList(sampleMedicine));
        List<Medicine> result = medicineService.findLowStock();
        assertEquals(1, result.size());
    }

    @Test
    void findById_existing_shouldReturnMedicine() {
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(sampleMedicine));
        Medicine result = medicineService.findById(1L);
        assertEquals("阿莫西林胶囊", result.getName());
    }

    @Test
    void findById_notFound_shouldThrowBusinessException() {
        when(medicineRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> medicineService.findById(1L));
    }

    @Test
    void create_success_shouldReturnSavedMedicine() {
        when(medicineRepository.existsByNameAndSpec("阿莫西林胶囊", "0.25g*24粒")).thenReturn(false);
        when(medicineRepository.save(any(Medicine.class))).thenReturn(sampleMedicine);
        Medicine result = medicineService.create(sampleMedicine);
        assertEquals("阿莫西林胶囊", result.getName());
    }

    @Test
    void create_duplicate_shouldThrowBusinessException() {
        when(medicineRepository.existsByNameAndSpec("阿莫西林胶囊", "0.25g*24粒")).thenReturn(true);
        assertThrows(BusinessException.class, () -> medicineService.create(sampleMedicine));
    }

    @Test
    void update_success_shouldReturnUpdatedMedicine() {
        Medicine updated = new Medicine();
        updated.setName("阿莫西林胶囊");
        updated.setSpec("0.25g*24粒");
        updated.setUnit("盒");
        updated.setSafetyStock(30);
        updated.setPrice(java.math.BigDecimal.valueOf(15.00));
        updated.setManufacturer("新测试药厂");
        updated.setStatus(1);

        when(medicineRepository.findById(1L)).thenReturn(Optional.of(sampleMedicine));
        when(medicineRepository.existsByNameAndSpecAndIdNot("阿莫西林胶囊", "0.25g*24粒", 1L)).thenReturn(false);
        when(medicineRepository.save(any(Medicine.class))).thenReturn(updated);

        Medicine result = medicineService.update(1L, updated);
        assertEquals("新测试药厂", result.getManufacturer());
    }

    @Test
    void update_duplicate_shouldThrowBusinessException() {
        Medicine updated = new Medicine();
        updated.setName("阿莫西林胶囊");
        updated.setSpec("0.25g*24粒");

        when(medicineRepository.findById(1L)).thenReturn(Optional.of(sampleMedicine));
        when(medicineRepository.existsByNameAndSpecAndIdNot("阿莫西林胶囊", "0.25g*24粒", 1L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> medicineService.update(1L, updated));
    }

    @Test
    void delete_existing_shouldSucceed() {
        when(medicineRepository.existsById(1L)).thenReturn(true);
        doNothing().when(medicineRepository).deleteById(1L);
        assertDoesNotThrow(() -> medicineService.delete(1L));
    }

    @Test
    void delete_notFound_shouldThrowBusinessException() {
        when(medicineRepository.existsById(1L)).thenReturn(false);
        assertThrows(BusinessException.class, () -> medicineService.delete(1L));
    }

    @Test
    void stockIn_shouldIncreaseStock() {
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(sampleMedicine));
        when(medicineRepository.save(any(Medicine.class))).thenReturn(sampleMedicine);
        when(inventoryLogRepository.save(any())).thenReturn(null);

        medicineService.stockIn(1L, 50, "B20260601", "2027-06-01", "admin", "测试入库");
        assertEquals(150, sampleMedicine.getStock());
    }

    @Test
    void stockOut_success_shouldDecreaseStock() {
        when(medicineRepository.safeDecrementStock(1L, 10)).thenReturn(1);
        when(inventoryLogRepository.save(any())).thenReturn(null);

        assertDoesNotThrow(() -> medicineService.stockOut(1L, 10, "B20260601", "", "admin", "测试出库"));
    }

    @Test
    void stockOut_insufficient_shouldThrowBusinessException() {
        when(medicineRepository.safeDecrementStock(1L, 99999)).thenReturn(0);
        assertThrows(BusinessException.class, () -> medicineService.stockOut(1L, 99999, "", "", "admin", ""));
    }
}
