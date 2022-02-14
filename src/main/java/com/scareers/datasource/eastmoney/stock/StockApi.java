package com.scareers.datasource.eastmoney.stock;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import com.scareers.annotations.CanCache;
import com.scareers.annotations.TimeoutCache;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.utils.Tqdm;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.scareers.datasource.eastmoney.EastMoneyUtil.*;
import static com.scareers.datasource.eastmoney.SettingsOfEastMoney.DEFAULT_TIMEOUT;
import static com.scareers.utils.JSONUtilS.jsonStrToDf;

/**
 * description: 东方财富股票 相关api; 可选底层http实现: hutool.http 或者 Kevin 库, 某些api仅其中之一可正常访问.
 *
 * @author: admin
 * @date: 2021/12/21/021-22:10:19
 */
public class StockApi {
    private static final Log log = LogUtil.getLogger();
    public static ConcurrentHashMap<String, String> FS_DICT = new ConcurrentHashMap<>();
    public static Map<Object, Object> EASTMONEY_QUOTE_FIELDS = new ConcurrentHashMap<>();
    public static Map<String, Object> EASTMONEY_KLINE_FIELDS = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, String> MARKET_NUMBER_DICT = new ConcurrentHashMap<>();
    public static Cache<String, DataFrame<Object>> quoteHistorySingleCache = CacheUtil.newLRUCache(1024,
            4 * 3600 * 1000); // 历史k线,分时图等
    public static Cache<String, List<Double>> stockPriceLimitCache = CacheUtil.newLRUCache(1024,
            3600 * 1000); // 个股今日涨跌停
    public static Cache<String, List<Double>> stockPreCloseAndTodayOpenCache = CacheUtil.newLRUCache(1024,
            3600 * 1000); // 个股昨收和今开盘价

    public static ThreadPoolExecutor poolExecutor;

    public static void checkPoolExecutor() {
        if (poolExecutor == null) {
            poolExecutor = new ThreadPoolExecutor(16, 32, 10000, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(), ThreadUtil.newNamedThreadFactory("klineGet-", null, true));

        }
    }


    public static void main(String[] args) throws Exception {
        TimeInterval timer = DateUtil.timer();
        timer.start();

        Console.log(getStockHandicap("000001", 2000, 1));
        // 个股今日涨跌停
        Console.log(getStockPriceLimitToday("000001", 2000, 1));

        Console.log(getRealtimeQuotes(Arrays.asList("概念板块", "行业板块", "地域板块")));
        //7 23.50 ok
        Console.log(getFSTransaction(120, "000001", 1, 1000).toString(250));
        // 分时成交:
        // 02:11 昨日df
        // 06:00
        // 08:55 得到空df,
        // 09:10
        // 09:14:x 得到空df,
        // 09:15:x 得到空df,
        // 09:24:x 得到空df,
        // 09:25:x 单行记录 09:25:07 	3508.24	2924545	1 , 表示竞价结果, 意味着无竞价过程.
        // 09:30:x 正常添加记录df
        // 09:40:x 正常df
        // 11:30:x 正常df, 但最大时间可能超过 11:30:00, 少数.
        // 13:00:x 正常df, 第一个常为 13:00:01
        // 14:54:x 正常df
        // 14:57:x 正常df, 但 14:57:05后一直不会有新数据, 持续到收盘 15:00:00 有1-2条收盘数据
        // 14:59:x 正常df, 14:57:xx后无新数据
        // 15:00:x 正常df, 最后1-2条 15:00:00 tick
        // 23:50:x 正常df, 未重置刷新,同 15:00:x
        // 周六同周五收盘后


        Console.log(getRealtimeQuotes(Arrays.asList("stock", "可转债")));
        // 实时截面数据
        // 00:00
        // 02:10 昨日df
        // 08:55 能得到正确df,但 只有动态市盈率,昨日收盘和市值字段可用. 其他数值字段用 - 填充, 将解析失败
        // 09:10
        // 09:14:x 占位df
        // 09:15:x 涨跌幅,最新价,涨跌额填充,其余 -
        // 09:24:x 涨跌幅,最新价,涨跌额填充,其余 -
        // 09:25:x 正常全填充df
        // 09:30:x 正常df
        // 09:40:x 正常df
        // 11:30:x 正常df
        // 13:00:x 正常df
        // 14:54:x 正常df
        // 14:57:x 正常df
        // 14:59:x 正常df
        // 15:00:x 正常df
        // 23:50:x 正常df

        Console.log(getFs1MToday("000001", false, 0, 2000));
        // @noti: 两大指数大约 6,7秒开始出现下一分钟分时, 个股大约1-2s之间
        // 1分钟分时图
        // 02:10 昨日df
        // 06:00
        // 08:55
        // 09:10
        // 09:14:x 得到空df,
        // 09:15:x 得到空df,
        // 09:24:x 得到空df,
        // 09:25:x 单行 9:31 的df.
        // 09:30:x 单行 9:31 的df.
        // 09:40:x 正常df, 分钟+1: 得到 41, 意味着此刻xx分钟 视为 xx+1 的分时图(但没有固定), 分时图xx分钟已固定
        // 11:30:x 正常df, 显然最大只能达到 11:30 (合理)
        // 13:00:x 于13:00:01, 立即开始出现 13:01 的数据, 规律不变.
        // 14:54:x 正常df, 算后一分
        // 14:57:x 正常df, 算后一分
        // 14:59:x 正常df, 算后一分, 此时已经出现 15:00 的分时图, 只是动态的,等待15:00:00 盖棺定论
        // 15:00:x 正常df, 不再有 15:01, 盖棺定论
        // 23:50:x 正常df, 同上截止于 15:00


        Console.log(getQuoteHistorySingle("000001", null, null, "101", "qfq", 3));
        // 日k线
        // 00:00
        // 02:10 截至昨天df
        // 08:55 截至昨天df
        // 09:14:x 截至昨天df
        // 09:15:x 截至昨天df
        // 09:24:x 截至昨天df
        // 09:25:x 已经出现含今日df
        // 09:30:x 含今日正常df
        // 09:40:x 含今日正常df
        // 11:30:x 含今日正常df
        // 13:00:x 含今日正常df
        // 14:54:x 含今日正常df
        // 14:57:x 含今日正常df
        // 14:59:x 含今日正常df
        // 15:00:x 含今日正常df
        // 21:00:x 含今日正常df
        // 23:50:x 含今日正常df


//        Console.log(getPreNTradeDateStrict("2021-01-08"));
//        Console.log(timer.intervalRestart());

        Console.log(getPreCloseAndTodayOpenOfIndex("000001", 2000));

    }

