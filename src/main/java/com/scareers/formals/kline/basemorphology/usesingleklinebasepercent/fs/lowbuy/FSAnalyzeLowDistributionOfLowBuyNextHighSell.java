package com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.fs.lowbuy;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.mail.MailUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.LowBuyNextHighSellDistributionAnalyze;
import com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.SettingsOfSingleKlineBasePercent;
import com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.keysfunc.KeyFuncOfSingleKlineBasePercent;
import com.scareers.pandasdummy.DataFrameSelf;
import com.scareers.settings.SettingsCommon;
import com.scareers.sqlapi.TushareApi;
import com.scareers.utils.CommonUtils;
import com.scareers.utils.SqlUtil;
import com.scareers.utils.Tqdm;
import joinery.DataFrame;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.fs.lowbuy.SettingsOfLowBuyFS.*;
import static com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.keysfunc.KeyFuncOfSingleKlineBasePercent.*;
import static com.scareers.sqlapi.TushareApi.*;
import static com.scareers.sqlapi.TushareFSApi.getFs1mStockPriceOneDayAsDfFromTushare;
import static com.scareers.utils.CommonUtils.*;
import static com.scareers.utils.CommonUtils.minOfListDouble;
import static com.scareers.utils.FSUtil.fsTimeStrParseToTickDouble;
import static com.scareers.utils.HardwareUtils.reportCpuMemoryDisk;

/**
 * description: 针对 next0b1s/1b2s等, 对 nextLow 在买入当日的 最低点出现的时间, 0-240; 分布分析.  -- 出现时间分布
 * -- 时间
 * 0 代表 09:30;  240代表 15:00 , 对应tushare分时数据, 每日 241个数据.
 * -- 分区间
 * 例如将241分钟, 中间 240间隔, 按刻分, 则分为8个区间, 规定 9:30 算作第一个区间内 30-45 算作第一个区间; 46-00 第二个
 *
 * @author: admin
 * @date: 2021/11/14  0014-4:48
 */
public class FSAnalyzeLowDistributionOfLowBuyNextHighSell {
    public static void main(String[] args) throws Exception {
        List<String> stocks = TushareApi.getStockListFromTushareExNotMain();
        stocks = stocks.subList(0, Math.min(stockAmountsBeCalcFS, stocks.size()));
        DataFrame<String> stockWithBoard = TushareApi.getStockListWithBoardFromTushare();
        List<List<String>> dateRanges = SettingsOfLowBuyFS.dateRanges;
        HashMap<String, List<List<String>>> stockWithStDateRanges = TushareApi.getStockWithStDateRanges();

        // 连接对象并不放在设置类
        Connection connSingleton = ConnectionFactory.getConnLocalKlineForms();
        // 未关闭连接,可复用
        SqlUtil.execSql(SettingsOfLowBuyFS.sqlCreateSaveTableFSDistribution,
                connSingleton, false);
        for (List<String> statDateRange : dateRanges) {
            // 测试时用最新一个日期区间即可
            Console.log("当前循环组: {}", statDateRange);
            // 不能关闭连接, 否则为 null, 引发空指针异常
            SqlUtil.execSql(
                    StrUtil.format(SettingsOfLowBuyFS.sqlDeleteExistDateRangeFS,
                            StrUtil.format("[\"{}\",\"{}\"]", statDateRange.get(0), statDateRange.get(1))),
                    connSingleton, false);
            // 主逻辑.
            // 主程序分析计算的几个参数用不到, 删除即可
            // 主程序使用 windowUsePeriodsCoreArg=7/8/9/10,
            // FS分析为了更加直观, 修改为 keyInt设定. 0代表next0, 即明日, 对应了主程序中的 7
            fsLowBuyDistributionDetailAnalyze(stocks, stockWithStDateRanges, stockWithBoard, statDateRange,
                    saveTablenameLowBuyFSRow, keyInts.get(0));

            String hardwareInfo = reportCpuMemoryDisk(true);
            MailUtil.send(SettingsCommon.receivers, StrUtil.format("LowBuy部分完成: {}", statDateRange),
                    StrUtil.format("LowBuy部分完成, 硬件信息:\n", hardwareInfo), false,
                    null);
            log.info("current time");
        }
    }

