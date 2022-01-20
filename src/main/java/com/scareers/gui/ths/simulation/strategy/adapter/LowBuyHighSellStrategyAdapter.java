package com.scareers.gui.ths.simulation.strategy.adapter;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import com.scareers.gui.ths.simulation.OrderFactory;
import com.scareers.gui.ths.simulation.Response;
import com.scareers.gui.ths.simulation.order.Order;
import com.scareers.gui.ths.simulation.strategy.LowBuyHighSellStrategy;
import com.scareers.gui.ths.simulation.strategy.StrategyAdapter;
import com.scareers.gui.ths.simulation.trader.Trader;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;

import java.util.List;

import static com.scareers.datasource.eastmoney.stock.StockApi.getPreNTradeDateStrict;
import static com.scareers.datasource.eastmoney.stock.StockApi.getQuoteHistorySingle;
import static com.scareers.gui.ths.simulation.strategy.LowBuyHighSellStrategy.STR_SEC_CODE;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/1/20/020-11:39:56
 */
public class LowBuyHighSellStrategyAdapter implements StrategyAdapter {
    LowBuyHighSellStrategy strategy;
    Trader trader;
    String pre2Date; // yyyy-MM-dd

    public LowBuyHighSellStrategyAdapter(LowBuyHighSellStrategy strategy,
                                         Trader trader) throws Exception {
        this.strategy = strategy;
        this.trader = trader;
        pre2Date = getPreNTradeDateStrict(DateUtil.today());
    }

    @Override
    public void buyDecision() throws Exception {
        int sleep = RandomUtil.randomInt(1, 10); // 睡眠n秒
        Thread.sleep(sleep * 2000);
        Order order = null;
        int type = RandomUtil.randomInt(22);
        if (type < 8) {
            order = OrderFactory.generateBuyOrderQuick("600090", 100, 1.2, Order.PRIORITY_HIGHEST);
        } else if (type < 16) {
            order = OrderFactory.generateSellOrderQuick("600090", 100, 1.2, Order.PRIORITY_HIGH);
        } else if (type < 18) {
            order = OrderFactory.generateCancelAllOrder("600090", Order.PRIORITY_HIGH);
        } else if (type < 20) {
            order = OrderFactory.generateCancelSellOrder("600090", Order.PRIORITY_HIGH);
        } else {
            order = OrderFactory.generateCancelBuyOrder("600090", Order.PRIORITY_HIGH);
        }
        trader.putOrderToWaitExecute(order);
    }

    @Override
    public void sellDecision() throws Exception {
        DataFrame<Object> yesterdayStockHoldsBeSell = strategy.getYesterdayStockHoldsBeSell();
        // 证券代码	 证券名称	 股票余额	 可用余额	冻结数量	  成本价	   市价	       盈亏	盈亏比例(%)	   当日盈亏	当日盈亏比(%)	       市值	仓位占比(%)	交易市场	持股天数
        for (int i = 0; i < yesterdayStockHoldsBeSell.size(); i++) {
            List<Object> line = yesterdayStockHoldsBeSell.row(i);
            String stock = line.get(0).toString();
            int amountsTotal = Integer.parseInt(line.get(2).toString()); // 原始总持仓, 今日开卖
            double costPrice = Double.parseDouble(line.get(5).toString()); // 成本价.

            // 1. 读取前日收盘价
            Double pre2ClosePrice = 0.0;
            try {
                //日期	   开盘	   收盘	   最高	   最低	    成交量	          成交额	   振幅	   涨跌幅	   涨跌额	  换手率	  股票代码	股票名称
                pre2ClosePrice = Double.valueOf(getQuoteHistorySingle(stock, pre2Date, pre2Date,
                        "101", "1", 3,
                        false, 2000).row(0).get(2).toString());
            } catch (Exception e) {
                log.warn("skip: data get fail: 获取股票前日收盘价失败 {}", stock);
                continue;
            }

            // 2. 判定当前是否是卖点?


        }


    }

    /**
     * 卖点判定.
     * 1.读取(真)分时图,
     * 2.判定前几分钟分时图 连续上升 n
     * 3.判定本分钟价格 比上一分钟 降低. (过半分钟的时间根据比例, 之前固定返回false)
     * 4.返回false
     *
     * @return
     * @key3 : 参考 SettingsOfFSBacktest 相关设定项
     */
    public boolean isSellPoint() {


        return false;
    }

    @Override
    public void checkBuyOrder(Order order, List<Response> responses, String orderType) {
        checkOtherOrder(order, responses, orderType);
    }

    @Override
    public void checkSellOrder(Order order, List<Response> responses, String orderType) {
        checkOtherOrder(order, responses, orderType);
    }

    @Override
    public void checkOtherOrder(Order order, List<Response> responses, String orderType) {
        JSONObject response = responses.get(responses.size() - 1);
        if ("success".equals(response.getStr("state"))) {
            log.info("执行成功: {}", order.getRawOrderId());
//            log.warn("待执行订单数量: {}", trader.getOrdersWaitForExecution().size());
            order.addLifePoint(Order.LifePointStatus.CHECKED, "执行成功");
        } else {
            log.error("执行失败: {}", order.getRawOrderId());
            log.info(JSONUtil.parseArray(responses).toString());
            order.addLifePoint(Order.LifePointStatus.CHECKED, "执行失败");
        }
        trader.successFinishOrder(order, responses);
    }

    private static final Log log = LogUtil.getLogger();
}