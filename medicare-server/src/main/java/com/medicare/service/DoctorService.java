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
        return doctorRepository.findDoctorVOList(deptId);
    }

    public List<Doctor> findByDepartmentId(Long deptId) {
        return doctorRepository.findByDepartmentIdAndStatus(deptId, 1);
    }

    public Doctor findById(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new BusinessException("医生不存在"));
    }

    public Doctor create(Doctor doctor) {
        if (!departmentRepository.existsById(doctor.getDepartmentId())) {
            throw new BusinessException("所属科室不存在");
        }
        return doctorRepository.save(doctor);
    }

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

    public void delete(Long id) {
        if (!doctorRepository.existsById(id)) {
            throw new BusinessException("医生不存在");
        }
        doctorRepository.deleteById(id);
    }
}
