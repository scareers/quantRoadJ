package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.bond;

import cn.hutool.core.util.StrUtil;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.charts.CrossLineListenerForKLineXYPlot;
import com.scareers.utils.charts.CrossLineXIndexChangeCallback;
import com.scareers.utils.charts.EmChartKLine;
import joinery.DataFrame;
import lombok.Data;
import org.jfree.chart.ChartPanel;

import javax.swing.*;
import java.awt.*;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.COLOR_THEME_MINOR;

/**
 * description: 使用东财数据的 K线显示面板
 * 核心为多参数 update() 方法; 本质上, 需要提供 昨日及之前的 k线数据, 以及 "今日当前" 的 4项数据 !! 先更新过去完整数据, 再更新今日数据
 * update()方法需要更新 df, preClose,title
 *
 * @author: admin
 * @date: 2022/4/5/005-06:30:09
 */
@Data
public class EmKLineDisplayPanel extends DisplayPanel {
    public static final int preferHeight = 300;
    public static final int infoPanelWidth = 80; // 信息显示panel宽度
    public static final Color infoLabelColor = new Color(192, 192, 192); // 文字颜色, 偏灰白, 同 同花顺
    public static final Color upColor = new Color(255, 50, 50); // 上升红色
    public static final Color downColor = new Color(0, 230, 0); // 下跌绿色
    public static final Color commonColor = new Color(2, 226, 244); // 振幅成交额换手蓝色!

    // 切换转债时, 应当更新本属性为新的实例; 单转债刷新过程, 则调用 其 updateKLine(...) 方法
    EmChartKLine.DynamicEmKLineChartForRevise dynamicKLineChart;
    ChartPanel chartPanel;

    // 信息panel, 多个label 竖直重叠, 网格1列
    JPanel jPanelOfCurrentKLineInfo;

    // 9项数据!
    JLabel labelOfDate = getCommonLabel("日期", infoLabelColor);
    JLabel labelOfDateValue = getCommonLabel(infoLabelColor);
    JLabel labelOfOpen = getCommonLabel("开盘", infoLabelColor);
    JLabel labelOfOpenValue = getCommonLabel(upColor);
    JLabel labelOfHigh = getCommonLabel("最高", infoLabelColor);
    JLabel labelOfHighValue = getCommonLabel(upColor);
    JLabel labelOfLow = getCommonLabel("最低", infoLabelColor);
    JLabel labelOfLowValue = getCommonLabel(upColor);
    JLabel labelOfClose = getCommonLabel("收盘", infoLabelColor);
    JLabel labelOfCloseValue = getCommonLabel(upColor);
    JLabel labelOfChgPct = getCommonLabel("涨跌幅", infoLabelColor);
    JLabel labelOfChgPctValue = getCommonLabel(upColor);
    JLabel labelOfAmplitude = getCommonLabel("振幅", infoLabelColor);
    JLabel labelOfAmplitudeValue = getCommonLabel(commonColor);
    JLabel labelOfAmount = getCommonLabel("成交额", infoLabelColor);
    JLabel labelOfAmountValue = getCommonLabel(commonColor);
    JLabel labelOfTurnover = getCommonLabel("换手率", infoLabelColor);
    JLabel labelOfTurnoverValue = getCommonLabel(commonColor);

    public EmKLineDisplayPanel() {
        this.setLayout(new BorderLayout());
        JLabel jLabel = new JLabel("暂无k线数据");
        jLabel.setPreferredSize(new Dimension(4096, preferHeight));
        jLabel.setForeground(Color.red);
        jLabel.setBackground(COLOR_THEME_MINOR);
        this.add(jLabel, BorderLayout.CENTER);
        initJPanelOfCurrentKLineInfo();
    }

    /**
     * k线具体数据显示panel
     */
    public void initJPanelOfCurrentKLineInfo() {
        // 原始api的列有这些! 都可以显示
        // 日期	   开盘	   收盘	   最高	   最低	    成交量	成交额	   振幅	   涨跌幅	   涨跌额	  换手率	  资产代码	资产名称
        jPanelOfCurrentKLineInfo = new JPanel(); // 配合十字线, 显示当前那根k线的信息.
        jPanelOfCurrentKLineInfo.setPreferredSize(new Dimension(infoPanelWidth, 4096));
        jPanelOfCurrentKLineInfo.setLayout(new GridLayout(18, 1, -1, -1));

        jPanelOfCurrentKLineInfo.add(labelOfDate);
        jPanelOfCurrentKLineInfo.add(labelOfDateValue);
        jPanelOfCurrentKLineInfo.add(labelOfOpen);
        jPanelOfCurrentKLineInfo.add(labelOfOpenValue);
        jPanelOfCurrentKLineInfo.add(labelOfHigh);
        jPanelOfCurrentKLineInfo.add(labelOfHighValue);
        jPanelOfCurrentKLineInfo.add(labelOfLow);
        jPanelOfCurrentKLineInfo.add(labelOfLowValue);
        jPanelOfCurrentKLineInfo.add(labelOfClose);
        jPanelOfCurrentKLineInfo.add(labelOfCloseValue);
        jPanelOfCurrentKLineInfo.add(labelOfChgPct);
        jPanelOfCurrentKLineInfo.add(labelOfChgPctValue);
        jPanelOfCurrentKLineInfo.add(labelOfAmplitude);
        jPanelOfCurrentKLineInfo.add(labelOfAmplitudeValue);
        jPanelOfCurrentKLineInfo.add(labelOfAmount);
        jPanelOfCurrentKLineInfo.add(labelOfAmountValue);
        jPanelOfCurrentKLineInfo.add(labelOfTurnover);
        jPanelOfCurrentKLineInfo.add(labelOfTurnoverValue);

        jPanelOfCurrentKLineInfo.setBorder(BorderFactory.createLineBorder(Color.red, 1));
        jPanelOfCurrentKLineInfo.setBackground(Color.black);

        this.add(jPanelOfCurrentKLineInfo, BorderLayout.EAST);
        this.setBackground(Color.black);
    }


