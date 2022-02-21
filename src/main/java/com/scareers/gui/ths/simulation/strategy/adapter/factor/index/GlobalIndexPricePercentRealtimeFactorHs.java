package com.scareers.gui.ths.simulation.strategy.adapter.factor.index;

import com.scareers.gui.ths.simulation.strategy.adapter.factor.HsFactor;
import com.scareers.gui.ths.simulation.strategy.adapter.state.HsState;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

import static com.scareers.gui.ths.simulation.strategy.adapter.factor.index.SettingsOfIndexPercentFactor.enhanceRateForHsDefault;

/**
 * description: 大盘指数影响
 *
 * @author: admin
 * @date: 2022/2/20/020-17:27:23
 */
@Setter
@Getter
public class GlobalIndexPricePercentRealtimeFactorHs extends HsFactor {
    // 大盘指数实时涨跌幅 影响倍率, 将乘以实时大盘涨跌幅. 可可人工修改?
    private Double enhanceRate = enhanceRateForHsDefault;

    public GlobalIndexPricePercentRealtimeFactorHs() {
        super(SettingsOfIndexPercentFactor.factorName, SettingsOfIndexPercentFactor.nameCn,
                SettingsOfIndexPercentFactor.description);
    }

    @Override
    public HsState influence(HsState state) {
        Objects.requireNonNull(state, "初始状态不可为null, 设置后方可调用influence进行影响");
        Double changePercent = getIndexPercent(state); // 变化百分比
        if (changePercent == null) { // 数据缺失
            log.error("GlobalIndexPricePercentRealtimeFactor: 指数涨跌幅获取失败, 无法计算影响, 返回原始状态");
            return state; // 相当于不移动
        }
        state.setIndexPricePercentThatTime(changePercent);
        state.movePdf(changePercent); // 高卖低卖, 均是平移pdf. 执行移动
        return state;
    }


    private Double getIndexPercent(HsState state) {
        // 所属大盘指数,默认上证指数, 若非沪A,深A, 将log错误, 但默认使用上证指数
        return SettingsOfIndexPercentFactor.getIndexPercent(state.getBean());
    }
}
