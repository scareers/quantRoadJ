package com.scareers.gui.ths.simulation.interact.gui.component.funcs;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.accountstate.AccountStatesDisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.order.OrderListAndDetailPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.bond.BondGlobalSimulationPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.industryconcept.IndustryConceptPanelForPlan;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news.plan.NewsTabPanelForPlan;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.stock.StockPanelForPlan;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.wencai.WenCaiApiPanelForPlan;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.securitylist.FsFetcherListAndDataPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.securitylist.FsTransFetcherListAndDataPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.state.SellStockListAndHsStatePanel;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.base.FuncFrameS;
import com.scareers.gui.ths.simulation.interact.gui.component.simple.FuncButton;
import com.scareers.gui.ths.simulation.interact.gui.ui.BasicScrollBarUIS;
import com.scareers.gui.ths.simulation.interact.gui.ui.renderer.TreeCellRendererS;
import com.scareers.gui.ths.simulation.interact.gui.util.GuiCommonUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.plaf.basic.BasicInternalFrameUI;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.*;
import static com.scareers.gui.ths.simulation.interact.gui.util.ImageScaler.zoomBySize;

/**
 * description: ????????????, ????????????. ??????trader?????????????????????
 *
 * @author: admin
 * @date: 2022/1/14/014-07:16:15
 */
@Getter
@Setter
public class FuncTreeWindow extends FuncFrameS {
    private static FuncTreeWindow INSTANCE;

    public static FuncTreeWindow getInstance(Type type, String title, TraderGui mainWindow,
                                             FuncButton belongBtn, boolean resizable, boolean closable,
                                             boolean maximizable,
                                             boolean iconifiable, int autoMaxWidthOrHeight, int autoMinWidthOrHeight,
                                             double preferScale,
                                             int funcToolsWidthOrHeight, boolean halfWidthOrHeight, Integer layer) {
        if (INSTANCE == null) {
            INSTANCE = new FuncTreeWindow(type, title, mainWindow, belongBtn, resizable, closable, maximizable,
                    iconifiable, autoMaxWidthOrHeight, autoMinWidthOrHeight, preferScale, funcToolsWidthOrHeight,
                    halfWidthOrHeight, layer);
            INSTANCE.setIconifiable(false);
            INSTANCE.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentHidden(ComponentEvent e) {
                    INSTANCE.getCorePanel().getMainDisplayWindow().flushBounds(true); // ?????????????????????????????????
                }
            });
            INSTANCE.setBorder(new LineBorder(COLOR_MAIN_DISPLAY_BORDER, 1));
            ((BasicInternalFrameUI) INSTANCE.getUI()).setNorthPane(null);
        }
        INSTANCE.getFuncTools().setVisible(false);
        INSTANCE.flushBounds();
        INSTANCE.getMainDisplayWindow().flushBounds();
        return INSTANCE;
    }

