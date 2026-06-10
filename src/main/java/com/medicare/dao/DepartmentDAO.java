package com.medicare.dao;

import com.medicare.model.Department;

import java.sql.SQLException;
import java.util.List;

/**
 * 科室数据访问对象
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class DepartmentDAO extends BaseDAO<Department> {

    private static final String SQL_INSERT =
            "INSERT INTO department (name, location, phone) VALUES (?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE department SET name = ?, location = ?, phone = ? WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM department WHERE id = ?";

    private static final String SQL_SELECT_BY_ID =
            "SELECT id, name, location, phone, create_time, update_time FROM department WHERE id = ?";

    private static final String SQL_SELECT_ALL =
            "SELECT id, name, location, phone, create_time, update_time FROM department ORDER BY id";

    private static final String SQL_SELECT_BY_NAME =
            "SELECT id, name, location, phone, create_time, update_time FROM department WHERE name LIKE ? ORDER BY id";

    private static final String SQL_COUNT_BY_NAME =
            "SELECT COUNT(*) FROM department WHERE name = ?";

    private static final String SQL_COUNT_BY_NAME_EXCLUDE_ID =
            "SELECT COUNT(*) FROM department WHERE name = ? AND id != ?";

    private static final String SQL_COUNT_DOCTORS =
            "SELECT COUNT(*) FROM doctor WHERE department_id = ?";

    public Long insert(Department dept) throws SQLException {
        return executeInsert(SQL_INSERT, dept.getName(), dept.getLocation(), dept.getPhone());
    }

    public int update(Department dept) throws SQLException {
        return executeUpdate(SQL_UPDATE, dept.getName(), dept.getLocation(), dept.getPhone(), dept.getId());
    }

    public int delete(Long id) throws SQLException {
        return executeUpdate(SQL_DELETE, id);
    }

    public Department findById(Long id) throws SQLException {
        return querySingle(SQL_SELECT_BY_ID, id);
    }

    public List<Department> findAll() throws SQLException {
        return queryList(SQL_SELECT_ALL);
    }

    public List<Department> findByName(String name) throws SQLException {
        return queryList(SQL_SELECT_BY_NAME, "%" + name + "%");
    }

    public boolean existsByName(String name) throws SQLException {
        Long count = queryScalar(SQL_COUNT_BY_NAME, name);
        return count != null && count > 0;
    }

    public boolean existsByName(String name, Long excludeId) throws SQLException {
        Long count = queryScalar(SQL_COUNT_BY_NAME_EXCLUDE_ID, name, excludeId);
        return count != null && count > 0;
    }

    /**
     * 检查科室下是否有医生
     */
    public boolean hasDoctors(Long deptId) throws SQLException {
        Long count = queryScalar(SQL_COUNT_DOCTORS, deptId);
        return count != null && count > 0;
    }
}
