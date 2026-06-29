package com.medicare.service;

import com.medicare.dto.DoctorVO;
import com.medicare.entity.Doctor;
import com.medicare.exception.BusinessException;
import com.medicare.repository.DepartmentRepository;
import com.medicare.repository.DoctorRepository;
import com.medicare.util.CodeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 医生服务 — 医生 CRUD + 按科室查询，创建/更新时校验科室存在。
 * <p>
 * 查询结果使用 Redis 缓存，写操作后自动清理缓存。
 */
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "doctors")
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;

    /**
     * 医生列表（接口投影）—— 不做 Redis 缓存，因为 JDK 动态代理类无法反序列化。
     */
    public List<DoctorVO> findDoctorVOList(Long deptId) {
        return doctorRepository.findDoctorVOList(deptId);
    }

    @Cacheable(key = "'dept:' + #deptId", unless = "#result == null")
    public List<Doctor> findByDepartmentId(Long deptId) {
        return doctorRepository.findByDepartmentIdAndStatus(deptId, 1);
    }

    @Cacheable(key = "#id", unless = "#result == null")
    public Doctor findById(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new BusinessException("医生不存在"));
    }

    @CacheEvict(allEntries = true)
    public Doctor create(Doctor doctor) {
        if (!departmentRepository.existsById(doctor.getDepartmentId())) {
            throw new BusinessException("所属科室不存在");
        }
        doctor = doctorRepository.save(doctor);
        doctor.setCode(CodeUtils.generateCode("DOC", doctor.getId()));
        return doctorRepository.save(doctor);
    }

    @CacheEvict(allEntries = true)
    public Doctor update(Long id, Doctor doctor) {
        Doctor existing = findById(id);
        if (!departmentRepository.existsById(doctor.getDepartmentId())) {
            throw new BusinessException("所属科室不存在");
        }
        existing.setName(doctor.getName());
        existing.setDepartmentId(doctor.getDepartmentId());
        existing.setTitle(doctor.getTitle());
        existing.setStatus(doctor.getStatus());
        return doctorRepository.save(existing);
    }

    @CacheEvict(allEntries = true)
    public void delete(Long id) {
        if (!doctorRepository.existsById(id)) {
            throw new BusinessException("医生不存在");
        }
        doctorRepository.deleteById(id);
    }
}
