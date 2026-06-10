package com.medicare.dao;

import com.medicare.model.Patient;

import java.sql.SQLException;
import java.util.List;

/**
 * 患者数据访问对象
 * 所有 SQL 通过常量定义，禁止方法内拼接 SQL
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class PatientDAO extends BaseDAO<Patient> {

    // ============================================================
    // SQL 常量定义
    // ============================================================

    private static final String SQL_INSERT =
            "INSERT INTO patient (id_card, name, gender, birth_date, phone, address, allergy_info) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE patient SET name = ?, gender = ?, birth_date = ?, phone = ?, " +
            "address = ?, allergy_info = ? WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM patient WHERE id = ?";

    private static final String SQL_SELECT_BY_ID =
            "SELECT id, id_card, name, gender, birth_date, phone, address, allergy_info, " +
            "create_time, update_time FROM patient WHERE id = ?";

    private static final String SQL_SELECT_BY_ID_CARD =
            "SELECT id, id_card, name, gender, birth_date, phone, address, allergy_info, " +
            "create_time, update_time FROM patient WHERE id_card = ?";

    private static final String SQL_SELECT_ALL =
            "SELECT id, id_card, name, gender, birth_date, phone, address, allergy_info, " +
            "create_time, update_time FROM patient ORDER BY create_time DESC";

    private static final String SQL_SELECT_BY_NAME =
            "SELECT id, id_card, name, gender, birth_date, phone, address, allergy_info, " +
            "create_time, update_time FROM patient WHERE name LIKE ? ORDER BY create_time DESC";

    private static final String SQL_SELECT_BY_PHONE =
            "SELECT id, id_card, name, gender, birth_date, phone, address, allergy_info, " +
            "create_time, update_time FROM patient WHERE phone LIKE ? ORDER BY create_time DESC";

    private static final String SQL_COUNT_BY_ID_CARD =
            "SELECT COUNT(*) FROM patient WHERE id_card = ?";

    private static final String SQL_COUNT_BY_ID_CARD_EXCLUDE_ID =
            "SELECT COUNT(*) FROM patient WHERE id_card = ? AND id != ?";

    // ============================================================
    // CRUD 方法
    // ============================================================

    /**
     * 新增患者
     * @return 生成的主键
     */
    public Long insert(Patient patient) throws SQLException {
        return executeInsert(SQL_INSERT,
                patient.getIdCard(),
                patient.getName(),
                patient.getGender(),
                patient.getBirthDate(),
                patient.getPhone(),
                patient.getAddress(),
                patient.getAllergyInfo()
        );
    }

    /**
     * 更新患者
     */
    public int update(Patient patient) throws SQLException {
        return executeUpdate(SQL_UPDATE,
                patient.getName(),
                patient.getGender(),
                patient.getBirthDate(),
                patient.getPhone(),
                patient.getAddress(),
                patient.getAllergyInfo(),
                patient.getId()
        );
    }

    /**
     * 删除患者
     */
    public int delete(Long id) throws SQLException {
        return executeUpdate(SQL_DELETE, id);
    }

    /**
     * 根据 ID 查询
     */
    public Patient findById(Long id) throws SQLException {
        return querySingle(SQL_SELECT_BY_ID, id);
    }

    /**
     * 根据身份证号查询
     */
    public Patient findByIdCard(String idCard) throws SQLException {
        return querySingle(SQL_SELECT_BY_ID_CARD, idCard);
    }

    /**
     * 查询全部患者
     */
    public List<Patient> findAll() throws SQLException {
        return queryList(SQL_SELECT_ALL);
    }

    /**
     * 按姓名模糊查询
     */
    public List<Patient> findByName(String name) throws SQLException {
        return queryList(SQL_SELECT_BY_NAME, "%" + name + "%");
    }

    /**
     * 按手机号模糊查询
     */
    public List<Patient> findByPhone(String phone) throws SQLException {
        return queryList(SQL_SELECT_BY_PHONE, "%" + phone + "%");
    }

    // ============================================================
    // 业务校验方法
    // ============================================================

    /**
     * 检查身份证号是否已存在
     */
    public boolean existsByIdCard(String idCard) throws SQLException {
        Long count = queryScalar(SQL_COUNT_BY_ID_CARD, idCard);
        return count != null && count > 0;
    }

    /**
     * 检查身份证号是否已被其他患者使用
     */
    public boolean existsByIdCard(String idCard, Long excludeId) throws SQLException {
        Long count = queryScalar(SQL_COUNT_BY_ID_CARD_EXCLUDE_ID, idCard, excludeId);
        return count != null && count > 0;
    }
}