    static {
        initFSDICT();
        initEASTMONEYQUOTEFIELDS();
        initMARKETNUMBERDICT();
        initEASTMONEYKLINEFIELDS();
    }

    /**
     * # 股票、ETF、债券 K 线表头
     * {f61=换手率, f60=涨跌额, f52=开盘, f51=日期, f54=最高, f53=收盘, f56=成交量, f55=最低, f58=振幅, f57=成交额, f59=涨跌幅}
     * EASTMONEY_KLINE_FIELDS = {
     * 'f51': '日期',
     * 'f52': '开盘',
     * 'f53': '收盘',
     * 'f54': '最高',
     * 'f55': '最低',
     * 'f56': '成交量',
     * 'f57': '成交额',
     * 'f58': '振幅',
     * 'f59': '涨跌幅',
     * 'f60': '涨跌额',
     * 'f61': '换手率'
     * <p>
     * }
     * <p>
     * 'f18': '昨日收盘', ???
     */
    private static void initEASTMONEYKLINEFIELDS() {
        String rawJsonFromPython = "{\"f51\": \"\\u65e5\\u671f\", \"f52\": \"\\u5f00\\u76d8\", \"f53\": \"\\u6536\\u76d8\", \"f54\": \"\\u6700\\u9ad8\", \"f55\": \"\\u6700\\u4f4e\", \"f56\": \"\\u6210\\u4ea4\\u91cf\", \"f57\": \"\\u6210\\u4ea4\\u989d\", \"f58\": \"\\u632f\\u5e45\", \"f59\": \"\\u6da8\\u8dcc\\u5e45\", \"f60\": \"\\u6da8\\u8dcc\\u989d\", \"f61\": \"\\u6362\\u624b\\u7387\"}\n";
        Map<String, Object> temp = JSONUtil.parseObj(rawJsonFromPython);
        for (String key : temp.keySet()) {
            EASTMONEY_KLINE_FIELDS.put(key, temp.get(key).toString());
        }
        //
    }

    /**
     * 市场类型代码
     * {142=上海能源期货交易所, 0=深A, 1=沪A, 155=英股, 113=上期所, 114=大商所, 115=郑商所, 105=美股, 116=港股, 106=美股, 128=港股, 90=板块, 107=美股, 8=中金所}
     */
    private static void initMARKETNUMBERDICT() {
        String rawJsonFromPython = "{\"0\": \"\\u6df1A\", \"1\": \"\\u6caaA\", \"105\": \"\\u7f8e\\u80a1\", \"106\": " +
                "\"\\u7f8e\\u80a1\", \"107\": \"\\u7f8e\\u80a1\", \"116\": \"\\u6e2f\\u80a1\", \"128\": \"\\u6e2f\\u80a1\", \"113\": \"\\u4e0a\\u671f\\u6240\", \"114\": \"\\u5927\\u5546\\u6240\", \"115\": \"\\u90d1\\u5546\\u6240\", \"8\": \"\\u4e2d\\u91d1\\u6240\", \"142\": \"\\u4e0a\\u6d77\\u80fd\\u6e90\\u671f\\u8d27\\u4ea4\\u6613\\u6240\", \"155\": \"\\u82f1\\u80a1\", \"90\": \"\\u677f\\u5757\"}\n";
        Map<String, Object> temp = JSONUtil.parseObj(rawJsonFromPython);
        for (String key : temp.keySet()) {
            MARKET_NUMBER_DICT.put(key, temp.get(key).toString());
        }
    }


    /**
     * 常态行情表头: getRealtimeQuotes 实时截面数据api的表头
     * ['f12', 'f14', 'f3', 'f2', 'f15', 'f16', 'f17', 'f4', 'f8', 'f10', 'f9', 'f5', 'f6', 'f18', 'f20', 'f21', 'f13']
     * {f10=量比, f21=流通市值, f20=总市值, f12=代码, f14=名称, f13=市场编号, f16=最低, f15=最高, f2=最新价, f18=昨日收盘, f3=涨跌幅, f17=今开, f4=涨跌额, f5=成交量, f6=成交额, f8=换手率, f9=动态市盈率}
     * <p>
     * # 股票、债券榜单表头
     * EASTMONEY_QUOTE_FIELDS = {
     * 'f12': '代码',
     * 'f14': '名称',
     * 'f3': '涨跌幅',
     * 'f2': '最新价',
     * 'f15': '最高',
     * 'f16': '最低',
     * 'f17': '今开',
     * 'f4': '涨跌额',
     * 'f8': '换手率',
     * 'f10': '量比',
     * 'f9': '动态市盈率',
     * 'f5': '成交量',
     * 'f6': '成交额',
     * 'f18': '昨日收盘',
     * 'f20': '总市值',
     * 'f21': '流通市值',
     * 'f13': '市场编号'
     * }
     */
    private static void initEASTMONEYQUOTEFIELDS() {
        String rawJsonFromPython = "{\"f12\": \"\\u4ee3\\u7801\", \"f14\": \"\\u540d\\u79f0\", \"f3\": " +
                "\"\\u6da8\\u8dcc\\u5e45\", \"f2\": \"\\u6700\\u65b0\\u4ef7\", \"f15\": \"\\u6700\\u9ad8\", \"f16\": \"\\u6700\\u4f4e\", \"f17\": \"\\u4eca\\u5f00\", \"f4\": \"\\u6da8\\u8dcc\\u989d\", \"f8\": \"\\u6362\\u624b\\u7387\", \"f10\": \"\\u91cf\\u6bd4\", \"f9\": \"\\u52a8\\u6001\\u5e02\\u76c8\\u7387\", \"f5\": \"\\u6210\\u4ea4\\u91cf\", \"f6\": \"\\u6210\\u4ea4\\u989d\", \"f18\": \"\\u6628\\u65e5\\u6536\\u76d8\", \"f20\": \"\\u603b\\u5e02\\u503c\", \"f21\": \"\\u6d41\\u901a\\u5e02\\u503c\", \"f13\": \"\\u5e02\\u573a\\u7f16\\u53f7\"}\n";
        Map<String, Object> temp = JSONUtil.parseObj(rawJsonFromPython);
        for (String key : temp.keySet()) {
            EASTMONEY_QUOTE_FIELDS.put(key, temp.get(key).toString());
        }
        Console.log(EASTMONEY_QUOTE_FIELDS);
    }

