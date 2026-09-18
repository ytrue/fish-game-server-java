package com.ytrue.game.framework.engine.utils;

import java.time.LocalDate;
import java.time.temporal.WeekFields;

/**
 * 日期工具类。
 *
 * <p>基于 {@code java.time} 包提供的现代日期 API（Java 8 引入），用于替代传统的
 * {@link java.util.Date} 与 {@link java.util.Calendar}。相比旧 API，{@code java.time}
 * 的日期对象不可变、线程安全、时区语义清晰，更适合在高并发的游戏服务端中使用。</p>
 *
 * @since 1.0.0
 */
public final class DateUtils {

    /**
     * 默认时间：2000-01-01 00:00:00（UTC）对应的毫秒时间戳。
     *
     * <p>常作为「未设置 / 从未发生」的时间哨兵值使用。</p>
     */
    public static final long DEFAULT_TIME = 946656000000L;

    /**
     * 私有构造器，禁止实例化（纯工具类）。
     */
    private DateUtils() {
    }

    /**
     * 判断两个日期是否为同一天。
     *
     * @param day1 第一个日期
     * @param day2 第二个日期
     * @return 若两个日期为同一天则返回 {@code true}，否则返回 {@code false}
     */
    public static boolean isSameDay(LocalDate day1, LocalDate day2) {
        return day1.equals(day2);
    }

    /**
     * 判断两个日期是否属于同一周（以周一为一周的第一天）。
     *
     * <p>采用 ISO 8601 周历规则（周一为一周开始，每年度第一周至少含 4 天）。
     * 通过「基于周的年份」（week-based year）与「周序号」共同比较，正确处理跨年边界
     * （例如 12 月底与次年 1 月初可能同属第 1 周），无需再像旧实现那样手工判断月份。</p>
     *
     * @param day1 第一个日期
     * @param day2 第二个日期
     * @return 若两个日期属于同一周则返回 {@code true}，否则返回 {@code false}
     */
    public static boolean isSameWeek(LocalDate day1, LocalDate day2) {
        WeekFields isoWeek = WeekFields.ISO;
        return day1.get(isoWeek.weekBasedYear()) == day2.get(isoWeek.weekBasedYear())
                && day1.get(isoWeek.weekOfWeekBasedYear()) == day2.get(isoWeek.weekOfWeekBasedYear());
    }

}
