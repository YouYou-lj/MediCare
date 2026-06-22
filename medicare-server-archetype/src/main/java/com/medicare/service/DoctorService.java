package com.medicare.service;

import com.medicare.dto.DoctorVO;
import com.medicare.entity.Doctor;
import com.medicare.exception.BusinessException;
import com.medicare.repository.DepartmentRepository;
import com.medicare.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 医生服务 — 医生 CRUD + 按科室查询，创建/更新时校验科室存在
 */
@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;

    public List<DoctorVO> findDoctorVOList(Long deptId) {
        return null;
    }

    public List<Doctor> findByDepartmentId(Long deptId) {
        return null;
    }

    public Doctor findById(Long id) {
        return null;
    }

    public Doctor create(Doctor doctor) {
        return null;
    }

    public Doctor update(Long id, Doctor doctor) {
        return null;
    }

    public void delete(Long id) {
    }
}