    /**
     * python json.dumps() 复制而来, java处理为 ConcurrentHashMap
     * 实时截面数据可以传递的市场参数有:
     * 实际的市场参数可传递的列表是:
     * ['bond', '可转债', 'stock', '沪深A股', '沪深京A股', '北证A股', '北A', 'futures', '期货', '上证A股', '沪A',
     * '深证A股', '深A', '新股', '创业板', '科创板', '沪股通', '深股通', '风险警示板', '两网及退市',
     * '地域板块', '行业板块', '概念板块', '上证系列指数', '深证系列指数', '沪深系列指数', 'ETF', 'LOF',
     * '美股', '港股', '英股', '中概股', '中国概念股']
     * <p>
     * # ! Powerful
     * FS_DICT = {
     * # 可转债
     * 'bond': 'b:MK0354',
     * '可转债': 'b:MK0354',
     * 'stock': 'm:0 t:6,m:0 t:80,m:1 t:2,m:1 t:23,m:0 t:81 s:2048',
     * # 沪深A股
     * # 'stock': 'm:0 t:6,m:0 t:80,m:1 t:2,m:1 t:23',
     * '沪深A股': 'm:0 t:6,m:0 t:80,m:1 t:2,m:1 t:23',
     * '沪深京A股': 'm:0 t:6,m:0 t:80,m:1 t:2,m:1 t:23,m:0 t:81 s:2048',
     * '北证A股': 'm:0 t:81 s:2048',
     * '北A': 'm:0 t:81 s:2048',
     * # 期货
     * 'futures': 'm:113,m:114,m:115,m:8,m:142',
     * '期货': 'm:113,m:114,m:115,m:8,m:142',
     * '上证A股': 'm:1 t:2,m:1 t:23',
     * '沪A': 'm:1 t:2,m:1 t:23',
     * '深证A股': 'm:0 t:6,m:0 t:80',
     * '深A': 'm:0 t:6,m:0 t:80',
     * # 沪深新股
     * '新股': 'm:0 f:8,m:1 f:8',
     * '创业板': 'm:0 t:80',
     * '科创板': 'm:1 t:23',
     * '沪股通': 'b:BK0707',
     * '深股通': 'b:BK0804',
     * '风险警示板': 'm:0 f:4,m:1 f:4',
     * '两网及退市': 'm:0 s:3',
     * # 板块
     * '地域板块': 'm:90 t:1 f:!50',
     * '行业板块': 'm:90 t:2 f:!50',
     * '概念板块': 'm:90 t:3 f:!50',
     * # 指数
     * '上证系列指数': 'm:1 s:2',
     * '深证系列指数': 'm:0 t:5',
     * '沪深系列指数': 'm:1 s:2,m:0 t:5',
     * # ETF 基金
     * 'ETF': 'b:MK0021,b:MK0022,b:MK0023,b:MK0024',
     * # LOF 基金
     * 'LOF': 'b:MK0404,b:MK0405,b:MK0406,b:MK0407',
     * '美股': 'm:105,m:106,m:107',
     * '港股': 'm:128 t:3,m:128 t:4,m:128 t:1,m:128 t:2',
     * '英股': 'm:155 t:1,m:155 t:2,m:155 t:3,m:156 t:1,m:156 t:2,m:156 t:5,m:156 t:6,m:156 t:7,m:156 t:8',
     * '中概股': 'b:MK0201',
     * '中国概念股': 'b:MK0201'
     * <p>
     * }
     */
    private static void initFSDICT() {
        String rawJsonFromPython = "{\"bond\": \"b:MK0354\", \"\\u53ef\\u8f6c\\u503a\": \"b:MK0354\", \"stock\": " +
                "\"m:0 t:6,m:0 t:80,m:1 t:2,m:1 t:23,m:0 t:81 s:2048\", \"\\u6caa\\u6df1A\\u80a1\": \"m:0 t:6,m:0 t:80,m:1 t:2,m:1 t:23\", \"\\u6caa\\u6df1\\u4eacA\\u80a1\": \"m:0 t:6,m:0 t:80,m:1 t:2,m:1 t:23,m:0 t:81 s:2048\", \"\\u5317\\u8bc1A\\u80a1\": \"m:0 t:81 s:2048\", \"\\u5317A\": \"m:0 t:81 s:2048\", \"futures\": \"m:113,m:114,m:115,m:8,m:142\", \"\\u671f\\u8d27\": \"m:113,m:114,m:115,m:8,m:142\", \"\\u4e0a\\u8bc1A\\u80a1\": \"m:1 t:2,m:1 t:23\", \"\\u6caaA\": \"m:1 t:2,m:1 t:23\", \"\\u6df1\\u8bc1A\\u80a1\": \"m:0 t:6,m:0 t:80\", \"\\u6df1A\": \"m:0 t:6,m:0 t:80\", \"\\u65b0\\u80a1\": \"m:0 f:8,m:1 f:8\", \"\\u521b\\u4e1a\\u677f\": \"m:0 t:80\", \"\\u79d1\\u521b\\u677f\": \"m:1 t:23\", \"\\u6caa\\u80a1\\u901a\": \"b:BK0707\", \"\\u6df1\\u80a1\\u901a\": \"b:BK0804\", \"\\u98ce\\u9669\\u8b66\\u793a\\u677f\": \"m:0 f:4,m:1 f:4\", \"\\u4e24\\u7f51\\u53ca\\u9000\\u5e02\": \"m:0 s:3\", \"\\u5730\\u57df\\u677f\\u5757\": \"m:90 t:1 f:!50\", \"\\u884c\\u4e1a\\u677f\\u5757\": \"m:90 t:2 f:!50\", \"\\u6982\\u5ff5\\u677f\\u5757\": \"m:90 t:3 f:!50\", \"\\u4e0a\\u8bc1\\u7cfb\\u5217\\u6307\\u6570\": \"m:1 s:2\", \"\\u6df1\\u8bc1\\u7cfb\\u5217\\u6307\\u6570\": \"m:0 t:5\", \"\\u6caa\\u6df1\\u7cfb\\u5217\\u6307\\u6570\": \"m:1 s:2,m:0 t:5\", \"ETF\": \"b:MK0021,b:MK0022,b:MK0023,b:MK0024\", \"LOF\": \"b:MK0404,b:MK0405,b:MK0406,b:MK0407\", \"\\u7f8e\\u80a1\": \"m:105,m:106,m:107\", \"\\u6e2f\\u80a1\": \"m:128 t:3,m:128 t:4,m:128 t:1,m:128 t:2\", \"\\u82f1\\u80a1\": \"m:155 t:1,m:155 t:2,m:155 t:3,m:156 t:1,m:156 t:2,m:156 t:5,m:156 t:6,m:156 t:7,m:156 t:8\", \"\\u4e2d\\u6982\\u80a1\": \"b:MK0201\", \"\\u4e2d\\u56fd\\u6982\\u5ff5\\u80a1\": \"b:MK0201\"}\n";
        Map<String, Object> temp = JSONUtil.parseObj(rawJsonFromPython);
        for (String key : temp.keySet()) {
            FS_DICT.put(key, temp.get(key).toString());
        }
    }

