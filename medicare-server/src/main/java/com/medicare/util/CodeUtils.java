package com.medicare.util;

/**
 * 业务编号生成工具
 * <p>
 * 为各业务对象生成带前缀的固定长度编号，格式：PREFIX-000001。
 * 数字部分使用数据库自增主键 id，保证同一实体类型内唯一；
 * 不同实体类型使用不同前缀，保证跨类型也不重复。
 */
public final class CodeUtils {

    private static final int SERIAL_LENGTH = 6;
    private static final String SERIAL_PADDING = "0";

    private CodeUtils() {
        // 工具类禁止实例化
    }

    /**
     * 根据前缀与自增 id 生成业务编号。
     *
     * @param prefix 业务前缀，如 PAT、DOC
     * @param id     数据库自增主键
     * @return 业务编号，例如 PAT-000001
     */
    public static String generateCode(String prefix, Long id) {
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("编号前缀不能为空");
        }
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("编号所使用的 id 必须大于 0");
        }
        return prefix + "-" + String.format("%0" + SERIAL_LENGTH + "d", id);
    }
}
