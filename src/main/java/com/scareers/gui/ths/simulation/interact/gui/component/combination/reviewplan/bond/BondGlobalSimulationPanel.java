package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.bond;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.BondUtil;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.SecurityBeanEm.SecurityEmPo;
import com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi;
import com.scareers.gui.ths.simulation.interact.gui.SmartFindDialog;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.MainDisplayWindow;
import com.scareers.gui.ths.simulation.interact.gui.component.simple.DateTimePicker;
import com.scareers.gui.ths.simulation.interact.gui.component.simple.FuncButton;
import com.scareers.gui.ths.simulation.interact.gui.component.simple.JXFindBarS;
import com.scareers.gui.ths.simulation.interact.gui.factory.ButtonFactory;
import com.scareers.gui.ths.simulation.interact.gui.model.DefaultListModelS;
import com.scareers.gui.ths.simulation.interact.gui.notify.BondBuyNotify;
import com.scareers.gui.ths.simulation.interact.gui.ui.BasicScrollBarUIS;
import com.scareers.gui.ths.simulation.interact.gui.ui.renderer.SecurityEmListCellRendererS;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.sqlapi.EastMoneyDbApi;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.charts.CrossLineListenerForFsXYPlot;
import com.scareers.utils.charts.EmChart;
import com.scareers.utils.charts.EmChart.DynamicEmFs1MV2ChartForRevise;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jdesktop.swingx.JXList;
import org.jdesktop.swingx.JXTable;
import org.jfree.chart.ChartPanel;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.*;

/**
 * description: 转债全市场(全局)复盘panel;
 * 主展示页面, 左转债列表, 带搜索栏; 右为动态分时图 -- 使用东财数据库数据实现的仿真
 *
 * @author: admin
 * @date: 2022/2/12/012-12:56:23
 */
@Getter
public class BondGlobalSimulationPanel extends JPanel {
    private static BondGlobalSimulationPanel INSTANCE;

    public static BondGlobalSimulationPanel getInstance(MainDisplayWindow mainDisplayWindow, int jListWidth) {
        if (INSTANCE == null) {
            INSTANCE = new BondGlobalSimulationPanel(mainDisplayWindow, jListWidth);
        }
        return INSTANCE;
    }

    public static BondGlobalSimulationPanel getInstance() {
        return INSTANCE;
    }

    public static final int tick3sLogPanelWidth = DynamicEmFs1MV2ChartForRevise.tickLogPanelWidthDefault; // 3stick数据显示组件宽度
    public static final double timeRateDefault = 3.0; // 默认复盘时间倍率
    // 转债全列表, 是否使用问财实时列表; 若不, 则使用数据库对应日期列表; @noti: 目前问财的成交额排名, 似乎有bug, 无法排名正确
    public static final boolean bondListUseRealTimeWenCai = true;
    public static final boolean loadAllFsDataFromDbWhenFlushBondList = true; // @key: 更新转债列表显示时, 是否载入所有fs数据

    protected volatile Vector<SecurityBeanEm.SecurityEmPo> securityEmPos = new Vector<>(); // 转债列表对象
    protected volatile JXTable jXTableForBonds; //  转债展示列表控件
    protected SecurityBeanEm selectedBean = null; // 被选中的转债 东财bean对象
    protected SecurityBeanEm preChangedSelectedBean = null; // 此前被选中,且更新过fs图对象, 当新的等于它时, 将不重新实例化动态图表对象
    protected int jListWidth; // 列表宽度, 例如300
    protected MainDisplayWindow mainDisplayWindow; // 主显示区

    JPanel panelLeft; // 左panel, 显示列表和搜索等. 列表在下, 各种功能按钮组在上!
    JPanel panelMainForRevise; // 主要的复盘区域panel, 在右

    @SneakyThrows
    public static void main(String[] args) {
        DataFrame<Object> res = getReviseTimeBondListOverviewDataDf(
                SecurityBeanEm.createBondList(Arrays.asList("小康转债", "盘龙转债"), true),
                "2022-06-07",
                "10:00:00");
        Console.log(res);
    }