    /**
     * 获取当日某个股的涨跌停限价. [涨停价,跌停价]. 调用盘口 api;
     * 一般而言并不会出现 "-" 导致解析错误
     * TODO: 2022/2/14/014 该api数据刷新时间??
     *
     * @param stock
     * @return
     */
    @TimeoutCache(timeout = "1 * 3600 * 1000")
    public static List<Double> getStockPriceLimitToday(String stockCodeSimple, int timeout, int retry)
            throws Exception {
        String cacheKey = stockCodeSimple;
        List<Double> res = stockPriceLimitCache.get(cacheKey);
        if (res != null) {
            return res;
        }
        res = new ArrayList<>();
        JSONObject resp = getStockHandicapCore(stockCodeSimple, "f51,f52", timeout, retry);
        res.add(Double.valueOf(resp.getByPath("data.f51").toString())); // 涨停价
        res.add(Double.valueOf(resp.getByPath("data.f52").toString())); // 跌停价
        stockPriceLimitCache.put(cacheKey, res);
        return res;
    }

    /**
     * 获取个股昨收今开.
     * 1小时有效期缓存.
     * 通常昨收不会有问题, 今开在开盘以前为 "-", 将解析错误, 使用-1.0 替代
     *
     * @param stockCodeSimple
     * @param timeout
     * @return
     * @throws Exception
     */
    @TimeoutCache(timeout = "1 * 3600 * 1000")
    public static List<Double> getStockPreCloseAndTodayOpen(String stockCodeSimple, int timeout, int retry)
            throws Exception {
        String cacheKey = stockCodeSimple;
        List<Double> res = stockPreCloseAndTodayOpenCache.get(cacheKey);
        if (res != null && !res.contains(-1.0)) { // 注意可能并未刷新, 因此需要加上此限制条件
            return res;
        }
        JSONObject resp = getStockHandicapCore(stockCodeSimple, "f60,f46", timeout, retry);
        res = new ArrayList<>();
        try {
            res.add(Double.valueOf(resp.getByPath("data.f60").toString())); // 昨收
        } catch (NumberFormatException e) {
            e.printStackTrace();
            res.add(-1.0);
        }
        try {
            res.add(Double.valueOf(resp.getByPath("data.f46").toString())); // 今开
        } catch (NumberFormatException e) {
            e.printStackTrace();
            res.add(-1.0);
        }
        stockPreCloseAndTodayOpenCache.put(cacheKey, res);
        return res;
    }

    /**
     * 个股实时盘口数据, 包含买卖盘, 常规行情项等.
     *
     * @param stockCodeSimple
     * @param fields
     * @param timeout
     * @return
     * @see getStockHandicapCore
     */
    private static StockHandicap getStockHandicap(String stockCodeSimple, int timeout, int retry) {
        JSONObject resp;
        try {
            resp = getStockHandicapCore(stockCodeSimple, StockHandicap.fieldsStr, timeout, retry);
        } catch (Exception e) {
            log.error("get exception: 获取个股实时盘口数据失败: stock: {}", stockCodeSimple);
            return null;
        }
        JSONObject rawJson = (JSONObject) resp.get("data");
        if (rawJson == null) {
            log.error("data字段为null: 获取个股实时盘口数据失败: stock: {}", stockCodeSimple);
            return null;
        }
        return new StockHandicap(rawJson);
    }


