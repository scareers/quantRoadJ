package com.scareers.gui.ths.simulation.interact.gui;

import cn.hutool.json.JSONUtil;
import com.scareers.gui.ths.simulation.interact.gui.component.JDisplayForLog;
import com.scareers.gui.ths.simulation.interact.gui.ui.TabbedPaneUIS;
import com.scareers.gui.ths.simulation.order.BuyOrder;
import com.scareers.gui.ths.simulation.trader.Trader;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.*;

import static java.awt.BorderLayout.NORTH;
import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.themeColor;


/**
 * description:
 *
 * @author: admin
 * @date: 2022/1/4/004-17:03:03
 */
public class TraderGUI {
    JFrame mainWindow;


    public static void main(String[] args) throws Exception {
        main0(args);
    }

    public static void main0(String[] agrs) throws Exception {
        TraderGUI gui = new TraderGUI();
        gui.initGlobalStyle();
        gui.initMainWindow();
        gui.showAndStartTrader();
    }

    private void showAndStartTrader() throws Exception {
        JTabbedPane jTabbedPane = new JTabbedPane(JTabbedPane.BOTTOM);
        jTabbedPane.setUI(new TabbedPaneUIS()); // 使用自定义ui
//        jTabbedPane.setFont(new Font());
        jTabbedPane.setForeground(Color.WHITE);

        JDisplayForLog jDisplayForLog = new JDisplayForLog();
        jDisplayForLog.setPreferredSize(new Dimension(1980, 300));


        JSplitPane jSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);//设定为左右拆分布局
        jSplitPane.setBorder(null);
        jSplitPane.setOneTouchExpandable(true); // 让分割线显示出箭头
        jSplitPane.setContinuousLayout(true); // 操作箭头，重绘图形
        jSplitPane.setDividerSize(1); //设置分割线的宽度


        JPanel mainPanel = new JPanel();
        mainPanel.setPreferredSize(new Dimension(1024, 800));

        jSplitPane.setTopComponent(mainPanel);//布局中添加组件 ，面板1


        // JTabbedPane 封装底部

//        jTabbedPane.addTab("Terminal", jDisplayForLog);
        JInternalFrame jInternalFrame = new JInternalFrame("Terminal", true, true, false);
        jInternalFrame.add(jDisplayForLog);
        jInternalFrame.setBorder(null);
        jTabbedPane.addTab("Terminal", jInternalFrame);

        jTabbedPane.addTab("Terminal2", new Label("测试 tab"));
        jSplitPane.setBottomComponent(jTabbedPane);

        JLabel label = new JLabel("我是状态栏");
        label.setFont(new Font("宋体", Font.BOLD, 20));
        label.setForeground(Color.RED);

        mainWindow.add(label, BorderLayout.SOUTH);
        mainWindow.add(jSplitPane, BorderLayout.CENTER);

        mainWindow.pack();
        mainWindow.setVisible(true);
        Trader.main0(null);
    }

    public void initMainWindow() {
        mainWindow = new JFrame("Trader");    //创建一个JFrame对象
        mainWindow.setLocation(200, 100);
        ImageIcon imageIcon = new ImageIcon(ClassLoader.getSystemResource("gui/img/titleIcon.png"));
        mainWindow.setIconImage(imageIcon.getImage());
        mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    }

    public void initGlobalStyle() throws UnsupportedLookAndFeelException {
        UIManager.setLookAndFeel(new MetalLookAndFeel());
        // UIManager.setLookAndFeel(new PgsLookAndFeel());

        UIDefaults defs = UIManager.getDefaults();
        defs.put("TextPane.background", new ColorUIResource(themeColor));
        defs.put("TextPane.inactiveBackground", new ColorUIResource(themeColor));
        defs.put("SplitPane.background", new ColorUIResource(themeColor));
        defs.put("SplitPane.inactiveBackground", new ColorUIResource(themeColor));
        defs.put("Panel.background", new ColorUIResource(themeColor));
        defs.put("Panel.inactiveBackground", new ColorUIResource(themeColor));
        //        System.out.println(JSONUtil.toJsonPrettyStr(JSONUtil.parse(defs)));
        defs.put("SplitPane.background", new ColorUIResource(themeColor));
    }

}