//    public static FuncTreeWindow getInstance() {
//        return INSTANCE; // ???null
//    }

    private FuncTreeWindow(Type type, String title, TraderGui mainWindow,
                           FuncButton belongBtn, boolean resizable, boolean closable, boolean maximizable,
                           boolean iconifiable, int autoMaxWidthOrHeight, int autoMinWidthOrHeight,
                           double preferScale,
                           int funcToolsWidthOrHeight, boolean halfWidthOrHeight, Integer layer) {
        super(type, title, mainWindow, belongBtn, resizable, closable, maximizable, iconifiable, autoMaxWidthOrHeight,
                autoMinWidthOrHeight, preferScale, funcToolsWidthOrHeight, halfWidthOrHeight, layer);

        FuncTreeWindow temp = this; // ???????????????. ???????????????, ?????????????????????size
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                temp.getMainDisplayWindow()
                        .setBounds(temp.getWidth(), 0, temp.getMainPane().getWidth() - temp.getWidth(),
                                temp.getMainPane().getHeight());
            }
        });
    }

    JTree tree;

    @Override
    public void initCenterPanel() { // ????????????
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BorderLayout());
        tree = buildTree();
        tree.setBackground(COLOR_THEME_MINOR);
        jPanel.add(tree, BorderLayout.WEST);
        tree.setLocation(0, 0);
        JScrollPane jScrollPane = new JScrollPane(jPanel);
        jScrollPane.setBorder(null);
        jScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        BasicScrollBarUIS
                .replaceScrollBarUI(jScrollPane, COLOR_THEME_MINOR, COLOR_SCROLL_BAR_THUMB); // ???????????????barUi

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(jScrollPane, BorderLayout.CENTER);
        this.centerPanel = panel;

        this.add(this.centerPanel, BorderLayout.CENTER);
    }

    @Override
    protected List<FuncButton> getToolButtons1() { // ??????????????????(????????????)
        return super.defaultToolsButtonList1();
    }

    @Override
    protected List<FuncButton> getToolButtons2() {
        return super.defaultToolsButtonList2();
    }

    private JTree buildTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("?????????");

        /*
        1.trader ??????
         */
        DefaultMutableTreeNode traderNode = new DefaultMutableTreeNode("Trader");
        // 5????????????,?????????
        DefaultMutableTreeNode orderExecutorNode = new DefaultMutableTreeNode("OrderExecutor");
        DefaultMutableTreeNode checkerNode = new DefaultMutableTreeNode("Checker");
        DefaultMutableTreeNode accountStatesNode = new DefaultMutableTreeNode("AccountStates");
        DefaultMutableTreeNode fsTransactionFetcherNode = new DefaultMutableTreeNode("FsTransactionFetcher");
        DefaultMutableTreeNode fsFetcherNode = new DefaultMutableTreeNode("FsFetcher");
        DefaultMutableTreeNode strategyNode = new DefaultMutableTreeNode("Strategy");

        // 4?????????/map
        DefaultMutableTreeNode queues = new DefaultMutableTreeNode("Queues!"); // ?????????
        DefaultMutableTreeNode ordersWaitForExecution = new DefaultMutableTreeNode("ordersWaitForExecution");
        DefaultMutableTreeNode ordersWaitForCheckTransactionStatusMap = new DefaultMutableTreeNode(
                "ordersWaitForCheckTransactionStatusMap");
        DefaultMutableTreeNode ordersSuccessFinished = new DefaultMutableTreeNode("ordersSuccessFinished");
        DefaultMutableTreeNode ordersResendFinished = new DefaultMutableTreeNode("ordersResendFinished");
        DefaultMutableTreeNode ordersFailedFinished = new DefaultMutableTreeNode("ordersFailedFinallyNeedManualHandle");
        DefaultMutableTreeNode ordersAllMap = new DefaultMutableTreeNode("ordersAllMap");
        queues.add(ordersWaitForExecution);
        queues.add(ordersWaitForCheckTransactionStatusMap);
        queues.add(ordersSuccessFinished);
        queues.add(ordersResendFinished);
        queues.add(ordersFailedFinished);
        queues.add(ordersAllMap);

        traderNode.add(queues);
        traderNode.add(orderExecutorNode);
        traderNode.add(checkerNode);
        traderNode.add(accountStatesNode);
        traderNode.add(fsTransactionFetcherNode);
        traderNode.add(fsFetcherNode);
        traderNode.add(strategyNode);


        /*
        2.???????????????????????????
         */

        DefaultMutableTreeNode analyzeNode = new DefaultMutableTreeNode("????????????");
        DefaultMutableTreeNode stockNode = new DefaultMutableTreeNode("??????");
        // ??????????????????
        DefaultMutableTreeNode sellNode = new DefaultMutableTreeNode("????????????");
        DefaultMutableTreeNode buyNode = new DefaultMutableTreeNode("????????????");
        stockNode.add(sellNode);
        stockNode.add(buyNode);
        analyzeNode.add(stockNode);

        /*
        3.????????????,???????????????
         */
        DefaultMutableTreeNode reviewAndPlanNode = new DefaultMutableTreeNode("????????????");
        DefaultMutableTreeNode tradePlanNode = new DefaultMutableTreeNode("????????????");
        // ?????????
        DefaultMutableTreeNode importantNewsNode2 = new DefaultMutableTreeNode("???????????????");
        DefaultMutableTreeNode wencaiApiNode = new DefaultMutableTreeNode("????????????");
        DefaultMutableTreeNode industryAndConceptNode = new DefaultMutableTreeNode("???????????????");
        DefaultMutableTreeNode stockPlanNode = new DefaultMutableTreeNode("????????????");
        DefaultMutableTreeNode bondSimulationNode = new DefaultMutableTreeNode("????????????");
        tradePlanNode.add(importantNewsNode2);
        tradePlanNode.add(wencaiApiNode);
        tradePlanNode.add(industryAndConceptNode);
        tradePlanNode.add(stockPlanNode);
        tradePlanNode.add(bondSimulationNode);

        reviewAndPlanNode.add(tradePlanNode);


        root.add(traderNode);
        root.add(analyzeNode);
        root.add(reviewAndPlanNode);


        final JTree tree = new JTree(root);
        // ??????????????????

        TreeCellRendererS renderer = new TreeCellRendererS();
        renderer.setBackgroundNonSelectionColor(COLOR_THEME_MINOR);
        renderer.setBackgroundSelectionColor(COLOR_TREE_ITEM_SELECTED);
        renderer.setBorderSelectionColor(Color.red);
        renderer.setClosedIcon(new ImageIcon(
                zoomBySize(new ImageIcon(ResourceUtil.getResource(ICON_FOLDER_CLOSE_PATH)).getImage(), 16, 16)));
        renderer.setOpenIcon(new ImageIcon(
                zoomBySize(new ImageIcon(ResourceUtil.getResource(ICON_FOLDER_OPEN_PATH)).getImage(), 16, 16)));
        renderer.setLeafIcon(new ImageIcon(
                zoomBySize(new ImageIcon(ResourceUtil.getResource(ICON_FILE0_PATH)).getImage(), 15, 15)));
        renderer.setFont(new Font("????????????", Font.BOLD, 14));
        renderer.setTextSelectionColor(COLOR_GRAY_COMMON);
        renderer.setTextNonSelectionColor(COLOR_GRAY_COMMON);

        tree.setCellRenderer(renderer);
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            @SneakyThrows
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                if (e.getNewLeadSelectionPath() != null) {
                    dispatch(e.getNewLeadSelectionPath().toString());
                }
            }
        });