    /**
     * private, 不使用缓存. 由各个具体字段api, 自行决定缓存机制
     * <p>
     * 实时核心盘口数据. 该api可访问极多数据项.
     * 东方财富行情页面, 主api之一, 有很多字段, 包括盘口. 均使用此url, 可传递极多字段
     * http://push2.eastmoney.com/api/qt/stock/get?ut=fa5fd1943c7b386f172d6893dbfba10b&invt=2&fltt=2&fields=f43,f57,f58,f169,f170,f46,f44,f51,f168,f47,f164,f163,f116,f60,f45,f52,f50,f48,f167,f117,f71,f161,f49,f530,f135,f136,f137,f138,f139,f141,f142,f144,f145,f147,f148,f140,f143,f146,f149,f55,f62,f162,f92,f173,f104,f105,f84,f85,f183,f184,f185,f186,f187,f188,f189,f190,f191,f192,f107,f111,f86,f177,f78,f110,f260,f261,f262,f263,f264,f267,f268,f250,f251,f252,f253,f254,f255,f256,f257,f258,f266,f269,f270,f271,f273,f274,f275,f127,f199,f128,f193,f196,f194,f195,f197,f80,f280,f281,f282,f284,f285,f286,f287,f292,f293,f181,f294,f295,f279,f288&secid=0.300059&cb=jQuery112409228148447288975_1643015501069&_=1643015501237
     * <p>
     * 其中 f51(涨停),f52(跌停) 是该股票涨跌停价格
     * jQuery112409228148447288975_1643015501069({"rc":0,"rt":4,"svr":181233083,"lt":1,"full":1,"data":{"f51":39.89,"f52":26.59}});
     * https://push2.eastmoney.com/api/qt/stock/get?ut=fa5fd1943c7b386f172d6893dbfba10b&invt=2&fltt=2&fields=f51,f52&secid=0.300059&cb=jQuery112409228148447288975_1643015501069&_=1643015501237
     * <p>
     * {
     * "rc": 0,
     * "rt": 4,
     * "svr": 181669437,
     * "lt": 1,
     * "full": 1,
     * "data": {
     * "f43": 8.74, 最新
     * "f44": 8.76, 最高
     * "f45": 8.67, 最低
     * "f46": 8.7,  今开
     * "f47": 376829, 总手
     * "f48": 328719168.0,  金额
     * "f49": 221532, 外盘
     * "f50": 0.96,  量比
     * "f51": 9.56,  涨停
     * "f52": 7.82,  跌停
     * "f55": 1.415091382, 公司核心数据-收益(三)
     * "f57": "600000", 股票代码
     * "f58": "浦发银行", 名称
     * "f60": 8.69, 昨收
     * "f62": 3,
     * "f71": 8.72, 均价
     * "f78": 0,
     * "f80": "[{\"b\":202202110930,\"e\":202202111130},{\"b\":202202111300,\"e\":202202111500}]", 交易时间
     * "f84": 29352168006.0, 总股本
     * "f85": 29352168006.0, 流通股本
     * "f86": 1644566391,
     * "f92": 18.7324153, 每股净资产
     * "f104": 191137000000.0,
     * "f105": 41536000000.0, 净利润
     * "f107": 1,
     * "f110": 1,
     * "f111": 2,
     * "f116": 256537948372.44, 总市值
     * "f117": 256537948372.44, 流通市值
     * "f127": "银行",
     * "f128": "上海板块",
     * "f135": 129358320.0,
     * "f136": 129068671.0,
     * "f137": 289649.0,
     * "f138": 38749279.0,
     * "f139": 51406071.0,
     * "f140": -12656792.0,
     * "f141": 90609041.0,
     * "f142": 77662600.0,
     * "f143": 12946441.0,
     * "f144": 99927841.0,
     * "f145": 109458394.0,
     * "f146": -9530553.0,
     * "f147": 88260797.0,
     * "f148": 79019893.0,
     * "f149": 9240904.0,
     * "f161": 155297,  内盘
     * "f162": 4.63,  市盈率[动]
     * "f163": 4.4,
     * "f164": 4.65,
     * "f167": 0.47, 市净率
     * "f168": 0.13, 换手 0.13%
     * "f169": 0.05,  涨跌价格值
     * "f170": 0.58, // 涨幅%
     * "f173": 7.25,  ROE%
     * "f177": 577,
     * "f183": 143484000000.0,  总营收
     * "f184": -3.5278455736, 总营收同比
     * "f185": -7.165526798087, 净利润同比
     * "f186": 0.0,  毛利率%
     * "f187": 29.3614619052, 净利率
     * "f188": 91.6841375217, 负债率
     * "f189": 19991110,
     * "f190": 6.300658948334, 每股未分配利润(元)
     * "f191": -41.62, 委比
     * "f192": -25487, 委差
     * "f193": 0.09,
     * "f194": -3.85,
     * "f195": 3.94,
     * "f196": -2.9,
     * "f197": 2.81,
     * "f199": 90,
     * "f250": "-",
     * "f251": "-",
     * "f252": "-",
     * "f253": "-",
     * "f254": "-",
     * "f255": 0,
     * "f256": "-",
     * "f257": 0,
     * "f258": "-",
     * "f262": "110059",
     * "f263": 1,
     * "f264": "浦发转债",
     * "f266": 107.32,
     * "f267": 106.98,
     * "f268": -0.32,
     * "f269": "-",
     * "f270": 0,
     * "f271": "-",
     * "f273": "-",
     * "f274": "-",
     * "f275": "-",
     * "f280": "-",
     * "f281": "-",
     * "f282": "-",
     * "f284": 0,
     * "f285": "-",
     * "f286": 0,
     * "f287": "-",
     * "f292": 5,
     * "f31": 8.78, // 卖5
     * "f32": 7546, // 卖5量
     * "f33": 8.77, // 卖4
     * "f34": 5681,
     * "f35": 8.76, // 卖3
     * "f36": 11189,
     * "f37": 8.75, // 卖2
     * "f38": 10413,
     * "f39": 8.74, // 卖1
     * "f40": 8531, // 卖1量
     * "f19": 8.73, // 买1
     * "f20": 3553, // 买1量
     * "f17": 8.72,
     * "f18": 4841,
     * "f15": 8.71,
     * "f16": 1406,
     * "f13": 8.7,
     * "f14": 6429,
     * "f11": 8.69, // 买5
     * "f12": 1644 // 买5量
     * }
     * }
     *
     * @param stockSimpleCode
     * @param fields          访问的字段列表, 逗号分割
     * @param timeout
     * @return 解析的json响应. 具体字段的访问由调用方决定. date.字段名: Double.valueOf(resp.getByPath("data.f51").toString())
     * @throws Exception
     */
    private static JSONObject getStockHandicapCore(String stockSimpleCode, String fields, int timeout, int retry)
            throws Exception {
        SecurityBeanEm bean = SecurityBeanEm.createStock(stockSimpleCode);
        String secId = bean.getSecId(); // 获取准确的secId

        String url = "https://push2.eastmoney.com/api/qt/stock/get";
        List<Double> res = new ArrayList<>();
        Map<String, Object> params = new HashMap<>(); // 参数map
        params.put("ut", "fa5fd1943c7b386f172d6893dbfba10b");
        params.put("invt", "2");
        params.put("fltt", "2");
        params.put("fields", fields);
        params.put("secid", secId);
        params.put("cb", StrUtil.format("jQuery112409885675811656662_{}",
                System.currentTimeMillis() - RandomUtil.randomInt(1000)));
        params.put("_", System.currentTimeMillis());

        String response;
        try {
            response = getAsStrUseKevin(url, params, timeout, retry);
        } catch (Exception e) {
            e.printStackTrace();

            log.error("get exception: 访问http失败: 获取涨跌停价: stock: {}", secId);
            return null;
        }
        response = response.substring(response.indexOf("(") + 1, response.lastIndexOf(")"));
        return JSONUtil.parseObj(response);
    }

    /**
     * 获取指数昨收今开, 使用盘口api
     * 用-1.0 代表 - , 即暂无数据.
     *
     * @param indexSimpleCode
     * @param timeout
     * @return
     * @throws Exception
     */
    public static List<Double> getPreCloseAndTodayOpenOfIndex(String indexSimpleCode, int timeout) throws Exception {
        JSONObject resp = getIndexHandicapCore(indexSimpleCode, "f60,f46", timeout); // 字段同个股. 昨收今开
        List<Double> res = new ArrayList<>();
        try {
            res.add(Double.valueOf(resp.getByPath("data.f60").toString()) / 100); // 昨收 , 注意/100
        } catch (Exception e) {
            e.printStackTrace();
            res.add(-1.0);
        }
        try {
            res.add(Double.valueOf(resp.getByPath("data.f46").toString()) / 100); // 今开
        } catch (Exception e) {
            e.printStackTrace();
            res.add(-1.0);
        }
        return res;
    }

