package com.scareers.utils;

import cn.hutool.core.lang.Console;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.apache.commons.beanutils.BeanUtils;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;

import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;


public class HardwareUtils {
    public static void main(String[] args)
            throws InterruptedException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
//        getCpuInfoAsString(true);
//        getMemInfoAsString(true);

//        SystemInfo systemInfo = new SystemInfo();
//        System.out.println(JSONUtil.toJsonPrettyStr(systemInfo.getHardware().getDiskStores()));
//        System.out.println(JSONUtil.toJsonPrettyStr(systemInfo.getHardware().getMemory()));
//        System.out.println(JSONUtil.toJsonPrettyStr(systemInfo.getHardware().getProcessor()));

        Console.log(reportCpuMemoryDisk(false));
    }

    public static String reportCpuMemoryDisk(boolean showInStdout) throws InterruptedException {
        String res = "";
        res += getCpuInfoAsString(showInStdout);
        res += getMemInfoAsString(showInStdout);
        res += getDiskInfoAsJsonString(showInStdout);
        return res;
    }

    public static String getDiskInfoAsJsonString(boolean showInStdout) {
        SystemInfo systemInfo = new SystemInfo();
        String res = "----------------磁盘信息---------------\n";
        res += JSONUtil.toJsonPrettyStr(systemInfo.getHardware().getDiskStores());
        res += "\n--------------------------------------";
        if (showInStdout) {
            Console.log(res);
        }
        return "\n" + res + "\n";
    }

    private static String getCpuInfoAsString(boolean showInStdout) throws InterruptedException {
        //System.out.println("----------------cpu信息---------------");
        SystemInfo systemInfo = new SystemInfo();
        CentralProcessor processor = systemInfo.getHardware().getProcessor();
        long[] prevTicks = processor.getSystemCpuLoadTicks();
        // 睡眠1s
        TimeUnit.SECONDS.sleep(1);
        long[] ticks = processor.getSystemCpuLoadTicks();
        long nice = ticks[CentralProcessor.TickType.NICE.getIndex()] - prevTicks[CentralProcessor.TickType.NICE
                .getIndex()];
        long irq = ticks[CentralProcessor.TickType.IRQ.getIndex()] - prevTicks[CentralProcessor.TickType.IRQ
                .getIndex()];
        long softirq = ticks[CentralProcessor.TickType.SOFTIRQ.getIndex()] - prevTicks[CentralProcessor.TickType.SOFTIRQ
                .getIndex()];
        long steal = ticks[CentralProcessor.TickType.STEAL.getIndex()] - prevTicks[CentralProcessor.TickType.STEAL
                .getIndex()];
        long cSys = ticks[CentralProcessor.TickType.SYSTEM.getIndex()] - prevTicks[CentralProcessor.TickType.SYSTEM
                .getIndex()];
        long user = ticks[CentralProcessor.TickType.USER.getIndex()] - prevTicks[CentralProcessor.TickType.USER
                .getIndex()];
        long iowait = ticks[CentralProcessor.TickType.IOWAIT.getIndex()] - prevTicks[CentralProcessor.TickType.IOWAIT
                .getIndex()];
        long idle = ticks[CentralProcessor.TickType.IDLE.getIndex()] - prevTicks[CentralProcessor.TickType.IDLE
                .getIndex()];
        long totalCpu = user + nice + cSys + idle + iowait + irq + softirq + steal;

        String res = "\n";
        res += "----------------cpu信息---------------" + "\n";
        res += "cpu核数:" + processor.getLogicalProcessorCount() + "\n";
        res += "cpu系统使用率:" + new DecimalFormat("#.##%").format(cSys * 1.0 / totalCpu) + "\n";
        res += "cpu用户使用率:" + new DecimalFormat("#.##%").format(user * 1.0 / totalCpu) + "\n";
        res += "cpu当前等待率:" + new DecimalFormat("#.##%").format(iowait * 1.0 / totalCpu) + "\n";
        res += "cpu当前使用率:" + new DecimalFormat("#.##%").format(1.0 - (idle * 1.0 / totalCpu)) + "\n";
        res += "--------------------------------------" + "\n";
        if (showInStdout) {
            System.out.println(res);
        }
        return res;
    }

    public static String getMemInfoAsString(boolean showInStdout) {
        SystemInfo systemInfo = new SystemInfo();
        GlobalMemory memory = systemInfo.getHardware().getMemory();
        //总内存
        long totalByte = memory.getTotal();
        //剩余
        long acaliableByte = memory.getAvailable();
        String res = "\n";
        res += "----------------主机内存信息-----------" + "\n";
        res += "总内存 = " + formatByte(totalByte) + "\n";
        res += "使用" + formatByte(totalByte - acaliableByte) + "\n";
        res += "剩余内存 = " + formatByte(acaliableByte) + "\n";
        res += "使用率：" + new DecimalFormat("#.##%").format((totalByte - acaliableByte) * 1.0 / totalByte) + "\n";
        res += "--------------------------------------" + "\n";
        if (showInStdout) {
            System.out.println(res);
        }
        return res;
    }

    public static void setSysInfo() {
        System.out.println("----------------操作系统信息----------------");
        Properties props = System.getProperties();
        //系统名称
        String osName = props.getProperty("os.name");
        //架构名称
        String osArch = props.getProperty("os.arch");
        System.out.println("操作系统名 = " + osName);
        System.out.println("系统架构 = " + osArch);
    }

    public static void setJvmInfo() {
        System.out.println("----------------jvm信息----------------");
        Properties props = System.getProperties();
        Runtime runtime = Runtime.getRuntime();
        //jvm总内存
        long jvmTotalMemoryByte = runtime.totalMemory();
        //jvm最大可申请
        long jvmMaxMoryByte = runtime.maxMemory();
        //空闲空间
        long freeMemoryByte = runtime.freeMemory();
        //jdk版本
        String jdkVersion = props.getProperty("java.version");
        //jdk路径
        String jdkHome = props.getProperty("java.home");
        System.out.println("jvm内存总量 = " + formatByte(jvmTotalMemoryByte));
        System.out.println("jvm已使用内存 = " + formatByte(jvmTotalMemoryByte - freeMemoryByte));
        System.out.println("jvm剩余内存 = " + formatByte(freeMemoryByte));
        System.out.println("jvm内存使用率 = " + new DecimalFormat("#.##%")
                .format((jvmTotalMemoryByte - freeMemoryByte) * 1.0 / jvmTotalMemoryByte));
        System.out.println("java版本 = " + jdkVersion);
        //System.out.println("jdkHome = " + jdkHome);
    }

    public static void getThread() {
        System.out.println("----------------线程信息----------------");
        ThreadGroup currentGroup = Thread.currentThread().getThreadGroup();

        while (currentGroup.getParent() != null) {
            // 返回此线程组的父线程组
            currentGroup = currentGroup.getParent();
        }
        //此线程组中活动线程的估计数
        int noThreads = currentGroup.activeCount();

        Thread[] lstThreads = new Thread[noThreads];
        //把对此线程组中的所有活动子组的引用复制到指定数组中。
        currentGroup.enumerate(lstThreads);
        for (Thread thread : lstThreads) {
            System.out.println(
                    "线程数量：" + noThreads + " 线程id：" + thread.getId() + " 线程名称：" + thread.getName() + " 线程状态：" + thread
                            .getState());
        }
    }

    public static String formatByte(long byteNumber) {
        //换算单位
        double FORMAT = 1024.0;
        double kbNumber = byteNumber / FORMAT;
        if (kbNumber < FORMAT) {
            return new DecimalFormat("#.##KB").format(kbNumber);
        }
        double mbNumber = kbNumber / FORMAT;
        if (mbNumber < FORMAT) {
            return new DecimalFormat("#.##MB").format(mbNumber);
        }
        double gbNumber = mbNumber / FORMAT;
        if (gbNumber < FORMAT) {
            return new DecimalFormat("#.##GB").format(gbNumber);
        }
        double tbNumber = gbNumber / FORMAT;
        return new DecimalFormat("#.##TB").format(tbNumber);
    }
}