//        GuiCommonUtil.selectTreeNode(tree, TreePathConstants.SELL_QUEUE);
//        GuiCommonUtil.selectTreeNode(tree, TreePathConstants.ORDER_ALL_MAP);
        //GuiCommonUtil.selectTreeNode(tree, TreePathConstants.PLAN_IMPORTANT_NEWS);
//        GuiCommonUtil.selectTreeNode(tree, TreePathConstants.INDUSTRY_AND_CONCEPT);
//        GuiCommonUtil.selectTreeNode(tree, TreePathConstants.STOCK_PLAN);
        GuiCommonUtil.selectTreeNode(tree, TreePathConstants.BOND_SIMULATION);
        return tree;
    }

    public void dispatch(String treePath) {
        // 1. 5?????????
        if (TreePathConstants.ORDERS_WAIT_FOR_EXECUTION.equals(treePath)) {
            changeToDisplayOrderList(OrderListAndDetailPanel.Type.ORDERS_WAIT_FOR_EXECUTION);
        } else if (TreePathConstants.ORDER_ALL_MAP.equals(treePath)) {
            changeToDisplayOrderList(OrderListAndDetailPanel.Type.ORDER_ALL_MAP);
        } else if (TreePathConstants.ORDERS_WAIT_FOR_CHECK_TRANSACTION_STATUS_MAP.equals(treePath)) {
            changeToDisplayOrderList(OrderListAndDetailPanel.Type.ORDERS_WAIT_FOR_CHECK_TRANSACTION_STATUS_MAP);
        } else if (TreePathConstants.ORDERS_SUCCESS_FINISHED.equals(treePath)) {
            changeToDisplayOrderList(OrderListAndDetailPanel.Type.ORDERS_SUCCESS_FINISHED);
        } else if (TreePathConstants.ORDERS_FAILED_FINISHED.equals(treePath)) {
            changeToDisplayOrderList(OrderListAndDetailPanel.Type.ORDERS_FAILED_FINISHED);
        } else if (TreePathConstants.ORDERS_RESEND_FINISHED.equals(treePath)) {
            changeToDisplayOrderList(OrderListAndDetailPanel.Type.ORDERS_RESEND_FINISHED);
            // 2.1???????????????
        } else if (TreePathConstants.FS_FETCHER.equals(treePath)) {
            changeToDisplayFs1MData();
            // 2.????????????
        } else if (TreePathConstants.FS_TRANSACTION_FETCHER.equals(treePath)) {
            changeToDisplayFsTransData();
            // 2.2.????????????
        } else if (TreePathConstants.ACCOUNT_STATES.equals(treePath)) {
            changeToDisplayAccountStates();
            // ????????????
        } else if (TreePathConstants.SELL_QUEUE.equals(treePath)) {
            changeToSellQueue();
            // ???????????? -- ?????????
        } else if (TreePathConstants.PLAN_IMPORTANT_NEWS.equals(treePath)) {
            changeToPlanImportantNews();
            // ???????????? -- ???????????????
        } else if (TreePathConstants.PLAN_WENCAI_API.equals(treePath)) {
            changeToWenCaiApiForPlan();
        } else if (TreePathConstants.INDUSTRY_AND_CONCEPT.equals(treePath)) {
            changeToIndustryConceptPanelForPlan();
        } else if (TreePathConstants.STOCK_PLAN.equals(treePath)) {
            changeToStockPlanPanel();
        } else if (TreePathConstants.BOND_SIMULATION.equals(treePath)) {
            changeToBondGlobalSimulationPanel();
        } else {
            System.out.println(treePath);
        }
    }

    private void changeToDisplayOrderList(OrderListAndDetailPanel.Type type) {
        OrderListAndDetailPanel
                .getInstance(this.getMainDisplayWindow())
                .changeType(type)
                .showInMainDisplayWindow();
    }

    private void changeToDisplayFs1MData() {
        FsFetcherListAndDataPanel.getInstance(getMainDisplayWindow(), 300) // ?????????????????????????????????
                .showInMainDisplayWindow();
    }

    private void changeToDisplayFsTransData() {
        FsTransFetcherListAndDataPanel.getInstance(getMainDisplayWindow(), 300) // ?????????????????????????????????
                .showInMainDisplayWindow();
    }

    private void changeToDisplayAccountStates() {
        AccountStatesDisplayPanel.getInstance(getMainDisplayWindow()).showInMainDisplayWindow();
    }

    private void changeToSellQueue() {
        SellStockListAndHsStatePanel
                .getInstance(this.getMainDisplayWindow(), 300)
                .showInMainDisplayWindow();
    }


    private void changeToPlanImportantNews() {
        NewsTabPanelForPlan
                .getInstance(this.getMainDisplayWindow())
                .showInMainDisplayWindow();
    }

    private void changeToIndustryConceptPanelForPlan() {
        IndustryConceptPanelForPlan
                .getInstance(this.getMainDisplayWindow())
                .showInMainDisplayWindow();
    }

    private void changeToWenCaiApiForPlan() {
        WenCaiApiPanelForPlan
                .getInstance(this.getMainDisplayWindow())
                .showInMainDisplayWindow();
    }

    private void changeToStockPlanPanel() {
        StockPanelForPlan
                .getInstance(this.getMainDisplayWindow())
                .showInMainDisplayWindow();
    }

    private void changeToBondGlobalSimulationPanel() {
        BondGlobalSimulationPanel
                .getInstance(this.getMainDisplayWindow(), 380)
                .showInMainDisplayWindow();
        if (TraderGui.INSTANCE != null) { // ??????gui????????????
            TraderGui.INSTANCE.setFunctionGuiCurrent(TraderGui.FunctionGuiCurrent.BOND_REVISE);
            ThreadUtil.execAsync(new Runnable() {
                @Override
                public void run() {
                    ThreadUtil.sleep(1000);
                    TraderGui.INSTANCE.objectsBtn.doClick(); // ???????????????
                }
            }, true);
        }

        // CTRL+Z ??????


    }


    public static class TreePathConstants { // ????????????, ???????????????
        /**
         * [?????????]
         * [?????????, Trader]
         * [?????????, Trader, Queues!]
         * [?????????, Trader, OrderExecutor]
         * [?????????, Trader, Checker]
         * [?????????, Trader, AccountStates]
         * [?????????, Trader, FsTransactionFetcher]
         * [?????????, Trader, FsFetcher]
         * [?????????, Trader, Strategy]
         * [?????????, Trader, Queues!, ordersWaitForExecution]
         * [?????????, Trader, Queues!, ordersAllMap]
         * [?????????, Trader, Queues!, ordersWaitForCheckTransactionStatusMap]
         * [?????????, Trader, Queues!, ordersSuccessFinished]
         * [?????????, Trader, Queues!, ordersResendFinished]
         * [?????????, Trader, Queues!, ordersFailedFinallyNeedManualHandle]
         */
        public static final String OBJECT_OBSERVER = "[?????????]";
        public static final String TRADER = "[?????????, Trader]";
        public static final String QUEUES = "[?????????, Trader, Queues!]";

        public static final String ORDER_EXECUTOR = "[?????????, Trader, OrderExecutor]";
        public static final String CHECKER = "[?????????, Trader, Checker]";
        public static final String ACCOUNT_STATES = "[?????????, Trader, AccountStates]";
        public static final String FS_TRANSACTION_FETCHER = "[?????????, Trader, FsTransactionFetcher]";
        public static final String FS_FETCHER = "[?????????, Trader, FsFetcher]";
        public static final String STRATEGY = "[?????????, Trader, Strategy]";

        public static final String ORDERS_WAIT_FOR_EXECUTION = "[?????????, Trader, Queues!, ordersWaitForExecution]";
        public static final String ORDER_ALL_MAP = "[?????????, Trader, Queues!, ordersAllMap]";
        public static final String ORDERS_WAIT_FOR_CHECK_TRANSACTION_STATUS_MAP = "[?????????, Trader, Queues!, " +
                "ordersWaitForCheckTransactionStatusMap]";
        public static final String ORDERS_SUCCESS_FINISHED = "[?????????, Trader, Queues!, ordersSuccessFinished]";
        public static final String ORDERS_RESEND_FINISHED = "[?????????, Trader, Queues!, ordersResendFinished]";
        public static final String ORDERS_FAILED_FINISHED = "[?????????, Trader, Queues!, " +
                "ordersFailedFinallyNeedManualHandle]";

        /**
         * [????????????]
         * [????????????, ??????]
         * [????????????, ??????, ????????????]
         * [????????????, ??????, ????????????]
         */
        public static final String REALTIME_ANALYZE = "[?????????, ????????????]";
        public static final String STOCK_NODE = "[?????????, ????????????, ??????]";
        public static final String BUY_QUEUE = "[?????????, ????????????, ??????, ????????????]";
        public static final String SELL_QUEUE = "[?????????, ????????????, ??????, ????????????]";

        /**
         * [?????????, ????????????]
         * [?????????, ????????????, ????????????]
         * [?????????, ????????????, ????????????, ???????????????]
         * "[?????????, ????????????, ????????????, ????????????]";
         * "[?????????, ????????????, ????????????, ???????????????]";
         * "[?????????, ????????????, ????????????, ????????????]";
         */

        public static final String REVIEW_AND_PLAN = "[?????????, ????????????]";
        public static final String PLAN_IMPORTANT_NEWS = "[?????????, ????????????, ????????????, ???????????????]";
        public static final String PLAN_WENCAI_API = "[?????????, ????????????, ????????????, ????????????]";
        public static final String INDUSTRY_AND_CONCEPT = "[?????????, ????????????, ????????????, ???????????????]";
        public static final String STOCK_PLAN = "[?????????, ????????????, ????????????, ????????????]";
        public static final String BOND_SIMULATION = "[?????????, ????????????, ????????????, ????????????]";

        public static final String TRADE_PLAN = "[?????????, ????????????, ????????????]";

    }
}