    /**
     * 指数实时行情. 盘口
     * http://push2.eastmoney.com/api/qt/stock/get?invt=2&fltt=1&cb=jQuery351037846734899553613_1644751180897&fields=f58%2Cf107%2Cf57%2Cf43%2Cf59%2Cf169%2Cf170%2Cf152%2Cf46%2Cf60%2Cf44%2Cf45%2Cf47%2Cf48%2Cf19%2Cf532%2Cf39%2Cf161%2Cf49%2Cf171%2Cf50%2Cf86%2Cf600%2Cf601%2Cf154%2Cf84%2Cf85%2Cf168%2Cf108%2Cf116%2Cf167%2Cf164%2Cf92%2Cf71%2Cf117%2Cf292%2Cf113%2Cf114%2Cf115%2Cf119%2Cf120%2Cf121%2Cf122&secid=1.000001&ut=fa5fd1943c7b386f172d6893dbfba10b&_=1644751180898
     * 数据实例
     * {
     * "rc": 0,
     * "rt": 4,
     * "svr": 182995791,
     * "lt": 1,
     * "full": 1,
     * "data": {
     * "f43": 346295, 最新
     * "f44": 350015, 最高
     * "f45": 345933, 最低
     * "f46": 347228, 今开
     * "f47": 361356091, 成交量
     * "f48": 426297925632.0, 成交额
     * "f49": 168609531, 外盘
     * "f50": 107,
     * "f57": "000001",
     * "f58": "上证指数",
     * "f59": 2,
     * "f60": 348591, 昨收
     * "f71": "-",
     * "f84": 4629436239233.0,
     * "f85": 4061217883902.0,
     * "f86": 1644566379,
     * "f92": "-",
     * "f107": 1,
     * "f108": "-",
     * "f113": 285, 涨家数
     * "f114": 1765, 跌家数
     * "f115": 44, 平家数
     * "f116": 49300990877782.0,
     * "f117": 41749793711638.0, 流通市值
     * "f119": 302, 5日涨跌幅, /100百分比
     * "f120": -326, 20日涨跌幅
     * "f121": -198, 60日
     * "f122": -486, 今年
     * "f152": 2,
     * "f154": 4,
     * "f161": 192746560, 内盘
     * "f164": "-",
     * "f167": "-",
     * "f168": 89, 换手率, /100百分比, 0.89%
     * "f169": -2296, 涨跌点数, 注意 /100, 实际 22.96点
     * "f170": -66, 涨跌幅度, 同样 /100且百分比,  -0.66%
     * "f171": 117, 振幅, /100百分比  1.17%
     * "f292": 5,
     * "f39": "-",
     * "f40": "-",
     * "f19": "-",
     * "f20": "-",
     * "f600": "-",
     * "f601": "-"
     * }
     * }
     *
     * @param indexCodeSimple
     * @param fields
     * @param timeout
     * @return
     * @throws Exception
     */
    private static JSONObject getIndexHandicapCore(String indexCodeSimple, String fields, int timeout)
            throws Exception {
        SecurityBeanEm bean = SecurityBeanEm.createIndex(indexCodeSimple);
        String secId = bean.getSecId(); // 获取准确的secId

        String url = "https://push2.eastmoney.com/api/qt/stock/get";

        List<Double> res = new ArrayList<>();
        Map<String, Object> params = new HashMap<>(); // 参数map
        params.put("ut", "fa5fd1943c7b386f172d6893dbfba10b");
        params.put("invt", "2");
        params.put("fltt", "1"); // 与股票相比, 仅本参数由2变1
        params.put("fields", fields);
        params.put("secid", secId);
        params.put("cb", StrUtil.format("jQuery351037846734899553613_{}", // callback有变化
                System.currentTimeMillis() - RandomUtil.randomInt(1000)));
        params.put("_", System.currentTimeMillis());

        String response;
        try {
            response = getAsStrUseKevin(url, params, timeout, 3);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("get exception: 访问http失败: 获取涨跌停价: stock: {}", secId);
            return null;
        }
        response = response.substring(response.indexOf("(") + 1, response.lastIndexOf(")"));
        return JSONUtil.parseObj(response);
    }


    /**
     * 获取分时成交
     * // @using
     * https://push2.eastmoney.com/api/qt/stock/details/get?ut=fa5fd1943c7b386f172d6893dbfba10b&fields1=f1,f2,f3,f4&fields2=f51,f52,f53,f54,f55&pos=-1400&secid=0.000153&cb=jQuery112409885675811656662_1640099646776&_=1640099646777
     * // @noti: 升序, 但是最新 x 条数据
     *
     * @param lastRecordAmounts 单页数量,
     * @param stockCodeSimple   股票/指数简单代码, 不再赘述
     * @param market            0 深市  1 沪市    (上交所暂时 0)
     * @return 出错则抛出异常
     * @noti: 8:55  details字段已经重置为 [] 空列表  todo: 时间限制更严格
     */
    public static DataFrame<Object> getFSTransaction(Integer lastRecordAmounts, String stockCodeSimple,
                                                     Integer market, int timeout) throws Exception {
        List<String> columns = Arrays.asList("stock_code", "market", "time_tick", "price", "vol", "bs");
        DataFrame<Object> res;
        String keyUrl = "https://push2.eastmoney.com/api/qt/stock/details/get";
        String response;
        try {
            Map<String, Object> params = new HashMap<>(); // 参数map
            params.put("ut", "fa5fd1943c7b386f172d6893dbfba10b");
            params.put("fields1", "f1,f2,f3,f4");
            params.put("fields2", "f51,f52,f53,f54,f55");
            params.put("pos", StrUtil.format("-{}", lastRecordAmounts));
            params.put("secid", StrUtil.format("{}.{}", market, stockCodeSimple));
            params.put("cb", StrUtil.format("jQuery112409885675811656662_{}",
                    System.currentTimeMillis() - RandomUtil.randomInt(1000)));
            params.put("_", System.currentTimeMillis());

            response = getAsStrUseHutool(keyUrl, params, timeout, 1);
        } catch (Exception e) {
            log.error("get exception: 访问http失败: stock: {}.{}", market, stockCodeSimple);
            throw e;
        }

        try {
            res = jsonStrToDf(response, "(", ")", columns,
                    Arrays.asList("data", "details"), String.class, Arrays.asList(3),
                    Arrays.asList(stockCodeSimple, market));
        } catch (Exception e) {
            log.warn("get exception: 获取数据错误. stock: {}.{}", market, stockCodeSimple);
            log.warn("raw data: 原始响应字符串: {}", response);
            throw e;
        }
        return res;
    }


