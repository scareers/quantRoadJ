package com.scareers.gui.ths.simulation.interact.gui.component.combination;

import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.MainDisplayWindow;
import com.scareers.gui.ths.simulation.interact.gui.model.DefaultListModelS;
import com.scareers.gui.ths.simulation.interact.gui.ui.renderer.OrderListCellRendererS;
import com.scareers.gui.ths.simulation.order.Order;
import com.scareers.gui.ths.simulation.order.Order.OrderSimple;
import com.scareers.gui.ths.simulation.trader.Trader;
import lombok.SneakyThrows;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.COLOR_GRAY_COMMON;
import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.COLOR_THEME_TITLE;
import static com.scareers.utils.CommonUtil.waitUtil;

/**
 * description: 显示 Order(某)列表 即某一具体Order细节 即响应列表. 组合 panel
 * 需要指定类型, 以便死循环遍历更新数据! 不同的类型将遍历不同Trader属性
 *
 * @author: admin
 * @date: 2022/1/19/019-00:06:06
 */
public class OrderListAndDetailPanel extends JPanel {
    public static volatile OrderListAndDetailPanel INSTANCE;
    // @noti: 使用单例模式. 当在 5种类型切换时, 仅仅切换 Type, 而jList和两个order展示, 将实时根据Type获取数据
    public static volatile Type currentDataFlushType = Type.ORDER_ALL_MAP; // 默认all
    // 股票列表池, 分为不同 Type. 将读取 objectPool 所有key(已注册的类型), 对对应key更新列表, 保存入 map.
    public static volatile Vector<OrderSimple> currentOrderListShouldDisplay = new Vector<>(
            Arrays.asList(OrderSimple.getDummyOrderSimple()));

