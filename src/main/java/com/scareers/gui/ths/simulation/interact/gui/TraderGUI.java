package com.scareers.gui.ths.simulation.interact.gui;

import com.scareers.gui.ths.simulation.interact.gui.component.JButtonV;
import com.scareers.gui.ths.simulation.interact.gui.component.forlog.JDisplayForLog;
import com.scareers.gui.ths.simulation.interact.gui.layout.VerticalFlowLayout;
import com.scareers.gui.ths.simulation.interact.gui.ui.TabbedPaneUIS;
import com.scareers.gui.ths.simulation.trader.Trader;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.*;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.themeColor;


/**
 * description: 模拟交易后台gui, 主要用于查看模拟交易进行各种状况.
 *
 * @key1 gui风格模仿idea;
 * @key2 主界面子组件:
 * 1.菜单栏, --> 常规实现
 * 2.工具栏 --> 常规实现
 * 3.路径栏(状态栏1) --> 多级label+> , 容器FlowLayout
 * 4.右侧边功能栏,可调宽度 JTabbedPane, 各panel为子功能
 * --> 内容控件或许是 JInternalFrame 或者 自定义(例如按钮容器+内容容器) , 主要含功能最小化.
 * 5.下边功能栏,可调高度 JTabbedPane , 各panel为子功能.
 * --> 内容控件或许是 JInternalFrame 或者 自定义(例如按钮容器+内容容器) , 主要含功能最小化.
 * 6.状态栏, 右侧含按钮 --> 简单容器放在下即可, 添加按钮
 * 7.左功能栏 + 主编辑器,  占据主要界面. 左功能栏可关闭
 * --> 标准 JSplitPane, 左功能栏可关闭. 编辑框不可最小化. 编辑框为 JTabbedPane,动态增加tab
 * @key3 主界面主布局采用  JDesktopPane / JLayeredPane
 * 使得 右/下 功能栏可以叠加到 主界面(左功能+编辑器)之上.
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
        jTabbedPane.addTab("Terminal", jDisplayForLog);
//        JInternalFrame jInternalFrame = new JInternalFrame("Terminal", true, true, true);
//        jInternalFrame.add(jDisplayForLog);
//        jInternalFrame.setBorder(null);
//        jInternalFrame.setForeground(themeColor);
//        jInternalFrame.setBackground(themeColor);
//        jTabbedPane.addTab("Terminal", jInternalFrame);

        jTabbedPane.addTab("Terminal2", new Label("测试 tab"));
        jSplitPane.setBottomComponent(jTabbedPane);

        JLabel label = new JLabel("Running");
        label.setFont(new Font("宋体", Font.BOLD, 15));
        label.setForeground(Color.RED);

        JLabel pathLabel = new JLabel("paths: ");
        pathLabel.setFont(new Font("宋体", Font.BOLD, 15));
        pathLabel.setForeground(Color.RED);


        JPanel corePanel = buildCorePanel();
        corePanel.add(jSplitPane, BorderLayout.CENTER);

        mainWindow.add(label, BorderLayout.NORTH);
        mainWindow.add(corePanel, BorderLayout.CENTER);
        mainWindow.add(pathLabel, BorderLayout.SOUTH);

        mainWindow.pack();
        mainWindow.setVisible(true);
        Trader.main0(null);
    }

    /**
     * 核心内容 Panel. 含 左右下 3个按钮列, 以及 JSplitPanel 的项目+编辑器 主界面
     *
     * @return
     */
    public JPanel buildCorePanel() {
        JPanel corePanel = new JPanel();
        corePanel.setLayout(new BorderLayout());

        JPanel leftTools = new JPanel(); // 工具栏包含2个Panel, 一个左浮动, 一个右浮动
        leftTools.setLayout(new BoxLayout(leftTools, BoxLayout.Y_AXIS)); // 上下
        leftTools.setPreferredSize(new Dimension(20, 100)); // 定宽
        JPanel panel1 = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 0));  // 上, 上浮动
        JPanel panel2 = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.BOTTOM, 0, 0)); // 下, 下浮动

        JButton projectButton = new JButtonV("对象查看");
        panel1.add(projectButton);
        JButton favoritesButton = new JButtonV("状态查看");
        panel2.add(favoritesButton);

        leftTools.add(panel1);
        leftTools.add(panel2);

        JPanel rightTools = new JPanel(); // 工具栏包含2个Panel, 一个左浮动, 一个右浮动
        rightTools.setLayout(new BoxLayout(rightTools, BoxLayout.Y_AXIS)); // 上下
        rightTools.setPreferredSize(new Dimension(20, 100)); // 定宽
        JPanel panel3 = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 0));  // 上, 上浮动
        JPanel panel4 = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.BOTTOM, 0, 0)); // 下, 下浮动

        JButton databaseButton = new JButtonV("database");
        panel3.add(databaseButton);
        JButton mavenButton = new JButtonV("maven");
        panel4.add(mavenButton);

        rightTools.add(panel3);
        rightTools.add(panel4);

        JPanel bottomTools = new JPanel(); // 工具栏包含2个Panel, 一个左浮动, 一个右浮动
        bottomTools.setLayout(new BoxLayout(bottomTools, BoxLayout.X_AXIS)); // 左右
        bottomTools.setPreferredSize(new Dimension(100, 20)); // 定高
        JPanel panel5 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));  // 上, 上浮动
        JPanel panel6 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0)); // 下, 下浮动

        JButton terminalButton = new JButton("terminal");
        panel5.add(terminalButton);
        JButton runButton = new JButton("run");
        panel6.add(runButton);

        bottomTools.add(panel5);
        bottomTools.add(panel6);


        corePanel.add(leftTools, BorderLayout.WEST); // 左工具
        corePanel.add(rightTools, BorderLayout.EAST); // 右工具
        corePanel.add(bottomTools, BorderLayout.SOUTH); // 下工具


        return corePanel;
    }

    public void initMainWindow() {
        mainWindow = new JFrame("Trader");    //创建一个JFrame对象
        mainWindow.setExtendedState(JFrame.MAXIMIZED_BOTH);
        mainWindow.setLocation(200, 100);
        mainWindow.setUndecorated(false); // 标题栏显示
//        mainWindow.addKeyListener(new KeyAdapter() {
//            @Override
//            public void keyPressed(KeyEvent e) {
//                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
//                    mainWindow.setUndecorated(true); // 退出真全屏模式
//                }
//            }
//        });
        ImageIcon imageIcon = new ImageIcon(ClassLoader.getSystemResource("gui/img/titleIcon0.png"));
        mainWindow.setIconImage(imageIcon.getImage());
        mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    }

    public void initGlobalStyle() throws UnsupportedLookAndFeelException {
        UIManager.setLookAndFeel(new MetalLookAndFeel()); // 重写ui类, 继承 Metal相关
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