    public static DataFrame<Object> getFSTransaction(Integer lastRecordAmounts, String stockCodeSimple,
                                                     Integer market) throws Exception {
        return getFSTransaction(lastRecordAmounts, stockCodeSimple, market, DEFAULT_TIMEOUT);
    }

    /**
     * efinance get_realtime_quotes 重载实现.  给定市场列表, 返回, 这些市场所有股票列表 截面数据
     * // ['f12', 'f14', 'f3', 'f2', 'f15', 'f16', 'f17', 'f4', 'f8', 'f10', 'f9', 'f5', 'f6', 'f18', 'f20', 'f21',
     * 'f13']
     * <p>
     * * ['bond', '可转债', 'stock', '沪深A股', '沪深京A股', '北证A股', '北A', 'futures', '期货', '上证A股', '沪A',
     * * '深证A股', '深A', '新股', '创业板', '科创板', '沪股通', '深股通', '风险警示板', '两网及退市',
     * * '地域板块', '行业板块', '概念板块', '上证系列指数', '深证系列指数', '沪深系列指数', 'ETF', 'LOF',
     * * '美股', '港股', '英股', '中概股', '中国概念股']
     *
     * @param markets
     * @return
     * @see StockApi.FS_DICT
     * @see StockApi.EASTMONEY_QUOTE_FIELDS
     * @see StockApi.MARKET_NUMBER_DICT
     */
    public static DataFrame<Object> getRealtimeQuotes(List<String> markets) {
        List<String> realMarketArgs = markets.stream().map(value -> FS_DICT.get(value)).collect(Collectors.toList());
        String marketArgsStr = StrUtil.join(",", realMarketArgs);
        //['f12', 'f14', 'f3', 'f2', 'f15', 'f16', 'f17', 'f4', 'f8', 'f10', 'f9', 'f5', 'f6', 'f18', 'f20', 'f21', 'f13']
        String url = "http://push2.eastmoney.com/api/qt/clist/get";
        List<String> fields = Arrays.asList("f12", "f14", "f3", "f2", "f15", "f16", "f17", "f4", "f8",
                "f10", "f9", "f5", "f6", "f18", "f20", "f21", "f13");
        String fieldsStr = StrUtil.join(",", fields);
        HashMap<String, Object> params = new HashMap<>();
        params.put("pn", "1");
        params.put("pz", "1000000");
        params.put("po", "1");
        params.put("np", "1");
        params.put("fltt", "2");
        params.put("invt", "2");
        params.put("fid", "f3");
        params.put("fs", marketArgsStr);
        params.put("fields", fieldsStr);
        String response = getAsStrUseKevin(url, params, 4000);
        DataFrame<Object> dfTemp = jsonStrToDf(response, null, null,
                fields, Arrays.asList("data", "diff"), JSONObject.class, Arrays.asList(),
                Arrays.asList());
        dfTemp = dfTemp.rename(EASTMONEY_QUOTE_FIELDS);
        dfTemp = dfTemp.add("行情id", values ->
                values.get(16).toString() + "." + values.get(0).toString()
        );
        dfTemp = dfTemp.add("市场类型", values ->
                MARKET_NUMBER_DICT.get(values.get(16).toString())
        );
        dfTemp = dfTemp.rename("代码", "股票代码");
        dfTemp = dfTemp.rename("名称", "股票名称");
        return dfTemp;
    }

