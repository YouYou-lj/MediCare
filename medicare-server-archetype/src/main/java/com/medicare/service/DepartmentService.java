package com.medicare.service;

import com.medicare.entity.Department;
import com.medicare.exception.BusinessException;
import com.medicare.repository.DepartmentRepository;
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
        return null;
    }

    public Department findById(Long id) {
        return null;
    }

    public Department create(Department dept) {
        return null;
    }

    public Department update(Long id, Department dept) {
        return null;
    }

    public void delete(Long id) {
    }
}
