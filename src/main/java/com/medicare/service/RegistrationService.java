package com.medicare.service;

import com.medicare.dao.RegistrationDAO;
import com.medicare.dao.ScheduleDAO;
import com.medicare.model.Registration;
import com.medicare.model.Schedule;
import com.medicare.util.ConnectionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * 挂号预约服务
 * 核心业务流程：挂号时事务级扣减号源
 * 事务控制统一在 Service 层
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class RegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationService.class);

    private final RegistrationDAO registrationDAO = new RegistrationDAO();
    private final ScheduleDAO scheduleDAO = new ScheduleDAO();

    /**
     * 挂号登记（事务控制）
     * 1. 校验号源是否充足
     * 2. 扣减号源
     * 3. 生成挂号记录
     * 4. 分配序号
     */
    public Long register(Registration reg) throws SQLException, IllegalArgumentException {
        if (reg.getPatientId() == null) {
            throw new IllegalArgumentException("患者不能为空");
        }
        if (reg.getScheduleId() == null) {
            throw new IllegalArgumentException("号源不能为空");
        }

        // 获取号源信息
        Schedule schedule = scheduleDAO.findById(reg.getScheduleId());
        if (schedule == null) {
            throw new IllegalArgumentException("号源不存在");
        }
        if (schedule.getRemainSlots() == null || schedule.getRemainSlots() <= 0) {
            throw new IllegalArgumentException("该时段号源已售罄");
        }

        Connection conn = null;
        try {
            conn = registrationDAO.getConnection();

            // 1. 扣减号源
            int rows = scheduleDAO.decrementRemain(reg.getScheduleId());
            if (rows == 0) {
                throw new IllegalArgumentException("号源不足，请刷新后重试");
            }

            // 2. 分配序号
            Integer seqNo = registrationDAO.getMaxSeqNo(LocalDate.now()) + 1;
            reg.setSeqNo(seqNo);
            reg.setStatus(Registration.STATUS_WAITING);
            if (reg.getFee() == null) {
                reg.setFee(BigDecimal.valueOf(10.00)); // 默认挂号费
            }

            // 3. 保存挂号记录
            Long id = registrationDAO.insert(conn, reg);

            registrationDAO.commit(conn);
            logger.info("挂号成功: id={}, patientId={}, scheduleId={}, seqNo={}",
                    id, reg.getPatientId(), reg.getScheduleId(), seqNo);
            return id;

        } catch (SQLException e) {
            registrationDAO.rollback(conn);
            logger.error("挂号失败，事务已回滚", e);
            throw e;
        } catch (IllegalArgumentException e) {
            registrationDAO.rollback(conn);
            throw e;
        } finally {
            registrationDAO.closeConnection(conn);
        }
    }

    /**
     * 取消挂号（事务控制）
     * 1. 释放号源
     * 2. 删除挂号记录
     */
    public void cancelRegistration(Long regId) throws SQLException, IllegalArgumentException {
        Registration reg = registrationDAO.findById(regId);
        if (reg == null) {
            throw new IllegalArgumentException("挂号记录不存在");
        }
        if (reg.getStatus() == Registration.STATUS_COMPLETED) {
            throw new IllegalArgumentException("已完成的挂号不能取消");
        }

        Connection conn = null;
        try {
            conn = registrationDAO.getConnection();

            // 1. 释放号源
            scheduleDAO.incrementRemain(reg.getScheduleId());

            // 2. 删除挂号记录
            registrationDAO.delete(conn, regId);

            registrationDAO.commit(conn);
            logger.info("取消挂号成功: regId={}", regId);

        } catch (SQLException e) {
            registrationDAO.rollback(conn);
            logger.error("取消挂号失败，事务已回滚", e);
            throw e;
        } finally {
            registrationDAO.closeConnection(conn);
        }
    }

    /**
     * 叫号（状态变更为就诊中）
     */
    public void callPatient(Long regId) throws SQLException, IllegalArgumentException {
        Registration reg = registrationDAO.findById(regId);
        if (reg == null) {
            throw new IllegalArgumentException("挂号记录不存在");
        }
        if (reg.getStatus() != Registration.STATUS_WAITING) {
            throw new IllegalArgumentException("仅候诊状态可叫号");
        }
        registrationDAO.updateStatus(regId, Registration.STATUS_IN_PROGRESS);
        logger.info("叫号成功: regId={}, patient={}", regId, reg.getPatientName());
    }

    /**
     * 完成就诊
     */
    public void completeRegistration(Long regId) throws SQLException, IllegalArgumentException {
        Registration reg = registrationDAO.findById(regId);
        if (reg == null) {
            throw new IllegalArgumentException("挂号记录不存在");
        }
        if (reg.getStatus() == Registration.STATUS_COMPLETED) {
            throw new IllegalArgumentException("该挂号已就诊完成");
        }
        registrationDAO.updateStatus(regId, Registration.STATUS_COMPLETED);
        logger.info("就诊完成: regId={}, patient={}", regId, reg.getPatientName());
    }

    // ============================================================
    // 查询方法
    // ============================================================

    public Registration getById(Long id) throws SQLException {
        return registrationDAO.findById(id);
    }

    public List<Registration> listToday() throws SQLException {
        return registrationDAO.findToday(LocalDate.now());
    }

    public List<Registration> listByPatient(Long patientId) throws SQLException {
        return registrationDAO.findByPatient(patientId);
    }

    public List<Registration> listByStatus(Integer status) throws SQLException {
        return registrationDAO.findByStatus(status);
    }

    public List<Registration> listWaitingByDoctor(Long doctorId) throws SQLException {
        return registrationDAO.findWaitingByDoctor(doctorId, LocalDate.now());
    }

    public List<Registration> listAll() throws SQLException {
        return registrationDAO.findToday(LocalDate.now());
    }
}