    /**
     * 获取k线. 复刻 efinance   get_quote_history
     * 日期	   开盘	   收盘	   最高	   最低	    成交量	          成交额	   振幅	  涨跌幅	  涨跌额	 换手率	  股票代码	股票名称
     * <p>
     * 获取股票的 K 线数据
     * <p>
     * Parameters
     * ----------
     * stock_codes : Union[str,List[str]]
     * 股票代码、名称 或者 股票代码、名称构成的列表
     * beg : str, optional
     * 开始日期，默认为 ``'19000101'`` ，表示 1900年1月1日
     * end : str, optional
     * 结束日期，默认为 ``'20500101'`` ，表示 2050年1月1日
     * klt : int, optional
     * 行情之间的时间间隔，默认为 ``101`` ，可选示例如下
     * <p>
     * - ``1`` : 分钟
     * - ``5`` : 5 分钟
     * - ``15`` : 15 分钟
     * - ``30`` : 30 分钟
     * - ``60`` : 60 分钟
     * - ``101`` : 日
     * - ``102`` : 周
     * - ``103`` : 月
     * <p>
     * fqt : int, optional
     * 复权方式，默认为 ``1`` ，可选示例如下
     * <p>
     * - ``0`` : 不复权
     * - ``1`` : 前复权
     * - ``2`` : 后复权
     * <p>
     * Returns
     * -------
     * Union[DataFrame, Dict[str, DataFrame]]
     * 股票的 K 线数据
     * <p>
     * - ``DataFrame`` : 当 ``stock_codes`` 是 ``str`` 时
     * - ``Dict[str, DataFrame]`` : 当 ``stock_codes`` 是 ``List[str]`` 时
     * <p>
     * Examples
     * // @noti: 前后两日期都包含, 且天然升序!
     *
     * @param stockCodesSimple
     * @param begDate
     * @param endDate
     * @param klType
     * @param fq
     * @param retrySingle
     * @param isIndex          给的股票代码列表, 是否指数??
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public static ConcurrentHashMap<String, DataFrame<Object>> getQuoteHistory(List<String> stockCodesSimple,
                                                                               String begDate,
                                                                               String endDate,
                                                                               String klType, String fq,
                                                                               int retrySingle,
                                                                               boolean isIndex,
                                                                               int timeoutOfReq,
                                                                               boolean useCache
    )
            throws ExecutionException, InterruptedException {

        log.info("klines gets batch: 线程池批量获取k线, 股票数量: {}", stockCodesSimple.size());
        checkPoolExecutor();
        HashMap<String, Future<DataFrame<Object>>> futures = new HashMap<>();
        for (String stock : stockCodesSimple) {
            futures.put(stock, poolExecutor.submit(new Callable<DataFrame<Object>>() {
                @Override
                public DataFrame<Object> call() throws Exception {
                    return getQuoteHistorySingle(stock, begDate, endDate, klType, fq, retrySingle, isIndex,
                            timeoutOfReq, useCache);
                }
            }));
        }
        ConcurrentHashMap<String, DataFrame<Object>> res = new ConcurrentHashMap<>();
        for (String stock : Tqdm.tqdm(stockCodesSimple, "process: ")) {
            DataFrame<Object> dfTemp = futures.get(stock).get();
            if (dfTemp != null) {
                res.put(stock, dfTemp);
            }
        }
        Console.log();
        log.info("finish klines gets batch: 线程池批量获取k线完成");
        return res;
    }

    /**
     * 单股票k线:
     * 日期	   开盘	   收盘	   最高	   最低	    成交量	          成交额	   振幅	   涨跌幅	   涨跌额	  换手率	  股票代码	股票名称
     *
     * @param stock       @noti: 绝对传递 simple模式, 是否指数由  isIndex 参数控制
     * @param begDate
     * @param endDate
     * @param klType
     * @param fq
     * @param retrySingle
     * @param quoteIdMode
     * @return
     */
    @CanCache
    @TimeoutCache(timeout = "4 * 3600 * 1000")
    public static DataFrame<Object> getQuoteHistorySingle(String stock, String begDate,
                                                          String endDate, String klType, String fq,
                                                          int retrySingle, boolean isIndex, int timeout,
                                                          boolean useCache)
            throws Exception {
        if (begDate == null) {
            begDate = "19900101";
        }
        if (endDate == null) {
            endDate = "20500101";
        }
        begDate = begDate.replace("-", ""); // 标准化
        endDate = endDate.replace("-", "");

        String cacheKey = StrUtil.format("{}__{}__{}__{}__{}__{}__{}", stock, begDate, endDate, klType, fq, retrySingle,
                isIndex);

        DataFrame<Object> res = null;
        if (useCache) { // 必须使用caCache 且真的存在缓存
            res = quoteHistorySingleCache.get(cacheKey);
        }
        if (res != null) {
            return res;
        }
        String fieldsStr = "f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61";// k线字段
        List<String> fields = StrUtil.split(fieldsStr, ",");
        JSONArray quoteRes = querySecurityId(stock);
        SecurityBeanEm bean = new SecurityBeanEm(quoteRes);
        String quoteId = null;
        try { // 转换失败
            quoteId = isIndex ? bean.convertToIndex().getSecId() : bean.convertToStock().getSecId();
        } catch (Exception e) {
            throw e;
        }

        HashMap<String, Object> params = new HashMap<>();
        params.put("fields1", "f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13");
        params.put("fields2", fieldsStr);
        params.put("beg", begDate);
        params.put("end", endDate);
        params.put("rtntype", "6");
        params.put("secid", quoteId);
        params.put("klt", klType);
        params.put("fqt", fq);

        String url = "https://push2his.eastmoney.com/api/qt/stock/kline/get";
        String response;
        try {
            response = getAsStrUseHutool(url, params, timeout, retrySingle);
        } catch (Exception e) {
            return null;
        }
        DataFrame<Object> dfTemp = jsonStrToDf(response, null, null,
                fields.stream().map(value -> EASTMONEY_KLINE_FIELDS.get(value).toString())
                        .collect(Collectors.toList()),
                Arrays.asList("data", "klines"), String.class, Arrays.asList(),
                Arrays.asList());

        dfTemp = dfTemp.add("股票代码", values -> bean.getStockCodeSimple());
        res = dfTemp.add("股票名称", values -> bean.getName());
        quoteHistorySingleCache.put(cacheKey, res); // 将更新
        return res;
    }

    /**
     * 默认使用cache
     *
     * @param stock
     * @param begDate
     * @param endDate
     * @param klType
     * @param fq
     * @param retrySingle
     * @param isIndex
     * @param timeout
     * @return
     * @throws Exception
     */
    public static DataFrame<Object> getQuoteHistorySingle(String stock, String begDate,
                                                          String endDate, String klType, String fq,
                                                          int retrySingle, boolean isIndex, int timeout
    ) throws Exception {
        return getQuoteHistorySingle(stock, begDate, endDate, klType, fq, retrySingle, isIndex, timeout, true);
    }

    /**
     * 默认使用cache
     *
     * @param stock
     * @param begDate
     * @param endDate
     * @param klType
     * @param fq
     * @param retrySingle
     * @return
     * @throws Exception
     */
    public static DataFrame<Object> getQuoteHistorySingle(String stock, String begDate,
                                                          String endDate, String klType, String fq,
                                                          int retrySingle)
            throws Exception {
        return getQuoteHistorySingle(stock, begDate, endDate, klType, fq, retrySingle, false, 2000);
    }

    /**
     * @param stock
     * @param isIndex
     * @param retrySingle
     * @param timeout
     * @return 1分钟分时图当日; 当某分钟开始后(即0秒以后, fs将更新到当分钟 + 1. 例如当前 13 : 21 : 10, 则将更新到 13 : 22
     * @throws Exception
     */
    public static DataFrame<Object> getFs1MToday(String stock, boolean isIndex, int retrySingle, int timeout,
                                                 boolean useCache)
            throws Exception {
        return getQuoteHistorySingle(stock, null, null, "1", "qfq", retrySingle, isIndex, timeout, useCache);
    }

    public static DataFrame<Object> getFs1MToday(String stock, boolean isIndex, int retrySingle, int timeout
    )
            throws Exception {
        return getQuoteHistorySingle(stock, null, null, "1", "qfq", retrySingle, isIndex, timeout, true);
    }

    /**
     * 给定一个具体日期, yyyy-MM-dd 形式, 因为em默认日期格式是这样, 方便比较.  返回上n个交易日的日期,
     * 原理上查询 上证指数 所有历史日k线, 获取得到日期列表. 倒序遍历即可
     * 返回  yyyy-MM-dd 形式
     *
     * @param todayDate
     * @return
     */
    public static String getPreNTradeDateStrict(String todayDate, int n)
            throws Exception {
        // 查询结果将被缓存.
        DataFrame<Object> dfTemp = getQuoteHistorySingle("000001", "19900101", "21000101", "101", "1", 3, true, 3000);
        List<String> dates = DataFrameS.getColAsStringList(dfTemp, "日期");
        for (int i = dates.size() - 1; i >= 0; i--) {
            if (dates.get(i).compareTo(todayDate) < 0) { // 倒序, 第一个小于 给定日期的, 即为 严格意义的上一个交易日
                // 此时i为  上 1 交易日. 因此..
                try {
                    return dates.get(i + 1 - n);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null; // 索引越界
                }
            }
        }
        return null;
    }

    public static String getPreNTradeDateStrict(String todayDate) throws Exception {
        return getPreNTradeDateStrict(todayDate, 1); // 默认获取today上一交易日
    }

}
