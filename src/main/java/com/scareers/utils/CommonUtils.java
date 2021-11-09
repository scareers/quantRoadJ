package com.scareers.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;

import java.util.Arrays;
import java.util.List;

/**
 * description:
 *
 * @author: admin
 * @date: 2021/11/6  0006-23:38
 */
public class CommonUtils {
    public static void main(String[] args) {
        Console.log(changeStatRangeForFull(Arrays.asList("20120102", "20130102")));
    }

    /**
     * # -- 获取读取数据的区间, 并非真实统计区间. 他会更大
     * # 给定统计的日期区间, 但是往往访问数据库, 需要的实际数据会超过这个区间,
     * # 本函数返回 新的日期区间, 它包括了给定区间, 并且前后都有 冗余的日期.
     * # python 的实现, 是 读取设置中的 settings.date_ranges, 取前一个设置和后一个设置的start/end作为新区间.
     * # java 实现修改: 将start区间提前1年, end区间延后一年即可. 该方法更加方便.
     * # 两种实现, 都需要注意的点: 末尾区间同样会缺少最后的几次统计.
     *
     * @param statRange : 8位标准的日期形式.
     * @return 为了数据完整性, 前后更宽的statRange
     */
    public static List<String> changeStatRangeForFull(List<String> statRange) {
//        return Arrays.asList("19000101", "21000101");
        String start = statRange.get(0);
        String end = statRange.get(1);
        int startYear = DateUtil.parse(start).year() - 2;
        int endYear = DateUtil.parse(start).year() + 2;

        String startNew = StrUtil.format("{}{}", startYear, StrUtil.sub(start, 4, 8));
        String endNew = StrUtil.format("{}{}", endYear, StrUtil.sub(end, 4, 8));
        return Arrays.asList(startNew, endNew);
    }

    public static void showMemoryUsageByte() {
        Console.log("{}b - {}b == {}b", Runtime.getRuntime().totalMemory(),
                Runtime.getRuntime().freeMemory(),
                Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
    }

    public static void showMemoryUsageMB() {
        Console.log("{}M - {}M == {}M", Runtime.getRuntime().totalMemory() / 1024 / 1024,
                Runtime.getRuntime().freeMemory() / 1024 / 1024,
                Runtime.getRuntime().totalMemory() / 1024 / 1024 - Runtime.getRuntime().freeMemory() / 1024 / 1024);
    }
}
