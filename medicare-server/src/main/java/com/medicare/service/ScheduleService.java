package com.medicare.service;

import com.medicare.dto.ScheduleVO;
import com.medicare.entity.Schedule;
import com.medicare.exception.BusinessException;
import com.medicare.repository.DoctorRepository;
import com.medicare.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final DoctorRepository doctorRepository;

    public List<ScheduleVO> findScheduleVOList(LocalDate date, Long deptId) {
        return scheduleRepository.findScheduleVOList(date, deptId);
    }

    public List<ScheduleVO> findAvailableSchedules(LocalDate date, Long deptId) {
        return scheduleRepository.findAvailableSchedules(date, deptId);
    }

    public Schedule findById(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("排班不存在"));
    }

    public Schedule create(Schedule schedule) {
        if (!doctorRepository.existsById(schedule.getDoctorId())) {
            throw new BusinessException("医生不存在");
        }
        schedule.setRemainSlots(schedule.getTotalSlots());
        return scheduleRepository.save(schedule);
    }

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

    public void delete(Long id) {
        if (!scheduleRepository.existsById(id)) {
            throw new BusinessException("排班不存在");
        }
        scheduleRepository.deleteById(id);
    }
}
