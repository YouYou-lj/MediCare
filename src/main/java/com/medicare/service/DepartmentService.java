package com.medicare.service;

import com.medicare.dao.DepartmentDAO;
import com.medicare.model.Department;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

/**
 * 科室管理服务
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class DepartmentService {

    private static final Logger logger = LoggerFactory.getLogger(DepartmentService.class);

    private final DepartmentDAO departmentDAO = new DepartmentDAO();

    public Long addDepartment(Department dept) throws SQLException, IllegalArgumentException {
        validate(dept);
        if (departmentDAO.existsByName(dept.getName())) {
            throw new IllegalArgumentException("科室名称已存在: " + dept.getName());
        }
        Long id = departmentDAO.insert(dept);
        logger.info("新增科室: id={}, name={}", id, dept.getName());
        return id;
    }

    public void updateDepartment(Department dept) throws SQLException, IllegalArgumentException {
        if (dept.getId() == null) {
            throw new IllegalArgumentException("科室 ID 不能为空");
        }
        validate(dept);
        if (departmentDAO.existsByName(dept.getName(), dept.getId())) {
            throw new IllegalArgumentException("科室名称已被其他科室使用: " + dept.getName());
        }
        int rows = departmentDAO.update(dept);
        if (rows == 0) {
            throw new IllegalArgumentException("科室不存在");
        }
        logger.info("更新科室: id={}, name={}", dept.getId(), dept.getName());
    }

    public void deleteDepartment(Long id) throws SQLException, IllegalArgumentException {
        if (departmentDAO.hasDoctors(id)) {
            throw new IllegalArgumentException("该科室下存在医生，无法删除");
        }
        int rows = departmentDAO.delete(id);
        if (rows == 0) {
            throw new IllegalArgumentException("科室不存在");
        }
        logger.info("删除科室: id={}", id);
    }

    public Department getById(Long id) throws SQLException {
        return departmentDAO.findById(id);
    }

    public List<Department> listAll() throws SQLException {
        return departmentDAO.findAll();
    }

    public List<Department> search(String keyword) throws SQLException {
        if (keyword == null || keyword.trim().isEmpty()) {
            return listAll();
        }
        return departmentDAO.findByName(keyword.trim());
    }

    private void validate(Department dept) {
        if (dept == null) {
            throw new IllegalArgumentException("科室信息不能为空");
        }
        if (dept.getName() == null || dept.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("科室名称不能为空");
        }
    }
}