    // 核心逻辑: next0 low 分时分布详细分析
    private static void fsLowBuyDistributionDetailAnalyze(List<String> stocks,
                                                          HashMap<String, List<List<String>>> stockWithStDateRanges,
                                                          DataFrame<String> stockWithBoard, List<String> statDateRange,
                                                          String saveTablenameLowBuyFS, int keyInt)
            throws SQLException, ExecutionException, InterruptedException {

        Console.log("构建结果字典");
        // 形态集合id__计算项: 值列表.
        ConcurrentHashMap<String, List<Double>> results = new ConcurrentHashMap<>(8);
        ThreadPoolExecutor poolOfParse = new ThreadPoolExecutor(processAmountParse,
                processAmountParse * 2, 10000, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
        Connection connOfParse = ConnectionFactory.getConnLocalTushare();
        AtomicInteger parseProcess = new AtomicInteger(0);
        ArrayList<Future<ConcurrentHashMap<String, List<Double>>>> futuresOfParse = new ArrayList<>();
        for (String stock : stocks) {
            // 全线程使用1个conn
            Future<ConcurrentHashMap<String, List<Double>>> f = poolOfParse
                    .submit(new LowBuyParseTask(stock, stockWithBoard,
                            statDateRange, stockWithStDateRanges, connOfParse, keyInt));
            futuresOfParse.add(f);
        }
        List<Integer> indexesOfParse = CommonUtils.range(futuresOfParse.size());
        for (Integer i : Tqdm.tqdm(indexesOfParse, StrUtil.format("{} process: ", statDateRange))) {
            // 串行不再需要使用 CountDownLatch
            Future<ConcurrentHashMap<String, List<Double>>> f = futuresOfParse.get(i);
            // @noti: 结果的 key为:  形态集合id__Low/2/High/2_ 5项基本数据
            ConcurrentHashMap<String, List<Double>> resultTemp = f.get();
            for (String key : resultTemp.keySet()) {
                results.putIfAbsent(key, new ArrayList<>());
                results.get(key).addAll(resultTemp.get(key));
            }
            resultTemp.clear();
            if (parseProcess.incrementAndGet() % gcControlEpochParse == 0) {
                System.gc();
                if (SettingsOfSingleKlineBasePercent.showMemoryUsage) {
                    showMemoryUsageMB();
                }
            }
        }
        poolOfParse.shutdown(); // 关闭线程池
        System.out.println();
        Console.log("results size: {}", results.size());
        System.gc();

        // --------------------------------------------------------- 保存
        Console.log("构建结果字典完成");
        Console.log("开始计算并保存");
        ArrayList<String> forNameRaws = new ArrayList<>(results.keySet());
        forNameRaws.sort(Comparator.naturalOrder()); // 排序, 自然顺序
        ThreadPoolExecutor poolOfCalc = new ThreadPoolExecutor(processAmountSave,
                processAmountSave * 2, 10000, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
        int totalEpochAmounts = (int) Math
                .ceil((double) results.size() / perEpochTaskAmounts);
        List<Integer> epochs = CommonUtils.range(totalEpochAmounts);
        Connection connOfSave = ConnectionFactory.getConnLocalKlineForms();
        CountDownLatch latchOfCalcForEpoch = new CountDownLatch(totalEpochAmounts);
        ArrayList<Future<List<String>>> futuresOfSave = new ArrayList<>();
        // 批量插入不伤ssd. 单条插入很伤ssd
        for (Integer currentEpoch : Tqdm
                .tqdm(epochs, StrUtil.format("{} process: ", statDateRange))) {
            int startIndex = currentEpoch * perEpochTaskAmounts;
            int endIndex = (currentEpoch + 1) * perEpochTaskAmounts;
            List<String> formNamesCurrentEpoch = forNameRaws
                    .subList(startIndex, Math.min(endIndex, forNameRaws.size()));

            Future<List<String>> f = poolOfCalc
                    .submit(new CalcStatResultAndSaveTaskOfFSLowBuyHighSell(latchOfCalcForEpoch,
                            connOfSave, formNamesCurrentEpoch,
                            stocks.size(), statDateRange, results, smallLargeThresholdOfValuePercent,
                            effectiveValueRangeOfValuePercent, binsOfValuePercent, smallLargeThresholdOfAmountPercent,
                            effectiveValueRangeOfAmountPercent, binsOfAmountPercent,
                            saveTablenameLowBuyFS));
            futuresOfSave.add(f);
        }
        AtomicInteger saveProcess = new AtomicInteger(0);
        for (Integer i : Tqdm
                .tqdm(epochs, StrUtil.format("{} process: ", statDateRange))) {
            Future<List<String>> f = futuresOfSave.get(i);
            List<String> finishedFormNames = f.get();
            for (String formName0 : finishedFormNames) {
                // 删除key, 节省空间
                results.remove(formName0);
            }
            //            if (parseProcess.incrementAndGet() % SettingsOfSingleKlineBasePercent.gcControlEpoch == 0) {
            //                System.gc();
            //                if (SettingsOfSingleKlineBasePercent.showMemoryUsage) {
            //                    showMemoryUsageMB();
            //                }
            //            }
        }

        Console.log("计算并保存完成!");
        latchOfCalcForEpoch.await();
        //        connOfSave.close(); // 不可关闭, 因底层默认会重新获取到null连接
        poolOfCalc.shutdown();
        // 本轮执行完毕
    }


    public static class LowBuyParseTask implements Callable<ConcurrentHashMap<String,
            List<Double>>> {
        // @key: 从数据库获取的 2000+形态集合.的字典.  形态集合id: 已解析json的字符串列表.
        public static ConcurrentHashMap<Long, List<String>> formSetsMapFromDB = null;

        String stock;
        DataFrame<String> stockWithBoard;
        List<String> statDateRange;
        HashMap<String, List<List<String>>> stockWithStDateRanges;
        Connection conn;
        int keyInt;

        public LowBuyParseTask(String stock, DataFrame<String> stockWithBoard, List<String> statDateRange,
                               HashMap<String, List<List<String>>> stockWithStDateRanges, Connection connOfParse,
                               int keyInt) {
            this.stock = stock;
            this.stockWithBoard = stockWithBoard;
            this.statDateRange = statDateRange;
            this.stockWithStDateRanges = stockWithStDateRanges;
            this.conn = connOfParse;
            this.keyInt = keyInt;
        }

        @Override
        public ConcurrentHashMap<String, List<Double>> call() {
            // 实际逻辑显然对应了python 的parse_single_stock() 函数
            // 使得不会返回null. 至少会返回空的字典
            ConcurrentHashMap<String, List<Double>> resultSingle = new ConcurrentHashMap<>(2 ^ 5);
            try {
                // 开始主要逻辑
                // 添加结果到 线程安全的 总结果集
                List<String> statDateRangeFull = CommonUtils.changeStatRangeForFull(statDateRange);
                // 单个线程用一个 conn 对象, 用完close(), 否则线程池容量不够
                // 连接未关闭, 传递了 conn. 若不传递, 则临时从池子获取.
                DataFrame<Object> dfRaw = getStockPriceByTscodeAndDaterangeAsDfFromTushare(stock, "nofq",
                        SettingsOfSingleKlineBasePercent.fieldsOfDfRaw,
                        statDateRangeFull, conn);
                dfRaw = dfRaw.dropna();
                // 新知识: java不定参数等价于 数组.而非List
                dfRaw.convert(fieldsOfDfRawClass);
                HashSet<String> adjDates = getAdjdatesByTscodeFromTushare(stock, conn);
                resultSingle = LowBuyAnalyzerOfPerStock(dfRaw, adjDates, stock, stockWithBoard,
                        stockWithStDateRanges, statDateRange,
                        conn, keyInt); // 注意: dfRaw依据fulldates获取, 而这里要传递统计区间日期
                return resultSingle;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                return resultSingle;
            }
        }

        /**
         * 对应python 的 BaseFormAndOpenConditionAnalyzer 函数
         * 因为conn一直没有关闭, 这里传递conn对象. 直到单只股票分析完, 再关闭
         *
         * @param dfRaw
         * @param adjDates
         * @param stock
         * @param stockWithBoard
         * @param stockWithStDateRanges
         * @param statDateRange:        已经是 statDateRangeFull.
         * @return
         */
        public static ConcurrentHashMap<String, List<Double>> LowBuyAnalyzerOfPerStock(
                DataFrame<Object> dfRaw, HashSet<String> adjDates,
                String stock,
                DataFrame<String> stockWithBoard,
                HashMap<String, List<List<String>>> stockWithStDateRanges,
                List<String> statDateRange,
                Connection conn, int keyInt) {
            //@key: 此前一直传递keyInt, 而非主程序的 windowUsePeriodsCoreArg, 这里构建
            //@key: 决定 LowBuy 和 HighSell 一起计算.  因此这里 windowUsePeriodsCoreArg, 应当等价于 高卖那一天..
            int windowUsePeriodsCoreArg = keyInts.get(1) + 7;
            ConcurrentHashMap<String, List<Double>> resultTemp = new ConcurrentHashMap<>(2);
            if (dfRaw.length() < windowUsePeriodsCoreArg) {
                return resultTemp;
            }
            for (int i = 4; i < dfRaw.length() - windowUsePeriodsCoreArg; i++) {
                try {
                    DataFrame<Object> dfWindowRaw = dfRaw.slice(i - 4,
                            i - 4 + windowUsePeriodsCoreArg);
                    String todayTemp = (String) dfWindowRaw
                            .get(5, fieldsOfDfRaw.indexOf("trade_date"));
                    if (todayTemp.compareTo(statDateRange.get(0)) < 0 || todayTemp
                            .compareTo(statDateRange.get(1)) >= 0) {
                        // 前包后不包
                        continue;
                    }
                    DataFrame<Object> dfWindow;
                    if (hasIntersectionBetweenAdjDatesAndDfWindow(adjDates, dfWindowRaw)) {
                        List<Object> colTradeDates = dfWindowRaw.col(fieldsOfDfRaw.indexOf("trade_date"));
                        List<String> dateRangeEqual = Arrays.asList((String) colTradeDates.get(0),
                                (String) colTradeDates.get(colTradeDates.size() - 1));
                        // @bugfix: 当获取等价后复权时,日期应当包含尾; 而原sqlApi实现包头不包尾.
                        // 不读取后复权sql时,不会影响,因此出现这种情况;  而python的sql是现写的, 没出现bug. :  < --> <=
                        // @bugfix: 已重构, 参数 excludeEndDate, 可以选择 设定. 此处设置 false, 则包尾
                        dfWindow = TushareApi
                                .getStockPriceByTscodeAndDaterangeAsDfFromTushare0(stock, "hfq", fieldsOfDfRaw,
                                        dateRangeEqual, conn, false);
                        dfWindow.convert(fieldsOfDfRawClass);
                    } else {
                        dfWindow = dfWindowRaw;
                    }
                    dfWindow = dfWindow.dropna();
                    //@bugfix: 注意是 length, 而非size()
                    if (dfWindow.length() != windowUsePeriodsCoreArg) {
                        continue; // 后复权数据,有所缺失
                    }

                    List<Object> pre5dayKlineRow = dfWindow.row(0);
                    List<Object> yesterdayKlineRow = dfWindow.row(4);
                    List<Object> todayKlineRow = dfWindow.row(5);
                    List<String> concreteTodayFormStrs = parseConditionsAsStrs(stock, dfWindow, pre5dayKlineRow,
                            yesterdayKlineRow, todayKlineRow, stockWithStDateRanges, stockWithBoard);

//                    // 四个结果值
//                    Double todayClose = getPriceOfSingleKline(todayKlineRow, "close");
//                    Double singleResultAccordingNextOpen =
//                            getPriceOfSingleKline(resultAccordingKlineRow, "open") / todayClose - 1;
//                    Double singleResultAccordingNextClose =
//                            getPriceOfSingleKline(resultAccordingKlineRow, "close") / todayClose - 1;
//                    Double singleResultAccordingNextHigh =
//                            getPriceOfSingleKline(resultAccordingKlineRow, "high") / todayClose - 1;
//                    Double singleResultAccordingNextLow =
//                            getPriceOfSingleKline(resultAccordingKlineRow, "low") / todayClose - 1;

                    // 7条件判定完成 *******
                    if (concreteTodayFormStrs.contains("-")) {
                        continue;
                    }
                    // *********
                    // 该window 128种形态
                    // @key: 以上均为, 与主程序形态判定逻辑一致, 从这里开始, 将判定是否符合某种形态集合!
                    // lb1: 数据库读取形态集合,单形态集合为: List<String>, 遍历形态集合 id, 看是否符合. 再计算15种结果
                    List<String> allForms = getAllFormNamesByConcreteFormStrs(concreteTodayFormStrs);
                    if (formSetsMapFromDB == null) {
                        // 初始化形态集合 必要;
                        formSetsMapFromDB = parseFromsSetsFromDb(conn);
                    }
                    // lowbuy2: 计算属于那些形态集合? 给出 id列表, 如果id列表空,显然不需要浪费时间计算 15个结果值.
                    List<Long> belongToFormsetIds = calcBelongToFormSets(formSetsMapFromDB, allForms);
                    if (belongToFormsetIds.size() == 0) {
                        continue; // 如果id列表空,显然不需要浪费时间计算 15个结果值.
                    }

                    // lowbuy3: 计算15项算法项*2, 以保存;  主程序中是简单的 ochl计算, 是否简单, 这里却需要访问分时图计算
                    // 注意window 原数据列: "trade_date", "open", "close", "high", "low", "vol".
                    List<Object> keyInt0LowBuyKlineRow = dfWindow.row(6 + keyInt); // 这里 keyInt==keyInts.get(0),懒得改
                    List<Object> keyInt1HighSellKlineRow = dfWindow.row(6 + keyInts.get(1));
                    // @special: 因为分时图未复权的考虑. 对于 todayClose, 我们从 不复权读取close, 然后读取 当两天复权因子比例,
                    // 折算到一个 当天等价的 前复权价格.   这个价格必须这样计算.
                    // 因此传递 today, 计算, 假设明日复权, 那么 临时前复权, 今日的 close应该 复权成多少??.
                    // 分时图中的 基准价格应该是当日前复权close;  而成交量使用成交额, 不存在此问题. 参见 TushareApi.qfqStockSpecialDay
                    // @specialend
                    String today = todayKlineRow.get(0).toString(); // 今日价格
                    String lowBuyDate = keyInt0LowBuyKlineRow.get(0).toString(); // 买入日期.
                    // 未复权时等价于今日收盘价
                    Double stdAmount = getPriceOfSingleKline(todayKlineRow, "amount"); // 今日作为基准成交额
                    Double stdCloseOfLowBuy = CloseOfQfqStockSpecialDay(stock, today, lowBuyDate, conn); //
                    // 临时前复权作为基准close.
                    // 对于时刻, 也使用 Double 0.0,1.0,2.0表示.
                    // 因此15种算法结果: low0/2/3 * percent,出现时刻,左支配数量,右支配数量,连续下跌成交量
                    // 使用 Map 保存15种结果, 不返回null, 最多返回 空Map
                    // todo: 2021/11/19, 仅仅实现 Low和Low1, 未实现Low2, 因此仅10种结果
                    HashMap<String, Double> resultOf10AlgorithmLow = calc10ItemValusOfLowBuy(stdAmount,
                            stdCloseOfLowBuy,
                            lowBuyDate, connOfFS, stock);

                    String highSellDate = keyInt1HighSellKlineRow.get(0).toString(); // 卖出日期..
                    Double stdCloseOfHighSell = CloseOfQfqStockSpecialDay(stock, today, highSellDate, conn); //
                    // 临时前复权作为基准close.
                    // todo: highSell 对应的10种结果

                    // 开始填充结果:  @noti: 结果的 key为:  形态集合id__Low/2/High/2_ 5项基本数据
                    for (Long setId : belongToFormsetIds) {
                        String prefix = setId.toString() + "__"; // 临时前缀.
                        for (String lowKeys : resultOf10AlgorithmLow.keySet()) {
                            String keyFull = StrUtil.format("{}{}", prefix, lowKeys);
                            resultTemp.putIfAbsent(keyFull, new ArrayList<>());
                            resultTemp.get(keyFull).add(resultOf10AlgorithmLow.get(lowKeys));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // 打印此时的 dfwindow 前3行
                    Console.log("发生了异常, dfWindow: ");
                    Console.log(dfRaw.slice(i, i + 3));
                }
            }
            return resultTemp;
        }

        /**
         * LowBuy 分析核心方法. 计算15种结果值.
         * 3*5
         * Low /Low2/Low3 * happen_tick / value_percent / dominate_left / dominate_right / continuous_fall_vol_percent
         *
         * @param stdAmount
         * @param stdCloseOfLowBuy
         * @param lowBuyDate
         * @return
         */
        /**
         * next0/1/2/3等,        low 在分时图中 出现的分析, 分析项目为以下几个.
         * --- 分析项目
         * 1.Low最低价出现时间(0-240表示).
         * 2.Low的左领域值 , 即low往左边计算, 左边相邻有多少个区域, 其价格比low高, 但是很相近, 例如low=-0.05, 相邻阈值可以 -0.045
         * 3.Low的右领域值 , 右边相近的时间数量    dominateRight
         * 4.Low2  次低价, 定义为排除掉 Low 以及其左右支配后, 剩余下面选择.
         * 5.Low2 的左右支配
         * 6.Low3 第三低价 及 左右支配
         * 7.Low /Low2/Low3 的具体数值.  // 总计12项目
         * --- 说明
         * 1.本脚本是给定形态集合,id, 分析之下 next Low 的相关分布.  本身行数不会多大. 列数多.
         * --- 保存的数据
         * 2.对单个形态集合,  基本分析以上 12项目:
         * 1.Low/2/3出现时间: 结果为 列表json字符串,   241个元素, 代表了在当分钟出现的次数!!!
         * 后续如果分区间分析, 也方便转换
         * 2.3个价格(百分比), 依旧采用 [] 区间分析方法.
         * 3.左支配时间:  因为最大可能 241个支配, 统一性, 同 出现时间, 用 241个数组
         * 4.右支配时间: 同理
         * --- 新增:成交量分析:
         * 当到达Low/2/3时,   连续下跌到达Low, 整个多分钟的 总成交量, 占(昨日成交量)的比例.
         * noti: 需要连续下跌, 才计算进成交量, == 的不计算. 显然至少能计算1分钟的成交量
         * 分析项名称为  continuous_fall_vol_percent   连续下跌成交量占昨日百分比
         * <p>
         * *************
         * 15项列表: 3*5
         * --> Low /2/3  == 3
         * --> 5项基本: happen_tick / value_percent / dominate_left / dominate_right / continuous_fall_vol_percent
         * --> 即        发生时间    / 具体的涨跌幅  / 左支配         /右支配           / 连续下跌到最低多分钟整个成交量占昨日比
         * <p>
         * *************
         *
         * @key: 保存 12项基本数据, 每种数据都是一个列表的 json字符串.
         * @key: 要对 12项基本 数组进行 分布分析,  因此考虑,  用 字段 "analyze_item_type" 列, 保存单条记录是什么"类型", 总计12种
         * @key: 数据表改造后, 单行数据, 表示了 某一项数据(一个列表)的分析结果.
         * analyze_item_type 表示分析的什么数据
         * detail_list  保存 那个json列表!   其余列, 表示对 detail_list 的统计分析
         */
        public static HashMap<String, Double> calc10ItemValusOfLowBuy(Double stdAmount, Double stdCloseOfLowBuy,
                                                                      String lowBuyDate, Connection conn,
                                                                      String stock) throws Exception {
            // 5项基本: happen_tick / value_percent / dominate_left / dominate_right / continuousFallVolPercent
            // key是 Low__5项基本  Low2... Low3...
            HashMap<String, Double> res = new HashMap<>();
            DataFrame<Object> dfFSLowBuyDay = getFs1mStockPriceOneDayAsDfFromTushare(conn, stock, lowBuyDate,
                    fsSpecialUseFields); // 字段列表: Arrays.asList("trade_time", "close", "amount");
            if (dfFSLowBuyDay == null || dfFSLowBuyDay.length() == 0) {
                return res; // 无分时数据, 则没有计算结果
            }
            dfFSLowBuyDay.convert(String.class, Double.class, Double.class);
            // 1.将 trade_time, 转换为 tickDouble.
            List<Object> tradeTimeCol = dfFSLowBuyDay.col(0);
            List<Double> tickDoubleCol = new ArrayList<>();
            for (Object o : tradeTimeCol) {
                String tick = o.toString();
                tickDoubleCol.add(fsTimeStrParseToTickDouble(tick.substring(11, 16))); // 分钟tick转换为整数型double
            }
            // 字段列表: Arrays.asList("trade_time", "close", "amount");  + "tick_double
            dfFSLowBuyDay.add("tick_double", tickDoubleCol);

            // 1.low, 相关
            List<Double> closeCol = DataFrameSelf.getColAsDoubleList(dfFSLowBuyDay, "close");
            List<Double> amountCol = DataFrameSelf.getColAsDoubleList(dfFSLowBuyDay, "amount");
            Double low = minOfListDouble(closeCol);
            // 1-1. happen_tick
            // 只不过最后再转换为Double, 目前int,方便计算, 且 tick相关均使用 tick_double列进行取值
            int happernTickOfLow = closeCol.indexOf(low);
            // 1-2. value_percent
            Double valuePercentOfLow = low / stdCloseOfLowBuy - 1;
            // 1-3/4. dominate_left / dominate_right
            Double dominateThreshold = ((Math
                    .abs(valuePercentOfLow) * dominateRateKeyArg + valuePercentOfLow) + 1) * stdCloseOfLowBuy; //
//            Console.log(dominateThreshold);
//            Console.log(closeCol);
//            Console.log(low);
            // 确定比low大
            int dominateLeft = 0; // 最小0, 最大 happernTickOfLow - 1
            if (!(happernTickOfLow == 0)) {
                for (int i = happernTickOfLow - 1; i >= 0; i--) {
                    if (closeCol.get(i) > dominateThreshold) {
                        break;
                    }
                    dominateLeft++;
                }
            }
            int dominateRight = 0;
            if (!(happernTickOfLow == closeCol.size() - 1)) {
                for (int i = happernTickOfLow + 1; i < closeCol.size(); i++) {
                    if (closeCol.get(i) > dominateThreshold) {
                        break;
                    }
                    dominateRight++;
                }
            }
            // 1.5. 连续成交额占比, 包含了 low那一分钟 . continuousFallVolPercent
            ArrayList<Integer> continuousFallIndexes = new ArrayList<>();
            continuousFallIndexes.add(happernTickOfLow); // low那一分钟加入
            if (!(happernTickOfLow == 0)) { // 至少1, 才有可能往前找
                for (int i = happernTickOfLow; i > 0; i--) { // 注意>0
                    Double closeAfter = closeCol.get(i);
                    Double closeBefore = closeCol.get(i - 1);
                    if (closeAfter <= closeBefore) {
                        continuousFallIndexes.add(i - 1); // 将前1分钟成交额加入索引列表.
                    } else {
                        break; // 一旦不符合应当立即跳出
                    }
                }
            }
            Double amountTotal = 0.0;
            for (Integer i : continuousFallIndexes) {
                amountTotal += amountCol.get(i);
            }
            Double continuousFallVolPercent = amountTotal / stdAmount;

            // Low相关5项完成.
            res.put("Low__happen_tick", tickDoubleCol.get(happernTickOfLow));
            res.put("Low__value_percent", valuePercentOfLow);
            res.put("Low__dominate_left", (double) dominateLeft);
            res.put("Low__dominate_right", (double) dominateRight);
            res.put("Low__continuous_fall_vol_percent", continuousFallVolPercent);


            // 将要计算Low2相关, 此时已经将241行分为了3部分, 排除掉 Low 支配的所有时间, 两边的分为2部分
            // 第一步, 需要找到 次低点会在哪里
            // @noti: List.subList(0,0)  两参数可以相等, 此时,返回空列表
            List<Double> closeColOfLow2Fragment1 = closeCol.subList(0, happernTickOfLow - dominateLeft);
            List<Double> closeColOfLow2Fragment2 = closeCol.subList(happernTickOfLow + dominateRight + 1,
                    closeCol.size()); // 前包后不包, 实测这里需要+1
            List<Double> closeColOfLow2Actual; // Low2 应该在哪里找呢? 应当是上面两个片段的一段
            // Low2计算出现tick时, 需要对 indexOf出来的结果修正, 片段1修正值0, 片段2显然是 happernTickOfLow + dominateRight + 1
            int fixHappenTick = 0;
            List<Double> amountColActual; // 成交额片段, 与 closeColOfLow2Actual同步设置
            if (closeColOfLow2Fragment1.size() == 0) {
                if (closeColOfLow2Fragment2.size() == 0) {
                    return res; // 此时 Low已经实现全部支配, Low2, Low3 不存在, 因此直接返回
                } else {
                    closeColOfLow2Actual = closeColOfLow2Fragment2;
                    fixHappenTick = happernTickOfLow + dominateRight + 1;
                    amountColActual = amountCol.subList(happernTickOfLow + dominateRight + 1,
                            closeCol.size());
                }
            } else {
                if (closeColOfLow2Fragment2.size() == 0) {
                    closeColOfLow2Actual = closeColOfLow2Fragment1;
                    amountColActual = amountCol.subList(0, happernTickOfLow - dominateLeft);
                } else {
                    Double min1 = minOfListDouble(closeColOfLow2Fragment1);
                    Double min2 = minOfListDouble(closeColOfLow2Fragment2);
                    if (min1 <= min2) {
//                        Console.log("fragment1 selected");
                        closeColOfLow2Actual = closeColOfLow2Fragment1; // 两段最小相等, 则选择片段1
                        amountColActual = amountCol.subList(0, happernTickOfLow - dominateLeft);
                    } else {
//                        Console.log("fragment2 selected");
                        closeColOfLow2Actual = closeColOfLow2Fragment2;
                        fixHappenTick = happernTickOfLow + dominateRight + 1;
                        amountColActual = amountCol.subList(happernTickOfLow + dominateRight + 1,
                                closeCol.size());
                    }
                }
            }
//            Console.log(closeColOfLow2Actual);
            // Low2 5项目开始, 4项算法完全相同,只是close列变为了片段. 出现tick则需要+ 修正值(片段1,片段2显然不同),
            Double low2 = minOfListDouble(closeColOfLow2Actual);
            // 1-1. happen_tick , 最终结果需要 + 修正值 fixHappenTick
//            Console.log(low2);
            int happernTickOfLow2 = closeColOfLow2Actual.indexOf(low2);
//            Console.log(happernTickOfLow2);
            // 1-2. value_percent
            Double valuePercentOfLow2 = low2 / stdCloseOfLowBuy - 1;
            // 1-3/4. dominate_left / dominate_right
            Double dominateThreshold2 = ((Math
                    .abs(valuePercentOfLow2) * dominateRateKeyArg + valuePercentOfLow2) + 1) * stdCloseOfLowBuy; //
//            Console.log(dominateThreshold2);
            int dominateLeft2 = 0; // 最小0, 最大 happernTickOfLow - 1
            if (!(happernTickOfLow2 == 0)) {
                for (int i = happernTickOfLow2 - 1; i >= 0; i--) {
                    if (closeColOfLow2Actual.get(i) > dominateThreshold2) {
                        break;
                    }
                    dominateLeft2++;
                }
            }
//            Console.log(dominateLeft2);
            int dominateRight2 = 0;
            if (!(happernTickOfLow2 == closeColOfLow2Actual.size() - 1)) {
                for (int i = happernTickOfLow2 + 1; i < closeColOfLow2Actual.size(); i++) {
                    if (closeColOfLow2Actual.get(i) > dominateThreshold2) {
                        break;
                    }
                    dominateRight2++;
                }
            }
//            Console.log(dominateRight2);
            // 1.5. 连续成交额占比, 包含了 low那一分钟 . continuousFallVolPercent
            ArrayList<Integer> continuousFallIndexes2 = new ArrayList<>();
            continuousFallIndexes2.add(happernTickOfLow2); // low那一分钟加入
//            Console.log(happernTickOfLow2);
            if (!(happernTickOfLow2 == 0)) { // 至少1, 才有可能往前找
                for (int i = happernTickOfLow2; i > 0; i--) { // 注意>0
                    Double closeAfter = closeColOfLow2Actual.get(i);
                    Double closeBefore = closeColOfLow2Actual.get(i - 1);
                    if (closeAfter <= closeBefore) {
                        continuousFallIndexes2.add(i - 1); // 将前1分钟成交额加入索引列表.
                    } else {
                        break; // 一旦不符合应当立即跳出
                    }
                }
            }
//            Console.log(continuousFallIndexes2);
            Double amountTotal2 = 0.0;
            for (Integer i : continuousFallIndexes2) {
//                Console.log(amountTotal2);
                amountTotal2 += amountColActual.get(i); // 注意.
            }
//            Console.log(amountTotal2);
            Double continuousFallVolPercent2 = amountTotal2 / stdAmount;

            // Low相关5项完成.
            res.put("Low2__happen_tick", tickDoubleCol.get(happernTickOfLow2 + fixHappenTick));
            res.put("Low2__value_percent", valuePercentOfLow2);
            res.put("Low2__dominate_left", (double) dominateLeft2);
            res.put("Low2__dominate_right", (double) dominateRight2);
            res.put("Low2__continuous_fall_vol_percent", continuousFallVolPercent2);

//            Console.log(res);
            return res;
        }

        private static List<Long> calcBelongToFormSets(ConcurrentHashMap<Long, List<String>> formSetsMapFromDB,
                                                       List<String> allForms) {
            List<Long> belongToFormsetIds = new ArrayList<>();
            for (Long key : formSetsMapFromDB.keySet()) {
                List<String> value = formSetsMapFromDB.get(key);
                // 判定交集
                HashSet<String> inter = intersectionOfList(allForms, value);
                if (inter.size() > 0) {
                    belongToFormsetIds.add(key);
                }
            }
            return belongToFormsetIds;
        }

        public static ConcurrentHashMap<Long, List<String>> parseFromsSetsFromDb(Connection conn)
                throws SQLException { // 直接读取设定,而非用keyInt
            String tableName = StrUtil.format(LowBuyNextHighSellDistributionAnalyze.tablenameSaveAnalyze,
                    keyInts.get(0),
                    keyInts.get(1));
            DataFrame<Object> dfFormSets = DataFrame.readSql(conn, StrUtil.format("select id,form_name from {};",
                    tableName));
            ConcurrentHashMap<Long, List<String>> res = new ConcurrentHashMap<>();

            for (int i = 0; i < dfFormSets.length(); i++) {
                List<Object> row = dfFormSets.row(i);
                Long key = Long.valueOf(row.get(0).toString());
                List<String> value = JSONUtil.parseArray(row.get(1).toString()).toList(String.class);// 转换为字符串
                res.put(key, value);
            }
            Console.log(StrUtil.format("一次解析形态集合完成, 数据表: {}", tableName));
            //@noti: res.get(1L)  才行, 注意时 long, 而非int
            return res;
        }

        // 同主程序
        public static List<String> parseConditionsAsStrs(String stock, DataFrame<Object> dfWindow,
                                                         List<Object> pre5dayKlineRow,
                                                         List<Object> yesterdayKlineRow,
                                                         List<Object> todayKlineRow,
                                                         HashMap<String, List<List<String>>> stockWithStDateRanges,
                                                         DataFrame<String> stockWithBoard) throws SQLException {
            return KeyFuncOfSingleKlineBasePercent.parseConditionsAsStrs(stock, dfWindow, pre5dayKlineRow,
                    yesterdayKlineRow, todayKlineRow,
                    stockWithStDateRanges, stockWithBoard, false, false);
        }


    }

    public static Log log = LogFactory.get();

    public static class CalcStatResultAndSaveTaskOfFSLowBuyHighSell implements Callable<List<String>> {
        @Override
        public List<String> call() throws Exception {
            return null;
        }
    }

}