    public static JLabel getCommonLabel(Color foreGroundColor) {
        return getCommonLabel("", foreGroundColor);
    }

    public static JLabel getCommonLabel(String text, Color foreGroundColor) {
        JLabel jlabel = new JLabel();
        jlabel.setText(text);
        jlabel.setHorizontalAlignment(SwingConstants.CENTER); // 居中对齐
        jlabel.setBackground(Color.black);
        jlabel.setForeground(foreGroundColor);
        return jlabel;
    }

    /**
     * 本质上是更新整个图表对象, 而非刷新图表对象
     *
     * @noti : 调用方负责实例化动态图表 的逻辑;
     */
    public void update(EmChartKLine.DynamicEmKLineChartForRevise dynamicKLineChart) {
        this.dynamicKLineChart = dynamicKLineChart; // 更新动态chart对象!
        this.update();
    }

    CrossLineListenerForKLineXYPlot crossLineListenerForKLineXYPlot0;

    /**
     * 本质上是更新整个图表对象, 而非刷新图表对象
     */
    @Override
    public void update() {
        if (chartPanel == null) {
            if (this.dynamicKLineChart != null && this.dynamicKLineChart.isInited()) { // 首次初始化
                chartPanel = new ChartPanel(this.dynamicKLineChart.getChart());
                // 大小
                chartPanel.setPreferredSize(new Dimension(4096, preferHeight));
                chartPanel.setMouseZoomable(false);
                chartPanel.setRangeZoomable(false);
                chartPanel.setDomainZoomable(false);
                crossLineListenerForKLineXYPlot0 =
                        EmChartKLine.getCrossLineListenerForKLineXYPlot(this.dynamicKLineChart.getAllDateTime());
                crossLineListenerForKLineXYPlot0.setXIndexChangeCallback(buildCrossLineXChangeCallback());
                chartPanel
                        .addChartMouseListener(
                                crossLineListenerForKLineXYPlot0);
                this.add(chartPanel, BorderLayout.CENTER);
                chartPanel.setVisible(true);
            } else {
                return;
            }
        }

        // 此后更新
        if (this.dynamicKLineChart != null && this.dynamicKLineChart.isInited()) {
            // 需要设置新的时间tick, 保证十字线正常!
            crossLineListenerForKLineXYPlot0.setTimeTicks(this.dynamicKLineChart.getAllDateTime());
            chartPanel.setChart(dynamicKLineChart.getChart());
        }
    }

    /**
     * 十字线x 索引改变回调
     *
     * @return
     */
    public CrossLineXIndexChangeCallback buildCrossLineXChangeCallback() {
        return new CrossLineXIndexChangeCallback() {
            @Override
            public void call(int newIndex) {
                if (dynamicKLineChart == null || !dynamicKLineChart.isInited()) {
                    return; // 需要动态图表已经初始化, 即有数据!
                }
                //         // 日期	   开盘	   收盘	   最高	   最低	    成交量	成交额	   振幅	   涨跌幅	   涨跌额	  换手率	  资产代码	资产名称
                DataFrame<Object> klineDfBeforeToday = dynamicKLineChart.getKlineDfBeforeToday();
                if (newIndex < klineDfBeforeToday.length() && newIndex > 0) { // 常态
                    // 9项数据
                    String date = StrUtil.replace(klineDfBeforeToday.get(newIndex, "日期").toString(), "-", "");
                    labelOfDateValue.setText(date);
                    Double preClose = Double.valueOf(klineDfBeforeToday.get(newIndex - 1, "收盘").toString());

                    Double open = Double.valueOf(klineDfBeforeToday.get(newIndex, "开盘").toString());
                    setTextColorByZero(labelOfOpenValue, open, preClose);
                    Double high = Double.valueOf(klineDfBeforeToday.get(newIndex, "最高").toString());
                    setTextColorByZero(labelOfHighValue, high, preClose);
                    Double low = Double.valueOf(klineDfBeforeToday.get(newIndex, "最低").toString());
                    setTextColorByZero(labelOfLowValue, low, preClose);
                    Double close = Double.valueOf(klineDfBeforeToday.get(newIndex, "收盘").toString());
                    setTextColorByZero(labelOfCloseValue, close, preClose);
                    Double chgPct = Double.valueOf(klineDfBeforeToday.get(newIndex, "涨跌幅").toString());
                    setTextColorByZero(labelOfChgPctValue, chgPct, 0.0);
                    labelOfChgPctValue.setText(labelOfChgPctValue.getText() + "%");

                    Double amount = Double.valueOf(klineDfBeforeToday.get(newIndex, "成交额").toString());
                    labelOfAmountValue.setText(CommonUtil.formatNumberWithSuitable(amount,1));
                    Double amplitude = Double.valueOf(klineDfBeforeToday.get(newIndex, "振幅").toString());
                    labelOfAmplitudeValue.setText(amplitude.toString() + "%");
                    Double turnover = Double.valueOf(klineDfBeforeToday.get(newIndex, "换手率").toString());
                    labelOfTurnoverValue.setText(turnover.toString() + "%");


                }
            }
        };
    }

    public static void setTextColorByZero(JLabel label, Double value, Double compareValue) {
        if (value == null) {
            label.setText("");
            return;
        }
        label.setText(value.toString());
        if (value > compareValue) {
            label.setForeground(upColor);
        } else if (value < compareValue) {
            label.setForeground(downColor);
        } else {
            label.setForeground(Color.white);
        }
    }
}