    /**
     * 单例模式
     *
     * @param type
     * @param mainDisplayWindow
     * @return
     */
    public static OrderListAndDetailPanel getInstance(MainDisplayWindow mainDisplayWindow) {
        if (INSTANCE == null) {
            // 首次调用, 将开始更新数据
            ThreadUtil.execAsync(new Runnable() {
                @SneakyThrows
                @Override
                public void run() {
                    //等待 有控件被注册
                    try {
                        waitUtil(() -> {
                            try {
                                return Trader.allOrderAmount > 0;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return false;
                        }, Integer.MAX_VALUE, 1, "等待首个订单生成", true);
                    } catch (TimeoutException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }


                    while (true) { // 开始更新所有注册控件类型的相关数据列表
                        Vector<OrderSimple> simpleOrders = new Vector<>();
                        if (currentDataFlushType == Type.ORDERS_WAIT_FOR_EXECUTION) {
                            simpleOrders = Order.ordersForDisplay(
                                    new ArrayList<>(Trader.ordersWaitForExecution));
                        } else if (currentDataFlushType == Type.ORDER_ALL_MAP) {
                            Trader.getInstance();
                            simpleOrders = Order.ordersForDisplay(
                                    new ArrayList<>(Trader.ordersAllMap.keySet()));
                        } else if (currentDataFlushType == Type.ORDERS_WAIT_FOR_CHECK_TRANSACTION_STATUS_MAP) {
                            simpleOrders = Order.ordersForDisplay(
                                    new ArrayList<>(
                                            Trader.ordersWaitForCheckTransactionStatusMap.keySet()));
                        } else if (currentDataFlushType == Type.ORDERS_SUCCESS_FINISHED) {
                            simpleOrders = Order.ordersForDisplay(
                                    new ArrayList<>(Trader.ordersSuccessFinished.keySet()));
                        } else if (currentDataFlushType == Type.ORDERS_RESEND_FINISHED) {
                            simpleOrders = Order.ordersForDisplay(
                                    new ArrayList<>(Trader.ordersResendFinished.keySet()));
                        } else {
                            System.out.println("未知类型");
                        }
                        if (simpleOrders.size() == 0) {
                            simpleOrders.add(OrderSimple.getDummyOrderSimple());
                        }
                        Collections.sort(simpleOrders); // 有序
                        currentOrderListShouldDisplay = simpleOrders;// 真实更新数据池
                        Thread.sleep(10);
                    }
                }
            }, true);
            INSTANCE = new OrderListAndDetailPanel(Type.ORDER_ALL_MAP, mainDisplayWindow); // 默认所有订单,自行调用changeType
        }
//        mainDisplayWindow.getCenterPanel().addComponentListener(new ComponentAdapter() {
//            @Override
//            public void componentResized(ComponentEvent e) {
//                System.out.println("xxx");
//                INSTANCE.setBounds(0, 0, mainDisplayWindow.getWidth(), mainDisplayWindow.getHeight());
//            }
//        });
        return INSTANCE;
    }

    /**
     * 5类队列
     */
    public enum Type {
        ORDERS_WAIT_FOR_EXECUTION,
        ORDER_ALL_MAP,
        ORDERS_WAIT_FOR_CHECK_TRANSACTION_STATUS_MAP,
        ORDERS_SUCCESS_FINISHED,
        ORDERS_RESEND_FINISHED
    }


    private static volatile OrderSimple selectedOrder; // 唯一被选中订单
    MainDisplayWindow mainDisplayWindow; // 主显示区
    private volatile Type type; // 类型
    volatile JList<OrderSimple> jList;

    private static volatile boolean changeSelect = false; // 标志订单是否改变.

    private OrderListAndDetailPanel() {
    }

    private OrderListAndDetailPanel(Type type, MainDisplayWindow mainDisplayWindow) {
        super();
        this.type = type;
        this.mainDisplayWindow = mainDisplayWindow;
        currentDataFlushType = this.type;

        // 1.主容器
        this.setLayout(new BorderLayout());

        // 2.JList显示列表
        jList = getOrderSimpleJList();
        jList.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
        this.add(jList, BorderLayout.WEST); // 添加列表
        jList.setPreferredSize(new Dimension(300, 10000));
        jList.setBackground(COLOR_THEME_TITLE);

        // 3.JSplitPane 分割显示 Order详情 及 响应对象
        JSplitPane orderContent = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT); // box 存放 order详情和响应
        orderContent.setDividerLocation(500);

        // 3.1. Order详情控件
        OrderDetailPanel orderDetailPanel = getDetailPanel();
        orderContent.setLeftComponent(orderDetailPanel);

        // 3.2. Order响应控件
        OrderResponsePanel responsePanel = getResponsePanel();
        orderContent.setRightComponent(responsePanel);

        // 4.添加分割面板
        this.add(orderContent, BorderLayout.CENTER);


        // 6.主panel 添加尺寸改变监听. 改变 jList 和 orderContent尺寸
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                jList.setBounds(0, 0, 300, getHeight()); // 固定宽 300
                orderContent.setDividerLocation(0.32); // 分割位置百分比
                orderContent.setBounds(300, 0, getWidth() - 300, getHeight()); // 其余占满
            }
        });


        ThreadUtil.execAsync(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                while (true) {
                    List<Order> allOrder = // 因此直接在全map中查找. 且像等待队列一类, 可能出现点击后消失, 找不到订单
                            new ArrayList<>(Trader.ordersAllMap.keySet());
                    Order currentOrder = null;
                    if (jList.getSelectedIndex() == -1) {
                        Thread.sleep(1); // 不断刷新
                        continue;
                    }
                    String orderId = null; // 避免变化, 及时JList已经刷新, 也能展示当时点击的订单而非刷新后
                    try {
                        orderId = currentOrderListShouldDisplay.get(jList.getSelectedIndex())
                                .getRawOrderId();
                        // 因实时改变,需要防止越界
                    } catch (Exception e) {
                        Thread.sleep(1); // 不断刷新
                        continue;
                    }
                    for (Order order : allOrder) {
                        if (order.getRawOrderId().equals(orderId)) {
                            currentOrder = order; // 找到唯一的 Order 对象
                            break;
                        }
                    }
                    if (currentOrder != null) {
                        orderDetailPanel.updateText(currentOrder);
                        responsePanel.updateText(currentOrder);
                    }
                    Thread.sleep(1); // 不断刷新
                }
            }
        }, true);
    }

    public void showInMainDisplayWindow() {
        // 9.更改主界面显示自身
        mainDisplayWindow.setCenterPanel(this);
    }

    /**
     * 修改类型达成展示列表的改变,
     *
     * @param newType
     * @return
     * @key3 实测
     */
    @SneakyThrows
    public OrderListAndDetailPanel changeType(Type newType) {
        this.type = newType;
        currentDataFlushType = this.type; // 将更换刷新数据源
        jList.setSelectedIndex(0); // 选择第一个.
        return this;
    }

    private OrderDetailPanel getDetailPanel() {
        return new OrderDetailPanel();
    }

    private OrderResponsePanel getResponsePanel() {
        return new OrderResponsePanel();
    }

    /**
     * 只需要 切换 type, 则达成 JList的数据改变
     *
     * @return
     */
    private JList<Order.OrderSimple> getOrderSimpleJList() {
        DefaultListModelS<Order.OrderSimple> model = new DefaultListModelS<>();
        model.flush(currentOrderListShouldDisplay);

        JList<Order.OrderSimple> jList = new JList<>(model);
        jList.setCellRenderer(new OrderListCellRendererS());
        jList.setForeground(COLOR_GRAY_COMMON);

        // 单例单线程
        ThreadUtil.execAsync(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                while (true) { // 每 100ms 刷新model
                    model.flush(currentOrderListShouldDisplay);
                    Thread.sleep(1);
                }
            }
        }, true);

        return jList;
    }


