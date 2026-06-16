package com.medicare.dao;

import org.apache.commons.dbutils.GenerousBeanProcessor;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 支持 java.time 类型的 BeanProcessor
 * DbUtils 默认 BeanProcessor 不支持 LocalDate / LocalDateTime / LocalTime 转换
 */
public class JavaTimeBeanProcessor extends GenerousBeanProcessor {

    @Override
    protected Object processColumn(ResultSet rs, int index, Class<?> propType) throws SQLException {
        if (propType == LocalDate.class) {
            Date date = rs.getDate(index);
            return date != null ? date.toLocalDate() : null;
        }
        if (propType == LocalDateTime.class) {
            Timestamp ts = rs.getTimestamp(index);
            return ts != null ? ts.toLocalDateTime() : null;
        }
        if (propType == LocalTime.class) {
            Time time = rs.getTime(index);
            return time != null ? time.toLocalTime() : null;
        }
        return super.processColumn(rs, index, propType);
    }
}
