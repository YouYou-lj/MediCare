package com.medicare.service;

import com.medicare.dto.ScheduleVO;
import com.medicare.entity.Schedule;
import com.medicare.exception.BusinessException;
import com.medicare.repository.DoctorRepository;
import com.medicare.repository.ScheduleRepository;
import com.medicare.util.CodeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 排班服务 — 排班 CRUD + 可用号源查询。
 * <p>
 * 创建排班时自动将 remainSlots 设为 totalSlots；
 * 挂号时通过 ScheduleRepository.decrementRemain() 原子扣减号源。
 * 查询结果使用 Redis 缓存，写操作后自动清理缓存。
 */
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "schedules")
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final DoctorRepository doctorRepository;

    /**
     * 排班列表（接口投影）—— 不做 Redis 缓存，因为 JDK 动态代理类无法反序列化。
     */
    public List<ScheduleVO> findScheduleVOList(LocalDate date, Long deptId) {
        return scheduleRepository.findScheduleVOList(date, deptId);
    }

    /**
     * 可用号源列表（接口投影）—— 不做 Redis 缓存，因为 JDK 动态代理类无法反序列化。
     */
    public List<ScheduleVO> findAvailableSchedules(LocalDate date, Long deptId) {
        return scheduleRepository.findAvailableSchedules(date, deptId);
    }

    @Cacheable(key = "#id", unless = "#result == null")
    public Schedule findById(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("排班不存在"));
    }

    @CacheEvict(allEntries = true)
    public Schedule create(Schedule schedule) {
        if (!doctorRepository.existsById(schedule.getDoctorId())) {
            throw new BusinessException("医生不存在");
        }
        schedule.setRemainSlots(schedule.getTotalSlots());
        schedule = scheduleRepository.save(schedule);
        schedule.setCode(CodeUtils.generateCode("SCH", schedule.getId()));
        return scheduleRepository.save(schedule);
    }

    @CacheEvict(allEntries = true)
    public Schedule update(Long id, Schedule schedule) {
        Schedule existing = findById(id);
        if (!doctorRepository.existsById(schedule.getDoctorId())) {
            throw new BusinessException("医生不存在");
        }
        existing.setDoctorId(schedule.getDoctorId());
        existing.setWorkDate(schedule.getWorkDate());
        existing.setTimeSlot(schedule.getTimeSlot());
        existing.setTotalSlots(schedule.getTotalSlots());
        existing.setRemainSlots(schedule.getRemainSlots());
        return scheduleRepository.save(existing);
    }

    @CacheEvict(allEntries = true)
    public void delete(Long id) {
        if (!scheduleRepository.existsById(id)) {
            throw new BusinessException("排班不存在");
        }
        scheduleRepository.deleteById(id);
    }
}
