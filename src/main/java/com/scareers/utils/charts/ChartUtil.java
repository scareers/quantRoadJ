package com.scareers.utils.charts;

import cn.hutool.core.lang.Console;
import joinery.DataFrame;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * description: 约定:给定df, 则 每一列, 视为同一y数据.! 即不同行可视为不同个体. 一列视为不同个体的相同属性
 * dataset.addValue(((Number) df.get(j, i)).doubleValue(), columns[i].toString(), indexes[j].toString());
 * 因此 第二参数为 列名称, 第三参数为行索引.
 * <p>
 * 目前实现折线图和条形图. 所有逻辑基本一模一样, 仅仅构造器唯一一个类型不一样
 *
 * @author: admin
 * @date: 2021/11/9  0009-20:37
 */
public class ChartUtil {
    private ChartUtil() {
    }

    public static void main(String[] args) throws IOException {
        DataFrame<Object> df = new DataFrame<>();
        df.add("a", Arrays.asList(1, 2, 3, 4));
        df.add("b", Arrays.asList(2, 3, 4, 5));
        df.add("c", Arrays.asList(2, 7, 4, 5));
        // 手动调用. 可调用不同的构造器
        File file = new File("c:/tempself/text.png");
        BarChartForDf chart = new BarChartForDf(df, file, null);
        chart.showIt();
        chart.saveIt();
        // 静态方法, 默认调用最简化构造器
//        dfAsLineChartSimple(df, false, null);
//        dfAsBarChartSimple(df, false, null);

    }

    public static void listOfDoubleAsBarChartSimple(List<Double> doubles, boolean save, File file,
                                                    List<Object> xUseColValues)
            throws IOException {
        DataFrame<Object> dataFrame = new DataFrame<>();
        dataFrame.add("temp_col", doubles);
        dataFrame.add("xLabels", xUseColValues);
        dfAsBarChartSimple(dataFrame, save, file, "xLabels");
    }

    public static void listOfDoubleAsLineChartSimple(List<Object> doubles, boolean save, File file,
                                                     List<Object> xUseColValues)
            throws IOException {
        DataFrame<Object> dataFrame = new DataFrame<>();
        dataFrame.add("temp_col", doubles);
        dataFrame.add("xLabels", xUseColValues);
        dfAsLineChartSimple(dataFrame, save, file, "xLabels");
    }

    public static void dfAsBarChartSimple(DataFrame<Object> df, boolean save, File file, String xUseCol)
            throws IOException {
        BarChartForDf chart = new BarChartForDf(df, file, xUseCol);
        chart.showIt();
        if (save) {
            chart.saveIt();
        }
    }

    public static void dfAsLineChartSimple(DataFrame<Object> df, boolean save, File file, String xUseCol)
            throws IOException {
        LineChartForDf chart = new LineChartForDf(df, file, xUseCol);
        chart.showIt();
        if (save) {
            chart.saveIt();
        }
    }

    /**
     * 折线图和条形图均使用此类数据集.
     *
     * @param df
     * @return
     */
    public static CategoryDataset createDefaultCategoryDataset(DataFrame<Object> df, String xUseCol) {
        if (df.length() == 0) {
            throw new IllegalArgumentException("df lenth 应该大于0");
        }
        df = df.convert();
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        Object[] columns = df.columns().toArray();
//
        ArrayList<Object> xs = new ArrayList<>();
        if (xUseCol == null) { // 未给定列作为 x轴各刻度时, 使用index
            Object[] indexes = df.index().toArray();
            for (Object index : indexes) {
                xs.add(index.toString());
            }
        } else {
            List<Object> xsCol = df.col(xUseCol);
            for (Object o : xsCol) {
                xs.add(o.toString());
            }
        }

        for (int i = 0; i < df.size(); i++) {
            if (!Number.class.isAssignableFrom(df.types().get(i))) {
                continue; // 不是数字跳过
            }
            List<Object> col = df.col(i);
            for (int j = 0; j < col.size(); j++) {
                dataset.addValue(((Number) df.get(j, i)).doubleValue(), columns[i].toString(), xs.get(j).toString());
            }
        }
        return dataset;
    }
}
