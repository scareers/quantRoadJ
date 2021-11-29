package com.scareers;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.fs.lowbuy.FSAnalyzeLowDistributionOfLowBuyNextHighSell;
import com.xnx3.robot.Robot;
import org.apache.commons.validator.Var;

import java.util.*;

import static com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.fs.lowbuy.FSAnalyzeLowDistributionOfLowBuyNextHighSell.LowBuyParseTask.calc5ItemValusOfHighSell;
import static com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.fs.lowbuy.FSAnalyzeLowDistributionOfLowBuyNextHighSell.LowBuyParseTask.calc5ItemValusOfLowBuy;
import static com.scareers.utils.CommonUtils.*;
import static com.scareers.utils.FSUtil.fsTickDoubleParseToTimeStr;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) throws Exception {
//        DataFrame<Object> df_ = new DataFrame<>();
//        df_.add("a", ListUtil.of("abc", "xyz"));
//        df_.add("b", ListUtil.of("2.0", "1.0"));
//
//        Console.log(df_);
//        Console.log(df_.types());
//        List<Object> col1 = df_.col(0);
//        Console.log(col1.get(0) instanceof String);
//        HashMap<String, Double> temp = calc10ItemValusOfLowBuyDeprecated(3346000000.0, 19.43, "20210831",
//                ConnectionFactory.getConnLocalTushare1M(), "000002.SZ");
//        Console.log(JSONUtil.toJsonPrettyStr(temp));

//        HashMap<String, Double> temp2 = calc5ItemValusOfHighSell(3346000000.0, 19.43, "20210901",
//                ConnectionFactory.getConnLocalTushare1M(), "000002.SZ", 4);
//        ArrayList<String> keys = new ArrayList<>(temp2.keySet());
//        keys.sort(Comparator.naturalOrder());
//
//        for (String key : keys) {
//            if (key.endsWith("happen_tick")) {
//                Console.log(StrUtil.format("{} --> {}", key, fsTickDoubleParseToTimeStr(temp2.get(key))));
//                continue;
//            }
//            Console.log(StrUtil.format("{} --> {}", key, temp2.get(key)));
//        }
//        Console.log(RandomUtil.randomInt(10));

//        Robot robot = new Robot();
//        robot.setSourcePath(App.class);

        List<String> x = new ArrayList<>();
        for (int i = 0; i < 128; i++) {
            x.add("key___________" + i);
        }

        HashSet<String> y = new HashSet<>();
        for (int i = 0; i < 2580; i++) {
            y.add("key___________" + RandomUtil.randomInt(10000));
        }

        TimeInterval timer = DateUtil.timer();
        timer.start();
        for (int i = 0; i < 10000; i++) {
            isIntersectOfSet(x, y);
        }

        Console.log(timer.intervalRestart());
        for (int i = 0; i < 10000; i++) {
            isIntersectOfSetUseStream(x, y);
        }

        Console.log(timer.intervalRestart());

        //        List<String> x = Arrays.asList("abc", "xyz");


//
//        MailUtil.send(SettingsCommon.receivers,
//                "硬件信息: ",
//                reportCpuMemoryDisk(true),
//                false, null);
// SF1141606309289
//        System.out.println(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
//        System.gc();
//        Map<Integer, String> passedMap = new HashMap<>();
//        for (int i = 0; i < 1000000; i++) {
//            passedMap.put(i, "000000");
//        }
//        System.out.println(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
//        for (int i = 0; i < 1000000; i++) {
//            passedMap.remove(i);
//        }
//        System.gc();
//        System.out.println(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
////
//
//        DataFrame<Double> dfo = new DataFrame<>("a");
//        ArrayList<Double> row = new ArrayList<>();
//        for (int i = 0; i < 100; i++) {
//            row.add(RandomUtil.randomDouble());
//        }
//        dfo.append(row);
////        dfo.show();
//        dfo.plot(DataFrame.PlotType.LINE);

//        for (int i : Tqdm.tqdm(Arrays.asList(1, 2, 3, 4, 5, 6), "iterating")) {
//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//
//        Object x = "abc";
//        Console.log(x instanceof String);
//        Double nan = Double.NaN;
//        Console.log(Double.compare(1, Double.NaN));
//
//        Console.log(JSONUtil.toJsonStr((Object) Arrays.asList(0.25)));
//        Console.log(JSONUtil.toJsonStr(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4))));
//        Console.log(Math.pow(2.1867241478865562, 5));
//
//        DataFrame<Double> df = new DataFrame<>();
//        df.add(Arrays.asList(1.0));
//        df = df.apply(value -> {
//            return value + 1;
//        });
//        Console.log(df);
//        Console.log(df.kurt());


//        DataFrameSelf<Object> df = new DataFrameSelf<>("a", "b");
//        Connection conn = ConnectionFactory.getConnLocalKlineForms();

//        df.append(Arrays.asList(1, 2));
//        df.append(Arrays.asList(3, 4));
//        Console.log(df.mean());
//        Console.log(df.mean().getClass().getName());
//        Console.log(df.mean().get(0, 0));
//        Console.log(df.skew().get(0, 0).getClass().getName());
//        Console.log(df.skew().get(0, 0).getClass().getName());


//        System.out.println(df.col("a")); // 注意无法直接打印 df, 只能打印 行,列 List,元素Object,  等等
//        System.out.println(df.getColumnsAsString());
//        System.out.println(df.getIndexesAsString()); //
//        System.out.println(df.columns());
//        System.out.println(df.index());  // 本质是 Map 的key, 组成的 keySet属性:  Set<K> 对象
//
//        df.toSql("test", conn, "replace", "create table if not exists test(a int null,b int null)");
//        conn.close(); // 自行封装的都并没有close, 注意手动关闭; 跟python稍有不同
//
//        HttpRequest req = HttpUtil.createGet("https://www.baidu.com");
//        HttpResponse res = req.execute();
////        System.out.println(res.body());
//
//        String zodiac = DateUtil.getZodiac(Month.OCTOBER.getValue(), 22);
//        System.out.println(zodiac);
//        System.out.println(DateUtil.getChineseZodiac(1990));
//        Console.log(new Date());
//
//
//        System.out.println(System.getProperty("user.dir"));
//        System.out.println(System.getProperty("java.class.path"));
//
//        System.out.println(ResourceUtil.readUtf8Str("config/test.txt"));
//
//        ClassPathResource resource = new ClassPathResource("test.properties");
//        Properties properties = new Properties();
//        properties.load(resource.getStream());
//        Console.log("Properties: {}", properties);
//
//        String str = "abcde" +
//                "fgh" + (1 ^ 2);
//        String strSub1 = StrUtil.sub(str, 2, 3); //strSub1 -> c
//        String strSub2 = StrUtil.sub(str, 2, -3); //strSub2 -> cde
//        String strSub3 = StrUtil.sub(str, 3, 2); //strSub2 -> c
//        System.out.println(strSub1);
//
////        System.out.println(RuntimeUtil.execForStr("systeminfo"));
//
//        System.out.println(Runtime.getRuntime().availableProcessors());
//        System.out.println(Runtime.getRuntime().maxMemory());
//        System.out.println(Runtime.getRuntime().freeMemory());
//        System.out.println(Runtime.getRuntime().totalMemory());


    }


}


