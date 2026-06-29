package com.medicare.common;

/**
 * 统一缓存 Key 常量与生成工具，避免魔法字符串散落。
 */
public final class CacheKey {

    private CacheKey() {}

    public static final String PREFIX = "medicare";

    public static final String DEPARTMENT_ALL = PREFIX + ":departments:all";
    public static final String DEPARTMENT_DETAIL = PREFIX + ":department:{id}";

    public static final String DOCTOR_ALL = PREFIX + ":doctors:all";
    public static final String DOCTOR_DETAIL = PREFIX + ":doctor:{id}";
    public static final String DOCTOR_BY_DEPARTMENT = PREFIX + ":doctors:dept:{deptId}";

    public static final String SCHEDULE_DETAIL = PREFIX + ":schedule:{id}";
    public static final String SCHEDULE_LIST = PREFIX + ":schedules:list";
    public static final String SCHEDULE_AVAILABLE = PREFIX + ":schedules:available";

    public static final String MEDICINE_DETAIL = PREFIX + ":medicine:{id}";
    public static final String MEDICINE_ALL = PREFIX + ":medicines:all";
    public static final String MEDICINE_LOW_STOCK = PREFIX + ":medicines:low-stock";

    public static final String USER_DETAIL = PREFIX + ":user:{id}";
    public static final String USER_BY_USERNAME = PREFIX + ":user:username:{username}";

    public static final String DASHBOARD_STATS = PREFIX + ":dashboard:stats";

    public static String departmentDetail(Long id) {
        return DEPARTMENT_DETAIL.replace("{id}", String.valueOf(id));
    }

    public static String doctorDetail(Long id) {
        return DOCTOR_DETAIL.replace("{id}", String.valueOf(id));
    }

    public static String doctorsByDepartment(Long deptId) {
        return DOCTOR_BY_DEPARTMENT.replace("{deptId}", String.valueOf(deptId));
    }

    public static String scheduleDetail(Long id) {
        return SCHEDULE_DETAIL.replace("{id}", String.valueOf(id));
    }

    public static String medicineDetail(Long id) {
        return MEDICINE_DETAIL.replace("{id}", String.valueOf(id));
    }

    public static String userDetail(Long id) {
        return USER_DETAIL.replace("{id}", String.valueOf(id));
    }

    public static String userByUsername(String username) {
        return USER_BY_USERNAME.replace("{username}", username);
    }
}