    protected BondGlobalSimulationPanel(MainDisplayWindow mainDisplayWindow, int jListWidth) {
        // 异步开始等待某些状态, 并一次或者持续刷新股票列表
        this.jListWidth = jListWidth;
        this.mainDisplayWindow = mainDisplayWindow;
        this.setLayout(new BorderLayout()); // border布局, 列表在左, 其余在右; 总宽度为展示区; 列表固定宽

        // 1.左panel 初始化和组装
        buildLeftPanel();
        this.add(panelLeft, BorderLayout.WEST); // 左

        // 2. 右panel
        buildMainPanel();
        this.add(panelMainForRevise, BorderLayout.CENTER); // 中

        // 3.主 展示窗口 添加尺寸改变监听. 改变 jList 和 orderContent尺寸.
        this.mainDisplayWindow.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                panelLeft.setBounds(0, 0, jListWidth, getHeight()); // 固定宽默认 300
                panelMainForRevise.setBounds(panelLeft.getWidth(), 0,
                        getWidth() - panelLeft.getWidth()
                        , getHeight());
                panelMainForRevise.repaint();
            }
        });

        // 4.需要异步调用 BondUtil, 初始化 转债-- 正股/指数 两大map, 方便查询, 以调用 自定义动态图表类 的构造器!!
        ThreadUtil.execAsync(new Runnable() {
            @Override
            public void run() {
                while (!BondUtil.flushBondToStockAndIndexMap()) { // 自动刷新, 只要填充超过200, 就视为成功
                    ThreadUtil.sleep(5000); // 间隔5s
                }
            }
        }, true);


    }

    /**
     * 复盘期间, 给定转债列表, 给定 日期和时间, 生成 转债"实时"数据截面列表df, 被table展示
     * 模拟实盘下, 点击全债券列表的展示页面, 对整个市场有个宏观展示
     * 当前仅展示 涨幅(很好计算) 和 当前总成交额(需要分时成交求和, 计算量稍大)
     *
     * @param bondList
     * @return
     */
    public static DataFrame<Object> getReviseTimeBondListOverviewDataDf(List<SecurityBeanEm> bondList, String date,
                                                                        String timeTick) {
        HashMap<SecurityBeanEm, Double> chgPctRealTime = new HashMap<>(); // 涨跌幅
        HashMap<SecurityBeanEm, Double> amountRealTime = new HashMap<>(); // 成交额
        for (SecurityBeanEm bondBean : bondList) {
            // 1.昨收, 计算涨跌幅
            Double preClose = EastMoneyDbApi.getPreCloseOf(date, bondBean.getQuoteId());
            if (preClose == null) {
                continue;
            }
            // 2.分时成交
            DataFrame<Object> fsTransDf = EastMoneyDbApi
                    .getFsTransByDateAndQuoteIdS(date, bondBean.getQuoteId(), false);
            if (fsTransDf == null) {
                continue;
            }
            // 3. 筛选有效分时成交! time_tick 列
            int shouldIndex = -1;
            for (int i = 0; i < fsTransDf.size(); i++) {
                String timeTick1 = fsTransDf.get(i, "time_tick").toString();
                if (timeTick1.compareTo(timeTick) <= 0) {
                    shouldIndex = i; // 找到截断索引
                } else {
                    break;
                }
            }

            if (shouldIndex == -1) {
                continue; // 筛选不到
            }
            DataFrame<Object> effectDf = fsTransDf.slice(0, Math.min(shouldIndex + 1, fsTransDf.length()));

            // 4.涨跌幅很好计算
            Double newestPrice = Double.valueOf(effectDf.get(effectDf.length() - 1, "price").toString());
            chgPctRealTime.put(bondBean, newestPrice / preClose - 1);

            // 5.总计成交额, 需要强行计算! price 和 vol 列, vol手数 需要转换为 张数, *10
            int volRate = bondBean.isBond() ? 10 : 100;
            List<Double> tickAmountList = new ArrayList<>();
            for (int i = 0; i < effectDf.length(); i++) {
                Object price = effectDf.get(i, "price");
                Object vol = effectDf.get(i, "vol");
                tickAmountList.add(Double.parseDouble(price.toString()) * Double.parseDouble(vol.toString()) * volRate);
            }
            amountRealTime.put(bondBean, CommonUtil.sumOfListNumberUseLoop(tickAmountList));
        }

        // 1.构建结果df! 列简单: 代码,名称, 涨跌幅, 当前总成交额!
        DataFrame<Object> res = new DataFrame<Object>(Arrays.asList("代码", "名称", "涨跌幅", "成交额"));
        for (SecurityBeanEm beanEm : bondList) {
            List<Object> row = new ArrayList<>();
            row.add(beanEm.getSecCode());
            row.add(beanEm.getName());
            row.add(chgPctRealTime.get(beanEm));
            row.add(amountRealTime.get(beanEm)); // 涨跌幅成交额都可能是 null, 但保证需要所有转债;
            res.append(row);
        }

        // 2. 无需排序, 自行使用 JXTable 的排序功能! 但转换为数字排序, 是需要重新一下排序逻辑的, 默认按照字符串排序
        return res;
    }

    DynamicEmFs1MV2ChartForRevise dynamicChart; // 随时更新对象
    ChartPanel chartPanel; // 更新时: 仅需要更新 内部chart对象;
    JPanel panelOfTick3sLog; // 更新时: 仅需将新 dynamicChart 的log组件, add到其center即可
    CrossLineListenerForFsXYPlot crossLineListenerForFsXYPlot; // 监听器, 更新时, 需要更新其时间列表,否则可能出现问题

    private boolean firstAddLogPanel = true; // 首次添加log到右panel

    /**
     * 更新分时图显示 主 区; 它读取自身属性, selectedBean, 以及设置区设置的 日期 ! 实例化 DynamicEmFs1MV2ChartForRevise 对象
     * 它要求 selectedBean 已设置不为 null;
     */

    public void updateFsDisplay(boolean forceCreateDynamicChart) {
        if (selectedBean == null) {
            return; // 为空或者未改变, 不会重新实例化 动态分时图表 对象
        }
        // 可强制重新创建 DynamicEmFs1MV2ChartForRevise 对象, 将读取新的selectedBean和 日期设置
        if (!forceCreateDynamicChart) {
            // 不强制时, 才使用对比机制, 可能无需新建对象; 但同样 DateStr 不会修改; 在开始和重启时, 显然需要重新读取
            if (this.selectedBean.equals(this.preChangedSelectedBean)) {
                return;
            }
        }
        // 1.实例化动态图表 -- 实例化最消耗时间
        String reviseDateStrSettingYMD = getReviseDateStrSettingYMD();
        // @key: 当前优化到 1-2 ms 级别
        SecurityBeanEm stock = BondUtil.getStockBeanByBond(selectedBean);
        SecurityBeanEm index = BondUtil.getIndexBeanByBond(selectedBean);
        if (stock == null || index == null) {
            // CommonUtil.notifyError("转债对应的 正股/指数 bean为null, 尝试访问网络直接获取");
            try {
                stock = SecurityBeanEm.createStock(BondUtil.getStockCodeOfBond(selectedBean), true);
                index = selectedBean.getSecCode().startsWith("11") ? SecurityBeanEm
                        .getShangZhengZhiShu() : SecurityBeanEm.getShenZhengChengZhi();
            } catch (Exception e) {

            }
        }
        if (stock == null || index == null) {
            CommonUtil.notifyError("转债对应的 正股/指数 bean为null, 可尝试刷新 债股映射后稍等重试; 返回");
            return;
        }
        try {
            dynamicChart = new DynamicEmFs1MV2ChartForRevise(selectedBean, reviseDateStrSettingYMD, index, stock);
        } catch (Exception e) {
            e.printStackTrace();
            CommonUtil.notifyError("实例化动态图表对象失败, 请检查构造器参数");
            return;
        }
        preChangedSelectedBean = this.selectedBean; // 更新了图表对象时, 才更新

        // 3. 更新chart对象, 刷新! // 后面常态逻辑也优化到了 0-1毫秒级别
        crossLineListenerForFsXYPlot.setTimeTicks(dynamicChart.getAllFsTimeTicks()); // 保证十字线正常
        chartPanel.setChart(dynamicChart.getChart());
        chartPanel.repaint();
        chartPanel.updateUI();
        if (firstAddLogPanel) {
            try {
                panelOfTick3sLog.removeAll(); // 需要删除才能保证只有一个
                JScrollPane jScrollPaneForTickLog = DynamicEmFs1MV2ChartForRevise.getJScrollPaneForTickLog();
                panelOfTick3sLog.setPreferredSize(new Dimension(tick3sLogPanelWidth, panelMainForRevise.getHeight()));
                jScrollPaneForTickLog
                        .setPreferredSize(new Dimension(tick3sLogPanelWidth, panelOfTick3sLog.getHeight())); // 容器同宽
                jScrollPaneForTickLog.setLocation(0, 0);
                jScrollPaneForTickLog.setBorder(null);
                panelOfTick3sLog.add(jScrollPaneForTickLog, BorderLayout.CENTER);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            firstAddLogPanel = false; // 首次添加panel
        }
    }


    JPanel functionContainerMain;

    /**
     * 主panel -- 对控制复盘的按钮, 还是应当放在本panel 最上方, 以便控制
     */
    private void buildMainPanel() {
        panelMainForRevise = new JPanel();
        panelMainForRevise.setLayout(new BorderLayout());

        // 1.复盘,分时图,等相关功能区
        functionContainerMain = new JPanel();
        functionContainerMain.setLayout(new FlowLayout(FlowLayout.LEFT));
        functionContainerMain.setPreferredSize(new Dimension(2048, 40));
        functionContainerMain.setBorder(BorderFactory.createLineBorder(Color.black, 1));
        functionContainerMain.setBackground(Color.black);
        addMainFunctions(); // 主功能区按钮

        // 2.分时图(或未来k线)显示区
        JPanel fsMainPanel = new JPanel(); // fs和tick容器
        fsMainPanel.setLayout(new BorderLayout());

        chartPanel = new ChartPanel(null); // 图表
        chartPanel.setPreferredSize(new Dimension(1200, 800));
        chartPanel.setMouseZoomable(false);
        chartPanel.setRangeZoomable(false);
        chartPanel.setDomainZoomable(false);
        chartPanel.setMouseZoomable(false, false);
        chartPanel.setMouseWheelEnabled(false);
        crossLineListenerForFsXYPlot =
                EmChart.getCrossLineListenerForFsXYPlot(CommonUtil.generateMarketOpenTimeListHm(false));
        chartPanel // 注意, 必须要求 东财1分钟分时图, 241 行; 即使用 v2 版本的东财api; 同同花顺默认;但更新chart时应当刷新
                .addChartMouseListener(crossLineListenerForFsXYPlot);
        panelOfTick3sLog = new JPanel();  // tick显示
        panelOfTick3sLog.setLayout(new BorderLayout());
        JLabel tempLabel = new JLabel("暂无数据");
        tempLabel.setPreferredSize(new Dimension(tick3sLogPanelWidth, 1024));
        tempLabel.setBackground(Color.black);
        tempLabel.setForeground(Color.red);
        panelOfTick3sLog.add(tempLabel, BorderLayout.CENTER);

        // 2.1. 加入两大组件
        chartPanel.setBackground(Color.black);
        panelOfTick3sLog.setBackground(Color.black);
        fsMainPanel.setBackground(Color.black);
        fsMainPanel.add(chartPanel, BorderLayout.CENTER);
        fsMainPanel.add(panelOfTick3sLog, BorderLayout.EAST);


        // 3.组装
        panelMainForRevise.add(fsMainPanel, BorderLayout.CENTER);
        panelMainForRevise.add(functionContainerMain, BorderLayout.NORTH);
    }

    // 主功能区相关 -- 主要实现 虚拟时间, 以便复盘, 以及开始,暂停,重置,复盘起始时间设定等功能; 达成仿真
    JTextField jTextFieldOfReviseStartDatetime; // 设置显示复盘开始日期和时间! 时间选择器
    DateTimePicker dateTimePickerOfReviseStartDatetime; // 与jTextFieldOfReviseStartDatetime结合达成时间选择功能
    DateTime reviseStartDatetime; // 默认的复盘开始时间
    // 单秒全序列: 复盘过程中, 可能出现的所有虚拟 时刻.复盘开始后, 遍历此序列, 选择第一个不小于reviseStartDatetime的开始;仅仅时分秒有效
    List<String> allFsTransTimeTicks; // 仅仅包含时分秒的标准时间
    JLabel labelOfRealTimeSimulationTime; // 仿真的 "实时时间"; 只显示时分秒, 年月日从reviseStartDatetime去看
    JTextField jTextFieldOfTimeRate; // 复盘时, 时间流速倍率, 无视了程序执行时间, 仅仅控制 sleep 的时间! 将解析text为double

    private volatile boolean reviseRunning = false; // 标志复盘是否进行中, 将其手动设置为false, 可以停止进行中的循环
    // 该值为true时, 点击重启才有效;
    private volatile boolean revisePausing = false; // 标志复盘是否暂停中; 当暂停时, 理论上running应当为 true

    // 功能按钮
    FuncButton startReviseButton; // 开始按钮
    FuncButton stopReviseButton; // 停止按钮
    FuncButton pauseRebootReviseButton; // 暂停和重启按钮, 将自行变换状态; 检测 自身text 判定应当执行的功能!


    /**
     * 主功能区组件添加, 添加到 functionContainerMain, 该panel为左浮动布局
     */
    private void addMainFunctions() {
        // 1. 初始化复盘功能 相关属性
        // 1.0. 默认的复盘开始时间: >16点,今天是交易日,就今天, 否则都上一天! 给爬虫一个小时!
        try {
            if (DateUtil.hour(DateUtil.date(), true) >= 16) {
                if (EastMoneyDbApi.isTradeDate(DateUtil.today())) {
                    reviseStartDatetime = DateUtil.parse(DateUtil.today() + " 09:30:00");
                } else {
                    reviseStartDatetime = DateUtil
                            .parse(EastMoneyDbApi.getPreNTradeDateStrict(DateUtil.today(), 1) + " " +
                                    "09:30:00");
                }
            } else {
                reviseStartDatetime = DateUtil
                        .parse(EastMoneyDbApi.getPreNTradeDateStrict(DateUtil.today(), 1) + " " +
                                "09:30:00");
            }
        } catch (SQLException e) {
            reviseStartDatetime = DateUtil.parse(DateUtil.today() + " 09:30:00");
        }
        // 1.1. 复盘开始时间 时间选择器!
        jTextFieldOfReviseStartDatetime = new JTextField("复盘开始时间");
        jTextFieldOfReviseStartDatetime.setBorder(null);
        jTextFieldOfReviseStartDatetime.setBackground(Color.black);
        jTextFieldOfReviseStartDatetime.setForeground(Color.yellow);
        jTextFieldOfReviseStartDatetime.setCaretColor(Color.red);
        jTextFieldOfReviseStartDatetime.setPreferredSize(new Dimension(130, 40));

        dateTimePickerOfReviseStartDatetime = new DateTimePicker("yyyy-MM-dd HH:mm:ss", 160, 200);
        dateTimePickerOfReviseStartDatetime.setEnable(true).setSelect(reviseStartDatetime) // 默认值
                .changeDateEvent(new Consumer<DateTimePicker>() {
                    @Override
                    public void accept(DateTimePicker o) { // 选择后回调, 它仅仅会自动修改注册组件的文字, 以及内部date
                        // 也修改静态属性!
                        reviseStartDatetime = DateUtil.parse(dateTimePickerOfReviseStartDatetime.getSelect());
                    }
                }).register(jTextFieldOfReviseStartDatetime);

        // 1.2. 所有可能的时间. 时分秒
        allFsTransTimeTicks = CommonUtil.generateMarketOpenTimeStrListHms(false);


        // 1.3. 仿真 实时时间显示label! 不可编辑,固定 HH:mm:ss 格式
        labelOfRealTimeSimulationTime = new JLabel();
        labelOfRealTimeSimulationTime.setForeground(Color.green);
        labelOfRealTimeSimulationTime.setPreferredSize(new Dimension(60, 40));
        labelOfRealTimeSimulationTime.setBackground(Color.black);
        labelOfRealTimeSimulationTime.setText("00:00:00"); // 初始!

        // 1.4. 时间流速倍率, 默认 1.0
        jTextFieldOfTimeRate = new JTextField(String.valueOf(timeRateDefault));
        jTextFieldOfTimeRate.setBackground(Color.black);
        jTextFieldOfTimeRate.setBorder(null);
        jTextFieldOfTimeRate.setForeground(Color.yellow);
        jTextFieldOfTimeRate.setPreferredSize(new Dimension(35, 40));
        jTextFieldOfTimeRate.setCaretColor(Color.red);

        // 2.主功能区!
        // 2.1. 时间选择器, 操作可绝对开始时间 reviseStartDatetime;
        // 2.2. 静态仿真实时时间显示label
        // 2.3. 开始复盘按钮: 开始复盘,读取reviseStartDatetime设置; 若当前正在运行, 则先停止再直接运行!
        startReviseButton = ButtonFactory.getButton("开始"); //
        startReviseButton.setForeground(Color.yellow);
        startReviseButton.addActionListener(new ActionListener() {
            @Override
            public synchronized void actionPerformed(ActionEvent e) { // 同步
                if (reviseRunning) { // 正在运行中, 则点击停止按钮, 并且等待 flag, 真正停止下来
                    CommonUtil.notifyError("复盘进行中, 停止后才可开始!");
                    return;
                }

                if (revisePausing) {
                    CommonUtil.notifyError("复盘暂停中, 请点击重启!");
                    return;
                }


                // 此时 reviseRunning 必然为 false, 正式执行 -- 开始复盘
                // @key3: 复盘逻辑:
                // 1.更新对象! 将读取 年月日 日期设定; 且强制更新,使用新日期设置
                updateFsDisplay(true);

                // 2.读取时间流速设定, -- @update: 已经改为实时读取
                // double timeRate = getReviseTimeRateSetting();

                // 3.读取开始的 时分秒 tick 设置!
                String startTickHms = getReviseDateStrSettingHMS();

                // 4.在 allFsTransTimeTicks 所有时间tick中, 筛选>= 开始tick的子列表, 以便遍历!
                int startIndex = allFsTransTimeTicks.indexOf(startTickHms);
                if (startIndex == -1) {
                    startIndex = 0; // 当没有找到, 则从0开始!
                }

                // 5.开始循环遍历 tick, 执行更新! 期间将 检测 pause flag, 有可能被暂停!
                // @noti: 暂停机制, 本质上也是break了循环 而停止; 只是保留 labelOfRealTimeSimulationTime 值, 以便重启!
                // @noti: 停止机制, 则 会将 labelOfRealTimeSimulationTime 设置为 00:00:00, 不可重启!
                int finalStartIndex = startIndex;
                ThreadUtil.execAsync(new Runnable() {
                    @Override
                    public void run() {
                        if (dynamicChart == null) {
                            return;
                        }

                        // 死循环开始执行!
                        // 0.真正逻辑上开始, 设置 flag
                        reviseRunning = true;
                        revisePausing = false;
                        CommonUtil.notifyKey("复盘即将开始");
                        for (int i = finalStartIndex; i < allFsTransTimeTicks.size(); i++) {
                            if (!reviseRunning) { // 被停止
                                labelOfRealTimeSimulationTime.setText("00:00:00");
                                reviseRunning = false; // 保证有效
                                CommonUtil.notifyCommon("复盘已停止");
                                break; // 被停止, 则立即停止循环!
                            }

                            if (revisePausing) { // 被暂停
                                // labelOfRealTimeSimulationTime.setText("00:00:00"); // 实时时间得以保留!
                                revisePausing = true; // 保证有效
                                CommonUtil.notifyCommon("复盘已暂停");
                                break; // 被停止也终止循环, 等待重启!
                            }

                            String tick = allFsTransTimeTicks.get(i);
                            labelOfRealTimeSimulationTime.setText(tick); // 更新tick显示label
                            dynamicChart.updateChartFsTrans(DateUtil.parse(tick)); // 重绘图表

                            ThreadUtil.sleep((long) (1000 / getReviseTimeRateSetting())); // 因为循环是1s的;
                        }
                        reviseRunning = false; // 非运行状态
                    }
                }, true);
            }
        });

        // 2.4. 停止按钮
        stopReviseButton = ButtonFactory.getButton("停止");
        stopReviseButton.setForeground(Color.RED);
        stopReviseButton.addActionListener(new ActionListener() {
            @Override
            public synchronized void actionPerformed(ActionEvent e) {
                labelOfRealTimeSimulationTime.setText("00:00:00");
                reviseRunning = false; // 将停止 start后 的线程中的循环
                revisePausing = false; // 暂停flag也将恢复!
                pauseRebootReviseButton.setText("暂停"); // 强制暂停按钮恢复暂停状态
            }
        });

        // 2.5. 暂停按钮
        pauseRebootReviseButton = ButtonFactory.getButton("暂停"); // 默认暂停!
        pauseRebootReviseButton.setForeground(Color.pink);
        pauseRebootReviseButton.addActionListener(new ActionListener() {
            @Override
            public synchronized void actionPerformed(ActionEvent e) {
                String text = pauseRebootReviseButton.getText();
                if ("暂停".equals(text)) {// 执行暂停功能
                    if (!reviseRunning) {
                        CommonUtil.notifyError("复盘尚未进行, 不可暂停!");
                        return;
                    }

                    revisePausing = true; // 暂停, 正在执行的将停止, 但保留进度
                    try {
                        CommonUtil.waitUtil(new BooleanSupplier() {
                            @Override
                            public boolean getAsBoolean() {
                                return reviseRunning == false; // 等待确实停止了下来
                            }
                        }, Integer.MAX_VALUE, 1, null, false);
                    } catch (TimeoutException | InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    pauseRebootReviseButton.setText("重启"); // 变换状态!
                } else if ("重启".equals(text)) { // 执行重启功能! 它与开始功能的差别在于, 开始tick从 label读取, 而非设置读取
                    if (reviseRunning) { // 正在运行中, 不可重启
                        CommonUtil.notifyError("复盘进行中, 停止后才可重启!");
                        return;
                    }
                    if (!revisePausing) {
                        CommonUtil.notifyError("复盘未暂停, 不可重启!");
                        return;
                    }

                    // 此时 reviseRunning 必然为 false, 且revisePausing 为true *************
                    // @key3: 复盘逻辑:
                    // 1.更新对象! 将读取 年月日 日期设定; 且强制更新,使用新日期设置
                    updateFsDisplay(true);
                    // 2.读取时间流速设定, -- @update: 已经改为实时读取
                    // double timeRate = getReviseTimeRateSetting();
                    // 3.读取开始的 时分秒 tick 设置!
                    String startTickHms = getReviseRestartTickFromLabel();

                    // 4.在 allFsTransTimeTicks 所有时间tick中, 筛选>= 开始tick的子列表, 以便遍历!
                    int startIndex = allFsTransTimeTicks.indexOf(startTickHms);
                    if (startIndex == -1) {
                        startIndex = 0; // 当没有找到, 则从0开始!
                    }

                    // 5.开始循环遍历 tick, 执行更新! 期间将 检测 pause flag, 有可能被暂停!
                    // @noti: 暂停机制, 本质上也是break了循环 而停止; 只是保留 labelOfRealTimeSimulationTime 值, 以便重启!
                    // @noti: 停止机制, 则 会将 labelOfRealTimeSimulationTime 设置为 00:00:00, 不可重启!
                    int finalStartIndex = startIndex;
                    ThreadUtil.execAsync(new Runnable() {
                        @Override
                        public void run() {
                            if (dynamicChart == null) {
                                return;
                            }

                            // 死循环开始执行!
                            // 0.真正逻辑上开始, 设置 flag
                            reviseRunning = true;
                            revisePausing = false; // 重设暂停flag
                            pauseRebootReviseButton.setText("暂停"); // 变换状态!
                            CommonUtil.notifyKey("复盘即将重启");
                            for (int i = finalStartIndex; i < allFsTransTimeTicks.size(); i++) {
                                if (!reviseRunning) { // 被停止
                                    labelOfRealTimeSimulationTime.setText("00:00:00");
                                    reviseRunning = false; // 保证有效
                                    CommonUtil.notifyCommon("复盘已停止");
                                    break; // 被停止, 则立即停止循环!
                                }

                                if (revisePausing) { // 被暂停
                                    // labelOfRealTimeSimulationTime.setText("00:00:00"); // 实时时间得以保留!
                                    revisePausing = true; // 保证有效
                                    CommonUtil.notifyCommon("复盘已暂停");
                                    break; // 被停止也终止循环, 等待重启!
                                }

                                String tick = allFsTransTimeTicks.get(i);
                                labelOfRealTimeSimulationTime.setText(tick); // 更新tick显示label
                                dynamicChart.updateChartFsTrans(DateUtil.parse(tick)); // 重绘图表

                                ThreadUtil.sleep((long) (1000 / getReviseTimeRateSetting())); // 因为循环是1s的;
                            }
                            reviseRunning = false; // 非运行状态
                        }
                    }, true);

                }// 其他不执行, 一般不可能
            }
        });

        // 3.全部组件添加
        functionContainerMain.add(jTextFieldOfReviseStartDatetime);
        functionContainerMain.add(labelOfRealTimeSimulationTime);
        functionContainerMain.add(jTextFieldOfTimeRate);
        functionContainerMain.add(startReviseButton);
        functionContainerMain.add(stopReviseButton);
        functionContainerMain.add(pauseRebootReviseButton);


    }

    public String getReviseDateStrSettingYMD() { // 复盘日期设定 -- 年月日
        try {
            return DateUtil.format(DateUtil.parse(jTextFieldOfReviseStartDatetime.getText()),
                    DatePattern.NORM_DATE_PATTERN); // 读取最新设定的 年月日 日期
        } catch (Exception e) {
            CommonUtil.notifyError("复盘程序读取 复盘日期失败, 默认返回今日");
            return DateUtil.today();
        }
    }

    public String getReviseDateStrSettingHMS() { // 复盘开始tick设定 -- 时分秒
        try {
            return DateUtil.format(DateUtil.parse(jTextFieldOfReviseStartDatetime.getText()),
                    DatePattern.NORM_TIME_PATTERN); // 读取最新设定的 年月日 日期
        } catch (Exception e) {
            CommonUtil.notifyError("复盘程序读取 复盘开始tick失败, 默认返回 09:30:00");
            return "09:30:00";
        }
    }

    public String getReviseDateStrSettingYMDHMS() { // 复盘开始tick设定 -- 年月日 时分秒
        try {
            return DateUtil.format(DateUtil.parse(jTextFieldOfReviseStartDatetime.getText()),
                    DatePattern.NORM_DATETIME_PATTERN); // 读取最新设定的 年月日 日期
        } catch (Exception e) {
            CommonUtil.notifyError("复盘程序读取 复盘开始tick失败, 默认返回 今日 09:30:00");
            return DateUtil.today() + " 09:30:00";
        }
    }

    public String getReviseRestartTickFromLabel() { // 重启复盘时, 应当从label读取tick, 失败则 9:30:00
        try {
            return DateUtil.format(DateUtil.parse(labelOfRealTimeSimulationTime.getText()),
                    DatePattern.NORM_TIME_PATTERN); // 读取当前被暂停时label的时间
        } catch (Exception e) {
            CommonUtil.notifyError("复盘程序读取 复盘暂停重启tick失败, 默认返回 09:30:00");
            return "09:30:00";
        }
    }

    public DateTime getReviseSimulationCurrentTime() { // 实时获取复盘 虚拟的 当前时间!
        try {
            return DateUtil.parse(labelOfRealTimeSimulationTime.getText()); // 读取当前label的时间
        } catch (Exception e) {
            CommonUtil.notifyError("复盘程序读取 虚拟当前时间失败,返回此刻但无逻辑意义");
            return DateUtil.date();
        }
    }

    public double getReviseTimeRateSetting() { // 复盘时间流速倍率, 错误将返回 1.0
        double v = timeRateDefault;
        try {
            v = Double.parseDouble(jTextFieldOfTimeRate.getText());
        } catch (Exception e) {
            CommonUtil.notifyError("复盘程序读取 时间流速倍率失败, 默认返回 " + timeRateDefault);
        }
        if (v <= 0.1) { // 倍率不能太小; 显然也不能为0
            return timeRateDefault;
        }
        return v;
    }

    JPanel functionPanel; // 功能按钮区 在左上
    JXFindBarS jxFindBarS; // 常规的查找转债列表 -- 查找控件

    /**
     * 组装左panel
     * 上功能区, 下列表区!
     *
     * @return
     */
    private void buildLeftPanel() {
        panelLeft = new JPanel();
        panelLeft.setLayout(new BorderLayout());
        panelLeft.setPreferredSize(new Dimension(jListWidth, 2048));

        // 1.上功能区
        initFunctionPanel();

        // 2.转债列表
        jXTableForBonds = getSecurityEmJXTable(); // 已经实现自动读取并刷新 securityEmPos 属性
        initJListWrappedJScrollPane(); // 列表被包裹
        flushBondListCare(); // 刷新一次列表, 该方法已经异步

        // 3.新panel包裹转债列表, 以及附带的查找框
        JPanel panelListContainer = new JPanel();
        panelListContainer.setLayout(new BorderLayout());
        jxFindBarS = new JXFindBarS(Color.red);
        panelListContainer.add(jxFindBarS, BorderLayout.NORTH);
        panelListContainer.add(jScrollPaneForList, BorderLayout.CENTER);

        // 4.最后组装
        panelLeft.add(functionPanel, BorderLayout.NORTH);
        panelLeft.add(panelListContainer, BorderLayout.CENTER);

    }


    /**
     * 功能区上方, 显示 当前选中的bean 的基本信息; 例如概念, 行业,余额等;  --> 最新背诵信息
     * 转债代码	转债名称	价格	剩余规模	上市日期	20日振幅	正股代码	正股名称	行业	概念	pe动	流值
     * 113537	文灿转债	278.03	1.4亿	20190705	40.6	603348	文灿股份	交运设备-汽车零部件-汽车零部件Ⅲ	蔚来汽车概念;新能源汽车;特斯拉	41.8	130.49亿
     */
    public static class SelectBeanDisplayPanel extends DisplayPanel {
        public static DataFrame<Object> allBondInfoDfForRevise = null; // 背诵字段df; 仅载入一次
        public static ConcurrentHashMap<String, List<Object>> allBondInfoForReviseMap = new ConcurrentHashMap<>(); //

        SecurityBeanEm bondBean;

        JLabel bondInfoLabel = getCommonLabel();
        JLabel stockInfoLabel = getCommonLabel();
        JLabel industryInfoLabel = getCommonLabel();
        JLabel conceptInfoLabel = getCommonLabel();

        public SelectBeanDisplayPanel() {
            this.setLayout(new GridLayout(4, 1, -1, -1)); // 4行1列;
            this.add(bondInfoLabel);
            this.add(stockInfoLabel);
            this.add(industryInfoLabel);
            this.add(conceptInfoLabel);

            if (allBondInfoDfForRevise == null || allBondInfoDfForRevise.length() < 100) {
                ThreadUtil.execAsync(new Runnable() {
                    @Override
                    public void run() {
                        allBondInfoDfForRevise = BondUtil.generateCSVForRecite1();
                        if (allBondInfoDfForRevise == null || allBondInfoDfForRevise.length() < 200) {
                            return;
                        }
                        // 载入到map里面, key为转债代码, value 为df单行!, 带有这种代码列, 因此注意索引!
                        for (int i = 0; i < allBondInfoDfForRevise.length(); i++) {
                            allBondInfoForReviseMap.put(allBondInfoDfForRevise.get(i, 0).toString(),
                                    allBondInfoDfForRevise.row(i));
                        }
                    }
                }, true);
            }
        }

        public static JLabel getCommonLabel() {
            JLabel jLabel = new JLabel();
            jLabel.setForeground(Color.red);
            jLabel.setBackground(Color.black);
            return jLabel;
        }

        /**
         * 给定df一行, 给定索引列表, 创建显示内容, 使用 / 间隔, 且null显示null
         *
         * @param objects
         * @param indexes
         * @return
         */
        public static String buildStrForLabelShow(List<Object> objects, List<Integer> indexes) {
            StringBuilder stringBuilder = new StringBuilder("");
            for (Integer index : indexes) {
                stringBuilder.append(" / "); // 最后去除
                Object o = objects.get(index);
                if (o == null) {
                    stringBuilder.append("null");
                } else {
                    stringBuilder.append(o.toString());
                }
            }
            return StrUtil.sub(stringBuilder.toString(), 3, stringBuilder.length());
        }


        @Override
        public void update() {
            if (this.bondBean == null) {
                return;
            }
            if (allBondInfoDfForRevise == null || allBondInfoDfForRevise.length() < 200) {
                return; // 要有全数据
            }
            String bondCode = bondBean.getSecCode();
            // 转债代码	转债名称	价格	剩余规模	上市日期	20日振幅	正股代码	正股名称	行业	概念	pe动	流值
            List<Object> infos = allBondInfoForReviseMap.get(bondCode);
            String s = buildStrForLabelShow(infos, Arrays.asList(0, 1, 2, 3));
            bondInfoLabel.setText(s);
            bondInfoLabel.setToolTipText(s);

            s = buildStrForLabelShow(infos, Arrays.asList(6, 7, 10, 11));
            stockInfoLabel.setText(s);
            stockInfoLabel.setToolTipText(s);

            s = buildStrForLabelShow(infos, Arrays.asList(8, 4, 5));
            industryInfoLabel.setText(s);
            industryInfoLabel.setToolTipText(s);

            s = buildStrForLabelShow(infos, Arrays.asList(9));
            conceptInfoLabel.setText(s);
            conceptInfoLabel.setToolTipText(s);
            // conceptInfoLabel.setText(infos.get(9).toString());
        }

        public void update(SecurityBeanEm beanEm) {
            this.bondBean = beanEm;
            this.update();
        }
    }

    SelectBeanDisplayPanel bondInfoPanel;

    /**
     * 功能区初始化
     */
    private void initFunctionPanel() {
        functionPanel = new JPanel();
        functionPanel.setPreferredSize(new Dimension(jListWidth, 200));
        functionPanel.setLayout(new BorderLayout());

        // 1.转债信息显示
        bondInfoPanel = new SelectBeanDisplayPanel();
        bondInfoPanel.setPreferredSize(new Dimension(jListWidth, 100));
        functionPanel.add(bondInfoPanel, BorderLayout.NORTH);

        // 2.功能按钮列表
        JPanel buttonContainer = new JPanel();
        buttonContainer.setLayout(new GridLayout(4, 4, 0, 0)); // 网格布局按钮
        buttonContainer.setBackground(Color.black);
        buttonContainer.setBorder(BorderFactory.createLineBorder(Color.red, 1));

        // 2.1. 8个点击改变复盘开始时间的按钮; 仅改变时分秒
        List<String> changeReviseStartTimeButtonTexts = Arrays
                .asList("9:30:00", "10:00:00", "10:30:00", "11:00:00", "13:00:00", "13:30:00", "14:00:00", "14:30:00");
        for (String text : changeReviseStartTimeButtonTexts) {
            FuncButton changeReviseStartTimeButton = getChangeReviseStartTimeButton(text);
            buttonContainer.add(changeReviseStartTimeButton);
        }

        // 2.2. 4个点击改变复盘时间倍率的按钮
        List<String> changeReviseTimeRateButtonTexts = Arrays
                .asList("1", "3", "5", "10");
        for (String text : changeReviseTimeRateButtonTexts) {
            FuncButton button = getChangeReviseTimeRateButton(text);
            buttonContainer.add(button);
        }

        // 2.3.@key: 各种功能按钮!
        // 2.3.1: 主动刷新转债列表 (已经有线程自动刷新)
        FuncButton loadBondListButton = ButtonFactory.getButton("刷新列表");
        loadBondListButton.addActionListener(e -> { // 点击加载或刷新转债列表;
            flushBondListCare(); // 已经实现了新建线程执行
        });


        // ---> 播报开启按钮 以及 停止按钮;
        // 2.3.2. 播报开启按钮
        FuncButton broadcastProcessStartButton = ButtonFactory.getButton("开启播报");
        broadcastProcessStartButton.setForeground(Color.orange);
        broadcastProcessStartButton.addActionListener(e -> { // 点击加载或刷新转债列表;
            ThreadUtil.execAsync(new Runnable() {
                @Override
                public void run() {
                    BondBuyNotify.changeEnvironmentToRevise(); // 模拟环境!
                    BondBuyNotify.main1(); // 播报程序启动; 主死循环会检测停止 flag; 重复点击将打印错误log
                }
            }, true);
        });

        // 2.3.3. 播报停止按钮 , 设置flag, 将会软停止播报主循环
        FuncButton broadcastProcessStopButton = ButtonFactory.getButton("停止播报");
        broadcastProcessStopButton.setForeground(Color.red);
        broadcastProcessStopButton.addActionListener(e -> { // 点击加载或刷新转债列表;
            ThreadUtil.execAsync(new Runnable() {
                @Override
                public void run() {
                    BondBuyNotify.stopBroadcast();
                }
            }, true);
        });

        //  2.3.3. 可手动刷新 转债 -- 正股/指数 map; 方便查询
        FuncButton flushBondToStockAndIndexMapButton = ButtonFactory.getButton("刷新债股字典");
        flushBondToStockAndIndexMapButton.addActionListener(e -> { // 点击加载或刷新转债列表;
            ThreadUtil.execAsync(new Runnable() {
                @Override
                public void run() {
                    BondUtil.flushBondToStockAndIndexMap();
                }
            }, true);
        });

        // 按钮添加
        buttonContainer.add(broadcastProcessStartButton);
        buttonContainer.add(broadcastProcessStopButton);
        buttonContainer.add(loadBondListButton);
        buttonContainer.add(flushBondToStockAndIndexMapButton);


        functionPanel.add(buttonContainer, BorderLayout.CENTER);
    }

    /**
     * 刷新转债列表方法; 列表组件实例化时将调用一次; 也是刷新列表按钮回调函数
     *
     * @throws Exception
     */
    public void flushBondListCare() {
        ThreadUtil.execAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    List<String> allBondCodes = null;
                    if (bondListUseRealTimeWenCai) {
                        DataFrame<Object> dataFrame = BondUtil.getVolTopNBondDf(200); // 限制200, 否则太多
                        try {
                            allBondCodes = DataFrameS.getColAsStringList(dataFrame, "code");
                        } catch (Exception e) {
                            e.printStackTrace();
                            return;
                        }
                    }
                    if (allBondCodes == null || allBondCodes.size() < 100) { // 失败或者设置就使用数据库
                        String dateStr = DateUtil.format(reviseStartDatetime, DatePattern.NORM_DATE_PATTERN);
                        allBondCodes = EastMoneyDbApi.getAllBondCodeByDateStr(
                                dateStr);

                        if (allBondCodes == null || allBondCodes.size() < 50) {
                            // 运行爬虫也是今日的了
                            log.warn("数据库获取转债代码列表失败: {} [建议运行爬虫 BondListEm] ; 将访问最新实时转债列表", dateStr);
                            try {
                                DataFrame<Object> bondDf = EmQuoteApi.getRealtimeQuotes(Arrays.asList("可转债"));
                                allBondCodes = DataFrameS.getColAsStringList(bondDf, "资产代码");
                            } catch (Exception e) {
                                CommonUtil.notifyError("访问最新实时转债代码列表依然失败, 更新转债列表失败");
                            }
                        }
                    }
                    if (allBondCodes == null || allBondCodes.size() < 100) {
                        CommonUtil.notifyError("转债代码列表获取失败, 更新转债列表失败");
                        return;
                    }
                    allBondCodes = allBondCodes.stream().filter(Objects::nonNull)
                            .collect(Collectors.toList()); // 不可null
                    List<SecurityBeanEm> bondList = SecurityBeanEm.createBondListOrdered(allBondCodes, false);
                    // bondList 无序, 将其按照 原来的allBondCodes 排序
                    flushBondListAs(bondList);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, true);
    }

    /**
     * 手动给定转债列表来刷新; 并且将查找信息 加入只能查找map中
     *
     * @param bondList
     */
    public void flushBondListAs(List<SecurityBeanEm> bondList) {
        for (SecurityBeanEm beanEm : bondList) {
            SmartFindDialog.findingMap.put(beanEm.getQuoteId(),
                    new SecurityBeanEm.SecurityEmPoForSmartFind(beanEm));
        }
        securityEmPos = SecurityEmPo.fromBeanList(bondList); // 更新
        if (loadAllFsDataFromDbWhenFlushBondList) { // 设置项: 是否载入数据到缓存; 异步执行
            ThreadUtil.execAsync(new Runnable() {
                @Override
                public void run() {
                    EastMoneyDbApi.loadFs1MAndFsTransDataToCache(bondList, getReviseDateStrSettingYMD());

                }
            }, true);
        }
    }

    /**
     * 一类按钮: 点击改变复盘开始时间(仅仅时分秒) 到按钮文字那么多
     *
     * @param tickHms
     * @return
     */
    public FuncButton getChangeReviseStartTimeButton(String tickHms) {
        FuncButton changeReviseStartTimeButton = ButtonFactory.getButton(tickHms);
        changeReviseStartTimeButton.setForeground(Color.red);
        changeReviseStartTimeButton.addActionListener(new ActionListener() {
            @SneakyThrows
            @Override
            public synchronized void actionPerformed(ActionEvent e) {
                // 重设复盘开始时间!
                reviseStartDatetime = DateUtil.parse(DateUtil.format(reviseStartDatetime,
                        DatePattern.NORM_DATE_PATTERN + " " + tickHms)); // 变量更新
                jTextFieldOfReviseStartDatetime // 文本框更新
                        .setText(DateUtil.format(reviseStartDatetime, DatePattern.NORM_DATETIME_PATTERN));
                dateTimePickerOfReviseStartDatetime.setSelect(reviseStartDatetime);// 改变选中
            }
        });
        return changeReviseStartTimeButton;
    }

    /**
     * 一类按钮: 点击改变 复盘时间倍率 到按钮文字那么多 -- 要求参数可解析为double
     *
     * @param tickHms
     * @return
     */
    public FuncButton getChangeReviseTimeRateButton(String timeRate) {
        FuncButton button = ButtonFactory.getButton(timeRate);
        button.setForeground(Color.yellow);
        button.addActionListener(new ActionListener() {
            @SneakyThrows
            @Override
            public synchronized void actionPerformed(ActionEvent e) {
                jTextFieldOfTimeRate.setText(timeRate);
            }
        });
        return button;
    }

    JScrollPane jScrollPaneForList;

    private void initJListWrappedJScrollPane() {
        jScrollPaneForList = new JScrollPane();
        jScrollPaneForList.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        jScrollPaneForList.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jScrollPaneForList.setViewportView(jXTableForBonds); // 滚动包裹转债列表
        jScrollPaneForList.getViewport().setBackground(COLOR_THEME_MINOR);
        BasicScrollBarUIS
                .replaceScrollBarUI(jScrollPaneForList, COLOR_THEME_TITLE, COLOR_SCROLL_BAR_THUMB); // 替换自定义 barUi
    }

    /**
     * 资产列表控件. 可重写
     *
     * @return
     */
    private JXTable getSecurityEmJXTable() {
        // securityEmPos --> 自行实现逻辑, 改变自身该属性; 则 列表将自动刷新

        DefaultListModelS<SecurityBeanEm.SecurityEmPo> model = new DefaultListModelS<>();
        model.flush(securityEmPos); // 刷新一次数据, 首次为空

        JXList jList = new JXList(model);
        jList.setCellRenderer(new SecurityEmListCellRendererS()); // 设置render
        jList.setForeground(COLOR_GRAY_COMMON);

        // 持续刷新列表, 100 ms一次. securityEmPos 应该为持续变化
        ThreadUtil.execAsync(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                while (true) { // 每 100ms 刷新model
                    model.flush(securityEmPos);

                    if (jXTableForBonds != null) {
                        jxFindBarS.setSearchable(jXTableForBonds.getSearchable());
                    }
                    Thread.sleep(10000);
                }
            }
        }, true);

        // 双击事件监听, 跳转到东方财富资产行情页面
        jList.addMouseListener(new MouseAdapter() { // 双击打开东财url
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2) { // 非双击
                    return;
                }
                int index = jList.getSelectedIndex();
                SecurityBeanEm.SecurityEmPo po = (SecurityEmPo) jList.getModel().getElementAt(index);
                openSecurityQuoteUrl(po);
            }
        });

        jList.addListSelectionListener(new ListSelectionListener() {
            private volatile int preIndex = -2; // 解决切换一次, 回调两次的问题; 记录preIndex, 相等则无视

            @Override
            public void valueChanged(ListSelectionEvent e) {
                int index = jList.getSelectedIndex(); // 选中切换
                if (preIndex == index) {
                    return;
                }
                preIndex = index; // 不相等
                SecurityBeanEm.SecurityEmPo po = (SecurityEmPo) jList.getModel().getElementAt(index);
                setSelectedBean(po.getBean());
            }
        });

        jList.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
        jList.setPreferredSize(new Dimension(jListWidth, 10000));
        jList.setBackground(COLOR_THEME_MAIN);
        jList.setBorder(null);
        return jList;
    }

    /**
     * 更新选中bean , 请调用方法, 同步设置pre的方式, 因控件事件触发, 而不合适, preSelectedBean的语义已经修改
     *
     * @param selectedBean
     */
    public void setSelectedBean(SecurityBeanEm bean) {
        this.selectedBean = bean;
        // @speed: 已经优化到 2-4 ms 级别
        updateFsDisplay(false); // 自动改变分时图显示, 不强制 首次18ms, 后面3ms
        bondInfoPanel.update(selectedBean); // 信息也要更改
    }

    public static void openSecurityQuoteUrl(SecurityBeanEm.SecurityEmPo po) {
        String url = null;
        SecurityBeanEm bean = po.getBean();
        if (bean.isBK()) {
            url = StrUtil.format("http://quote.eastmoney.com/bk/{}.html", bean.getQuoteId());
        } else if (bean.isIndex()) {
            url = StrUtil.format("http://quote.eastmoney.com/zs{}.html", bean.getSecCode());
        } else if (bean.isStock()) {
            if (bean.isHuA() || bean.isHuB()) {
                url = StrUtil.format("https://quote.eastmoney.com/{}{}.html", "sh", po.getSecCode());
            } else if (bean.isShenA() || bean.isShenB()) {
                url = StrUtil.format("https://quote.eastmoney.com/{}{}.html", "sz", po.getSecCode());
            } else if (bean.isJingA()) {
                url = StrUtil.format("http://quote.eastmoney.com/bj/{}.html", po.getSecCode());
            } else if (bean.isXSB()) {
                url = StrUtil.format("http://xinsanban.eastmoney.com/QuoteCenter/{}.html", po.getSecCode());
            } else if (bean.isKCB()) {
                url = StrUtil.format("http://quote.eastmoney.com/kcb/{}.html", po.getSecCode());
            }
        } else if (bean.isBond()) {
            if (bean.getMarket() == 0) {
                url = StrUtil.format("http://quote.eastmoney.com/sz{}.html", po.getSecCode());
            } else if (bean.getMarket() == 1) {
                url = StrUtil.format("http://quote.eastmoney.com/sh{}.html", po.getSecCode());
            }
        }

        if (url == null) {
            log.warn("未知资产类别, 无法打开行情页面: {}", bean.getName(), bean.getSecurityTypeName());
            return;
        }
        CommonUtil.openUrlWithDefaultBrowser(url);
    }

    public void showInMainDisplayWindow() {
        // 9.更改主界面显示自身
        mainDisplayWindow.setCenterPanel(this);
    }

    private static final Log log = LogUtil.getLogger();
}
