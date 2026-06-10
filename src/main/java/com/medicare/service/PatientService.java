package com.medicare.service;

import com.medicare.dao.PatientDAO;
import com.medicare.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 患者管理服务
 * 负责业务规则校验、流程编排
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class PatientService {

    private static final Logger logger = LoggerFactory.getLogger(PatientService.class);

    private final PatientDAO patientDAO = new PatientDAO();

    // 身份证号正则（18位）
    private static final Pattern ID_CARD_PATTERN = Pattern.compile(
            "^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$"
    );

    // 手机号正则
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^1[3-9]\\d{9}$"
    );

    // ============================================================
    // 新增患者
    // ============================================================

    /**
     * 患者登记
     * 规则：身份证唯一性校验；手机号格式校验；首次就诊自动建档
     *
     * @return 新增患者的 ID
     */
    public Long registerPatient(Patient patient) throws SQLException, IllegalArgumentException {
        // 1. 参数校验
        validatePatient(patient);

        // 2. 身份证唯一性校验
        if (patientDAO.existsByIdCard(patient.getIdCard())) {
            throw new IllegalArgumentException("身份证号已存在，请勿重复建档");
        }

        // 3. 保存
        Long id = patientDAO.insert(patient);
        logger.info("患者建档成功: id={}, name={}, idCard={}", id, patient.getName(), patient.getIdCard());
        return id;
    }

    // ============================================================
    // 更新患者
    // ============================================================

    /**
     * 更新患者档案
     */
    public void updatePatient(Patient patient) throws SQLException, IllegalArgumentException {
        if (patient.getId() == null) {
            throw new IllegalArgumentException("患者 ID 不能为空");
        }

        validatePatient(patient);

        // 身份证唯一性校验（排除自身）
        if (patientDAO.existsByIdCard(patient.getIdCard(), patient.getId())) {
            throw new IllegalArgumentException("身份证号已被其他患者使用");
        }

        int rows = patientDAO.update(patient);
        if (rows == 0) {
            throw new IllegalArgumentException("患者不存在或已被删除");
        }
        logger.info("患者档案更新成功: id={}, name={}", patient.getId(), patient.getName());
    }

    // ============================================================
    // 删除患者
    // ============================================================

    /**
     * 删除患者
     * TODO: M4 阶段需检查是否存在关联挂号记录，存在则禁止删除
     */
    public void deletePatient(Long id) throws SQLException {
        int rows = patientDAO.delete(id);
        if (rows == 0) {
            throw new IllegalArgumentException("患者不存在或已被删除");
        }
        logger.info("患者删除成功: id={}", id);
    }

    // ============================================================
    // 查询方法
    // ============================================================

    /**
     * 根据 ID 查询
     */
    public Patient getById(Long id) throws SQLException {
        return patientDAO.findById(id);
    }

    /**
     * 根据身份证号查询（快速调取档案）
     */
    public Patient getByIdCard(String idCard) throws SQLException {
        return patientDAO.findByIdCard(idCard);
    }

    /**
     * 查询全部患者
     */
    public List<Patient> listAll() throws SQLException {
        return patientDAO.findAll();
    }

    /**
     * 按条件搜索
     * @param keyword 姓名或手机号关键字
     */
    public List<Patient> search(String keyword) throws SQLException {
        if (keyword == null || keyword.trim().isEmpty()) {
            return patientDAO.findAll();
        }
        String kw = keyword.trim();

        // 优先按姓名查询
        List<Patient> byName = patientDAO.findByName(kw);
        if (!byName.isEmpty()) {
            return byName;
        }

        // 再按手机号查询
        return patientDAO.findByPhone(kw);
    }

    // ============================================================
    // 私有校验方法
    // ============================================================

    private void validatePatient(Patient patient) {
        if (patient == null) {
            throw new IllegalArgumentException("患者信息不能为空");
        }
        if (patient.getIdCard() == null || patient.getIdCard().trim().isEmpty()) {
            throw new IllegalArgumentException("身份证号不能为空");
        }
        if (!ID_CARD_PATTERN.matcher(patient.getIdCard().trim()).matches()) {
            throw new IllegalArgumentException("身份证号格式不正确");
        }
        if (patient.getName() == null || patient.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("患者姓名不能为空");
        }
        if (patient.getGender() == null) {
            throw new IllegalArgumentException("性别不能为空");
        }
        if (patient.getPhone() != null && !patient.getPhone().trim().isEmpty()) {
            if (!PHONE_PATTERN.matcher(patient.getPhone().trim()).matches()) {
                throw new IllegalArgumentException("手机号格式不正确");
            }
        }
    }
}
