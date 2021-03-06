package com.scareers.utils.charts;

import lombok.Getter;
import lombok.Setter;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.PlotEntity;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;
import org.jfree.ui.LengthAdjustmentType;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.TextAnchor;

import java.awt.*;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.COLOR_CHART_CROSS_LINE_EM;
import static com.scareers.utils.charts.ChartUtil.decimalFormatForPercent;

/**
 * description: 十字交叉监听器. 常规要求 x/y 轴为数字. 十字线x/y均取百分比
 *
 * @author: admin
 * @date: 2022/2/24/024-17:39:46
 */
@Setter
@Getter
public class CrossLineListenerForSingleNumberXYPlot implements ChartMouseListener {
    @Override
    public void chartMouseClicked(ChartMouseEvent event) {
    }

    protected ValueMarkerS markerX;
    protected ValueMarkerS markerY;

    public CrossLineListenerForSingleNumberXYPlot() {
        BasicStroke basicStroke = new BasicStroke(1);

        markerX = new ValueMarkerS(Double.MIN_VALUE); // 水平线的值, 昨日收盘
        markerX.setType(ValueMarkerS.Type.MOUSE_CROSS_MARKER); // 标志类型
        markerX.setLabelOffsetType(LengthAdjustmentType.EXPAND);
        markerX.setPaint(COLOR_CHART_CROSS_LINE_EM); //线条颜色
        markerX.setStroke(basicStroke); //粗细
        markerX.setLabelFont(new Font("SansSerif", 0, 10)); //文本格式
        markerX.setLabelPaint(Color.red);
        setMarkerXLabelPosition(markerX);


        markerY = new ValueMarkerS(Double.MIN_VALUE); // 水平线的值, 昨日收盘
        markerY.setType(ValueMarkerS.Type.MOUSE_CROSS_MARKER);
        markerY.setLabelOffsetType(LengthAdjustmentType.EXPAND);
        markerY.setPaint(COLOR_CHART_CROSS_LINE_EM); //线条颜色

        markerY.setStroke(basicStroke); //粗细
        // markerY.setLabel(decimalFormatForPercent.format(markerValueY)); //线条上显示的文本
        markerY.setLabelFont(new Font("SansSerif", 0, 10)); //文本格式
        markerY.setLabelPaint(Color.red);
        markerY.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
        markerY.setLabelTextAnchor(TextAnchor.BOTTOM_RIGHT);

    }

    protected void setMarkerXLabelPosition(ValueMarkerS markerX) {
        markerX.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
        markerX.setLabelTextAnchor(TextAnchor.TOP_LEFT);
    }

    @Override
    public void chartMouseMoved(ChartMouseEvent event) {

        // 2.ChartPanel 对象
        ChartPanel chartPanel = (ChartPanel) event.getTrigger().getSource();
        // 3.事件发生的坐标, 对比屏幕中的数据区域, 所占百分比位置, 对应两个坐标轴值range百分比, 求出鼠标点对应的 x,y值
        // 3.1: 求鼠标当前位置, 对应的x值
        double cursorX = event.getTrigger().getX(); // 鼠标位置
        double minX = chartPanel.getScreenDataArea().getMinX();
        double maxX = chartPanel.getScreenDataArea().getMaxX(); // 图最大最小y

        double cursorY = event.getTrigger().getY(); // 鼠标位置
        double maxY = chartPanel.getScreenDataArea().getMaxY(); // 图最大最小y
        double minY = chartPanel.getScreenDataArea().getMinY();
        XYPlot plot = (XYPlot) event.getChart().getPlot();

        // 去掉了==, 范围更小, 更灵敏一点
        if (!(minX <= cursorX && cursorX <= maxX) || !(minY <= cursorY && cursorY <= maxY)) {
            plot.removeDomainMarker(markerX);
            plot.removeRangeMarker(markerY);
            return; // 超过范围则移除十字星
        }

        // 1.只监听 XYPlot 上的鼠标移动, 其他类型无视; 获取 xyplot对象
        if (event.getEntity() == null) {
            return;
        }
        if (!(event.getEntity() instanceof PlotEntity)) {
            return;
        }
        if (!(event.getChart().getPlot() instanceof XYPlot)) {
            return;
        }


        double percentX = (maxX - cursorX) / (maxX - minX); // 从下到上部分百分比, 后面计算 value Range同百分比的x值即可
        ValueAxis domainAxis = plot.getDomainAxis();
        Range rangeX = domainAxis.getRange();
        Double markerValueX = rangeX.getUpperBound() - rangeX.getLength() * percentX; // 同百分比取得marker位置

        // 3.2: 删除所有DomainMarkers, 新建对应x值得 Marker并设置. 可得到十字竖线
        plot.removeDomainMarker(markerX);
        markerX.setValue(markerValueX);
        markerX.setLabel(getMarkerXLabel(markerValueX));
        plot.addDomainMarker(markerX);

        // 3.3: 同理, 求出鼠标对应y值
        double percentY = (maxY - cursorY) / (maxY - minY); // 从下到上部分百分比, 后面计算 value Range同百分比的y值即可
        ValueAxis rangeAxis = plot.getRangeAxis();
        Range range = rangeAxis.getRange();
        Double markerValueY = range.getLowerBound() + range.getLength() * percentY; // 同百分比取得marker位置
        // 3.4: 同理, 创建y值 横向marker
        plot.removeRangeMarker(markerY);
        markerY.setValue(markerValueY);
        markerY.setLabel(getMarkerYLabel(markerValueY));
        plot.addRangeMarker(markerY);


    }

    String getMarkerYLabel(Double markerValueY) {
        return decimalFormatForPercent.format(markerValueY);
    }

    protected String getMarkerXLabel(Double markerValueX) {
        return decimalFormatForPercent.format(markerValueX);
    }
}
