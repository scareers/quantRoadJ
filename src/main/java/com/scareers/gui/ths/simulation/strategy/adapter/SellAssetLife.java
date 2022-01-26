package com.scareers.gui.ths.simulation.strategy.adapter;

import com.scareers.datasource.eastmoney.SecurityBeanEm;

/**
 * description: 对于单只股票(仅限于股票), 今日打算卖出, 记录整个Trader周期, 所有数据.
 * 例如 每分钟tick, 若发生买卖点,相关的 决定仓位的参数值,和仓位.
 * 例如 卖点列表
 *
 * @warning: 高卖分布tick, 必须由小到大, 例如 -0.10-> +0.10; 否则各种加成参数将异常(反向)
 * @author: admin
 * @date: 2022/1/26/026-14:22:34
 */
public class SellAssetLife {
    // 静态属性
    public static final double tickGapDefault = 0.005; // 高卖分布tick间隔, 千分之5

    // 1. 高卖分布仓位算法基本参数默认值
    /**
     * 高卖分布仓位算法简介: 以最简单情况, next0买入, next1尝试高卖
     * 统计某种选股策略next1最高点, 相对于前2日收盘价的百分比, 分布. (将类似于正态分布)
     * 假设此分布平均值在 +0.03附近, 最小值 -0.20, 最大值 +0.20;
     * 根据凯利公式核心思想: "在概率越大的地方, 我们施加更大的注码, 即我们认为确定性越大!"
     * 因此在卖出当日: 当股票价格==+0.01时, 我们的剩余仓位应当极大, 已卖出仓位应当偏小, 因为我们还有极大可能, 价格能够达到 +0.03附近
     */
    public static final double positionCalcKeyArgsOfCdfHighSell = 1.4; // 高卖仓位cdf计算 倍率
    public static final double execHighSellThreshold = -0.02; // 只有价格高于此才可能高卖, 当前价格相对于前日收盘
    public static final double continuousRaiseTickCountThreshold = 1; // fs连续上升n分钟以上,本分钟(大概率)下降, 才可能高卖

    // 2. 指数当时tick涨跌幅加成倍率
    // 高卖等价价格最早实现: highPrice - indexBelongThatTimePriceEnhanceArgHighSell * indexPriceThatTime; // @v2
    // --> @see: FSBacktestOfLowBuyNextHighSell.BacktestTaskOfPerDay.calcEquivalenceCdfUsePriceOfHighSell
    // --> 高卖, 当倍率和大盘+时, 意味着大盘很好, 此时等价价格减小, 因高卖分时分布是从小到大的, 意味着等价价格左移,即减小高卖仓位,等待更高价位再卖出
    // --> 因此倍率若设定负数, 则大盘指数时负反馈
    // @usage: 本参数将*个股对应大盘指数当时涨跌幅.
    // @usage: 当本指数为正数时, 意味着 "大盘越好,我们等价减小高卖仓位,认为未来有更高价位概率大, 慢卖出"!
    // @usage: 当本指数为负数时, 意味着 "大盘越差,我们等价增加高卖仓位,认为未来有更高价位概率大, 慢卖出"!
    public static final double indexBelongThatTimePriceEnhanceArgHighSell = 0.1; // fs连续上升n分钟以上,本分钟下降, 才可能高卖


    // 基本实例属性
    String stockCodeSimpe;
    SecurityBeanEm stockBean;

    // 仓位计算参数

}