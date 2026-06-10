package com.medicare.service;

import com.medicare.dao.DoctorDAO;
import com.medicare.model.Doctor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

/**
 * 医生管理服务
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class DoctorService {

    private static final Logger logger = LoggerFactory.getLogger(DoctorService.class);

    private final DoctorDAO doctorDAO = new DoctorDAO();

    public static final String TITLE_CHIEF = "主任医师";
    public static final String TITLE_VICE_CHIEF = "副主任医师";
    public static final String TITLE_ATTENDING = "主治医师";
    public static final String TITLE_RESIDENT = "医师";

    public static final List<String> TITLES = List.of(
            TITLE_CHIEF, TITLE_VICE_CHIEF, TITLE_ATTENDING, TITLE_RESIDENT
    );

    public Long addDoctor(Doctor doctor) throws SQLException, IllegalArgumentException {
        validate(doctor);
        if (doctorDAO.existsByNameAndDept(doctor.getName(), doctor.getDepartmentId())) {
            throw new IllegalArgumentException("该科室下已存在同名医生");
        }
        if (doctor.getStatus() == null) {
            doctor.setStatus(1);
        }
        Long id = doctorDAO.insert(doctor);
        logger.info("新增医生: id={}, name={}, deptId={}", id, doctor.getName(), doctor.getDepartmentId());
        return id;
    }

    public void updateDoctor(Doctor doctor) throws SQLException, IllegalArgumentException {
        if (doctor.getId() == null) {
            throw new IllegalArgumentException("医生 ID 不能为空");
        }
        validate(doctor);
        int rows = doctorDAO.update(doctor);
        if (rows == 0) {
            throw new IllegalArgumentException("医生不存在");
        }
        logger.info("更新医生: id={}, name={}", doctor.getId(), doctor.getName());
    }

    public void deleteDoctor(Long id) throws SQLException, IllegalArgumentException {
        int rows = doctorDAO.delete(id);
        if (rows == 0) {
            throw new IllegalArgumentException("医生不存在");
        }
        logger.info("删除医生: id={}", id);
    }

    public Doctor getById(Long id) throws SQLException {
        return doctorDAO.findById(id);
    }

    public List<Doctor> listAll() throws SQLException {
        return doctorDAO.findAll();
    }

    public List<Doctor> listByDepartment(Long deptId) throws SQLException {
        return doctorDAO.findByDepartment(deptId);
    }

    public List<Doctor> search(String keyword) throws SQLException {
        if (keyword == null || keyword.trim().isEmpty()) {
            return listAll();
        }
        return doctorDAO.findByName(keyword.trim());
    }

    private void validate(Doctor doctor) {
        if (doctor == null) {
            throw new IllegalArgumentException("医生信息不能为空");
        }
        if (doctor.getName() == null || doctor.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("医生姓名不能为空");
        }
        if (doctor.getDepartmentId() == null) {
            throw new IllegalArgumentException("所属科室不能为空");
        }
        if (doctor.getTitle() == null || doctor.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("职称不能为空");
        }
    }
}