//    private volatile OrderSimple selectedOrder; // 唯一被选中订单
//    private boolean monitorStarted; // 监控订单的线程是否启动
//    private boolean selectedOrderChanged = false; // 监控订单的线程是否启动
//    MainDisplayWindow mainDisplayWindow;
//
//    private boolean stopFlushList = false; // 可设置此flag, 停止死循环更新 JList
//    private boolean stoped = false; // 是否已被停止, 将 waitUtil此flag, 代表 JList的刷新已被真正停止. 见 stopFlushJList 方法
//
//    private void changeToOrdersWaitForExecution() throws Exception {
//        JList<OrderSimple> jList = getOrderSimpleJList();
//
//        JPanel panel = new JPanel();
//        panel.setLayout(new BorderLayout());
//        panel.add(jList, BorderLayout.WEST); // 添加列表
//        jList.setPreferredSize(new Dimension(300, 10000));
//        jList.setBackground(COLOR_THEME_TITLE);
//
//        JSplitPane orderContent = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT); // box 存放 order详情和响应
//        orderContent.setDividerLocation(500);
//
//        OrderDetailPanel orderDetailPanel = getDetailPanel();
////        orderDetailPanel.setPreferredSize(new Dimension(700, 10));
//        orderContent.setLeftComponent(orderDetailPanel); // 添加响应
//
//        OrderResponsePanel responsePanel = getResponsePanel();
//        orderContent.setRightComponent(responsePanel); // 添加响应
////        responsePanel.setPreferredSize(new Dimension(10000, 10000));
//
//
//        jList.addListSelectionListener(new ListSelectionListener() {
//            @SneakyThrows
//            @Override
//            public void valueChanged(ListSelectionEvent e) {
//                if (e.getValueIsAdjusting()) {
//                    return; // 若是数据更新调整, 则无视
//                }
//                ThreadUtil.execAsync(new Runnable() {
//                    @SneakyThrows
//                    @Override
//                    public void run() {
//                        while (true) {
//                            int index = e.getLastIndex();
//                            try {
//                                selectedOrder = jList.getModel().getElementAt(index);
//                                selectedOrderChanged = true;
//                            } catch (Exception ex) {
//                                Thread.sleep(100);
//                                continue;
//                            }
//                            break; // 仅设置1次
//                        }
//                    }
//                }, true);
//            }
//        });
//
//        panel.add(orderContent, BorderLayout.CENTER); // 添加详情
//        panel.addComponentListener(new ComponentAdapter() {
//            @Override
//            public void componentResized(ComponentEvent e) {
//                jList.setSize(300, 10000);
//                orderContent.setDividerLocation(0.35); // 分割位置百分比
//                orderContent.setBounds(0, 0, panel.getWidth() - 300, panel.getHeight()); // 占满
//            }
//        });
//
//        jList.setSelectedIndex(0); // 选择第一个
//
//        if (monitorStarted) {
//            return;
//        }
//        ThreadUtil.execAsync(new Runnable() { // 开始监控 selectedOrder
//            @SneakyThrows
//            @Override
//            public void run() {
//                while (true) {
//                    if (selectedOrder == null) {
//                        Thread.sleep(100);
//                        continue;
//                    }
//                    String orderId = selectedOrder.getRawOrderId();
//
//                    while (true) {
//                        if (selectedOrderChanged) {
//                            selectedOrderChanged = false;
//                            break;
//                        }
//                        Order currentOrder = null;
//                        // 从最新队列读取
//                        for (Order order : Trader.getInstance().getOrdersAllMap().keySet()) {
////                        for (Order order : Trader.getInstance().getOrdersWaitForExecution()) {
//                            if (order.getRawOrderId().equals(orderId)) {
//                                currentOrder = order;
//                            }
//                        } // 查找具体
//                        if (currentOrder != null) {
//                            orderDetailPanel.updateText(currentOrder);
//                            responsePanel.updateText(currentOrder);
//                        }
//                        Thread.sleep(100); // 不断刷新响应
//                    }
//                }
//            }
//        }, true);
//        mainDisplayWindow.setCenterPanel(panel);
//    }
//
//    private OrderDetailPanel getDetailPanel() {
//        return new OrderDetailPanel();
//    }
//
//    private OrderResponsePanel getResponsePanel() {
//        return new OrderResponsePanel();
//    }
//
//    private JList<Order.OrderSimple> getOrderSimpleJList() throws Exception {
//        Vector<OrderSimple> simpleOrders = getOrderSimplesAccordingType();
//        DefaultListModelS<Order.OrderSimple> model = new DefaultListModelS<>();
//        model.flush(simpleOrders);
//
//        JList<Order.OrderSimple> jList = new JList<>(model);
//        jList.setCellRenderer(new OrderListCellRendererS());
//        jList.setForeground(COLOR_GRAY_COMMON);
//        ThreadUtil.execAsync(new Runnable() {
//            @SneakyThrows
//            @Override
//            public void run() {
//                while (true) { // 每半秒刷新model
//                    Vector<Order.OrderSimple> simpleOrders = Order
//                            .ordersForDisplay(new ArrayList<>(Trader.getInstance().getOrdersAllMap().keySet()));
////                            .ordersForDisplay(new ArrayList<>(Trader.getInstance().getOrdersWaitForExecution()));
//                    if (simpleOrders.size() == 0) {
//                        simpleOrders.add(Order.OrderSimple.getDummyOrderSimple());
//                    }
//                    model.flush(simpleOrders);
//                    Thread.sleep(100);
//                }
//            }
//        }, true);
//        return jList;
//    }
//
//    private Vector<OrderSimple> getOrderSimplesAccordingType() throws Exception {
//        Trader trader = Trader.getInstance();
//        PriorityBlockingQueue<Order> orders = trader.getOrdersWaitForExecution();
//
//        Vector<OrderSimple> simpleOrders = Order.ordersForDisplay(new ArrayList<>(orders));
//
//        if (simpleOrders.size() == 0) {
//            simpleOrders.add(OrderSimple.getDummyOrderSimple());
//        }
//        return simpleOrders;
//    }
}
