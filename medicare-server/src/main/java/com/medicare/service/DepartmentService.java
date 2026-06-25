package com.medicare.service;

import com.medicare.entity.Department;
import com.medicare.exception.BusinessException;
import com.medicare.repository.DepartmentRepository;
import com.medicare.util.CodeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 科室服务 — 科室 CRUD，名称唯一性校验
 */
@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public List<Department> findAll() {
        return departmentRepository.findAll();
    }

    public Department findById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("科室不存在"));
    }

    public Department create(Department dept) {
        if (departmentRepository.existsByName(dept.getName())) {
            throw new BusinessException("科室名称已存在");
        }
        dept = departmentRepository.save(dept);
        dept.setCode(CodeUtils.generateCode("DEP", dept.getId()));
        return departmentRepository.save(dept);
    }

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

    public void delete(Long id) {
        if (!departmentRepository.existsById(id)) {
            throw new BusinessException("科室不存在");
        }
        departmentRepository.deleteById(id);
    }
}
