package com.medicare.service;

import com.medicare.common.CacheKey;
import com.medicare.entity.Department;
import com.medicare.exception.BusinessException;
import com.medicare.repository.DepartmentRepository;
import com.medicare.util.CodeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 科室服务 — 科室 CRUD，名称唯一性校验。
 * <p>
 * 查询结果使用 Redis 缓存，写操作后自动清理缓存。
 */
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "departments")
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    @Cacheable(key = "'all'", unless = "#result == null || #result.isEmpty()")
    public List<Department> findAll() {
        return departmentRepository.findAll();
    }

    @Cacheable(key = "#id", unless = "#result == null")
    public Department findById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("科室不存在"));
    }

    @CacheEvict(allEntries = true)
    public Department create(Department dept) {
        if (departmentRepository.existsByName(dept.getName())) {
            throw new BusinessException("科室名称已存在");
        }
        dept = departmentRepository.save(dept);
        dept.setCode(CodeUtils.generateCode("DEP", dept.getId()));
        return departmentRepository.save(dept);
    }

    @CacheEvict(allEntries = true)
    public Department update(Long id, Department dept) {
        Department existing = findById(id);
        if (departmentRepository.existsByNameAndIdNot(dept.getName(), id)) {
            throw new BusinessException("科室名称已存在");
        }
        existing.setName(dept.getName());
        existing.setLocation(dept.getLocation());
        existing.setPhone(dept.getPhone());
        return departmentRepository.save(existing);
    }

    @CacheEvict(allEntries = true)
    public void delete(Long id) {
        if (!departmentRepository.existsById(id)) {
            throw new BusinessException("科室不存在");
        }
        departmentRepository.deleteById(id);
    }
}
