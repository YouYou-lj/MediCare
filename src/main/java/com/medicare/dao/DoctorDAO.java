package com.medicare.dao;

import com.medicare.model.Doctor;

import java.sql.SQLException;
import java.util.List;

/**
 * 医生数据访问对象
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class DoctorDAO extends BaseDAO<Doctor> {

    private static final String SQL_INSERT =
            "INSERT INTO doctor (name, department_id, title, status) VALUES (?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE doctor SET name = ?, department_id = ?, title = ?, status = ? WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM doctor WHERE id = ?";

    private static final String SQL_SELECT_BY_ID =
            "SELECT d.id, d.name, d.department_id, d.title, d.status, d.create_time, d.update_time, " +
            "dep.name AS departmentName " +
            "FROM doctor d LEFT JOIN department dep ON d.department_id = dep.id WHERE d.id = ?";

    private static final String SQL_SELECT_ALL =
            "SELECT d.id, d.name, d.department_id, d.title, d.status, d.create_time, d.update_time, " +
            "dep.name AS departmentName " +
            "FROM doctor d LEFT JOIN department dep ON d.department_id = dep.id " +
            "ORDER BY d.department_id, d.id";

    private static final String SQL_SELECT_BY_DEPT =
            "SELECT d.id, d.name, d.department_id, d.title, d.status, d.create_time, d.update_time, " +
            "dep.name AS departmentName " +
            "FROM doctor d LEFT JOIN department dep ON d.department_id = dep.id " +
            "WHERE d.department_id = ? AND d.status = 1 ORDER BY d.id";

    private static final String SQL_SELECT_BY_NAME =
            "SELECT d.id, d.name, d.department_id, d.title, d.status, d.create_time, d.update_time, " +
            "dep.name AS departmentName " +
            "FROM doctor d LEFT JOIN department dep ON d.department_id = dep.id " +
            "WHERE d.name LIKE ? ORDER BY d.department_id, d.id";

    private static final String SQL_COUNT_BY_NAME_DEPT =
            "SELECT COUNT(*) FROM doctor WHERE name = ? AND department_id = ?";

    public Long insert(Doctor doctor) throws SQLException {
        return executeInsert(SQL_INSERT,
                doctor.getName(), doctor.getDepartmentId(), doctor.getTitle(), doctor.getStatus());
    }

    public int update(Doctor doctor) throws SQLException {
        return executeUpdate(SQL_UPDATE,
                doctor.getName(), doctor.getDepartmentId(), doctor.getTitle(), doctor.getStatus(), doctor.getId());
    }

    public int delete(Long id) throws SQLException {
        return executeUpdate(SQL_DELETE, id);
    }

    public Doctor findById(Long id) throws SQLException {
        return querySingle(SQL_SELECT_BY_ID, id);
    }

    public List<Doctor> findAll() throws SQLException {
        return queryList(SQL_SELECT_ALL);
    }

    public List<Doctor> findByDepartment(Long deptId) throws SQLException {
        return queryList(SQL_SELECT_BY_DEPT, deptId);
    }

    public List<Doctor> findByName(String name) throws SQLException {
        return queryList(SQL_SELECT_BY_NAME, "%" + name + "%");
    }

    public boolean existsByNameAndDept(String name, Long deptId) throws SQLException {
        Long count = queryScalar(SQL_COUNT_BY_NAME_DEPT, name, deptId);
        return count != null && count > 0;
    }
}
