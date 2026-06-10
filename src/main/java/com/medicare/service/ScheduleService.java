package com.medicare.service;

import com.medicare.dao.ScheduleDAO;
import com.medicare.model.Schedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * 排班号源管理服务
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class ScheduleService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleService.class);

    private final ScheduleDAO scheduleDAO = new ScheduleDAO();

    public static final String SLOT_MORNING = "上午";
    public static final String SLOT_AFTERNOON = "下午";
    public static final String SLOT_EVENING = "晚上";

    public static final List<String> TIME_SLOTS = List.of(SLOT_MORNING, SLOT_AFTERNOON, SLOT_EVENING);

    /**
     * 新增排班
     */
    public Long addSchedule(Schedule schedule) throws SQLException, IllegalArgumentException {
        validate(schedule);
        if (scheduleDAO.exists(schedule.getDoctorId(), schedule.getWorkDate(), schedule.getTimeSlot())) {
            throw new IllegalArgumentException("该医生在指定日期和时段已有排班");
        }
        if (schedule.getRemainSlots() == null) {
            schedule.setRemainSlots(schedule.getTotalSlots());
        }
        Long id = scheduleDAO.insert(schedule);
        logger.info("新增排班: id={}, doctorId={}, date={}, slot={}",
                id, schedule.getDoctorId(), schedule.getWorkDate(), schedule.getTimeSlot());
        return id;
    }

    /**
     * 更新排班
     */
    public void updateSchedule(Schedule schedule) throws SQLException, IllegalArgumentException {
        if (schedule.getId() == null) {
            throw new IllegalArgumentException("排班 ID 不能为空");
        }
        validate(schedule);
        int rows = scheduleDAO.update(schedule);
        if (rows == 0) {
            throw new IllegalArgumentException("排班不存在");
        }
        logger.info("更新排班: id={}", schedule.getId());
    }

    /**
     * 删除排班
     */
    public void deleteSchedule(Long id) throws SQLException, IllegalArgumentException {
        int rows = scheduleDAO.delete(id);
        if (rows == 0) {
            throw new IllegalArgumentException("排班不存在");
        }
        logger.info("删除排班: id={}", id);
    }

    /**
     * 根据 ID 查询
     */
    public Schedule getById(Long id) throws SQLException {
        return scheduleDAO.findById(id);
    }

    /**
     * 查询全部排班
     */
    public List<Schedule> listAll() throws SQLException {
        return scheduleDAO.findAll();
    }

    /**
     * 按日期查询
     */
    public List<Schedule> listByDate(LocalDate date) throws SQLException {
        return scheduleDAO.findByDate(date);
    }

    /**
     * 查询可用号源（剩余号源 > 0）
     */
    public List<Schedule> listAvailable(LocalDate fromDate) throws SQLException {
        return scheduleDAO.findAvailable(fromDate);
    }

    /**
     * 挂号时扣减号源
     * @return true 扣减成功，false 号源不足
     */
    public boolean deductSlot(Long scheduleId) throws SQLException {
        int rows = scheduleDAO.decrementRemain(scheduleId);
        if (rows > 0) {
            logger.info("号源扣减成功: scheduleId={}", scheduleId);
            return true;
        }
        logger.warn("号源不足: scheduleId={}", scheduleId);
        return false;
    }

    /**
     * 取消挂号时释放号源
     */
    public void releaseSlot(Long scheduleId) throws SQLException {
        scheduleDAO.incrementRemain(scheduleId);
        logger.info("号源释放成功: scheduleId={}", scheduleId);
    }

    private void validate(Schedule schedule) {
        if (schedule == null) {
            throw new IllegalArgumentException("排班信息不能为空");
        }
        if (schedule.getDoctorId() == null) {
            throw new IllegalArgumentException("医生不能为空");
        }
        if (schedule.getWorkDate() == null) {
            throw new IllegalArgumentException("出诊日期不能为空");
        }
        if (schedule.getTimeSlot() == null || schedule.getTimeSlot().trim().isEmpty()) {
            throw new IllegalArgumentException("时段不能为空");
        }
        if (schedule.getTotalSlots() == null || schedule.getTotalSlots() <= 0) {
            throw new IllegalArgumentException("总号源数必须大于 0");
        }
    }
}
