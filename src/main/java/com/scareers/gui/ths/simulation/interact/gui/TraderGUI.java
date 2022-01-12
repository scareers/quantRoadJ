package com.scareers.gui.ths.simulation.interact.gui;

import cn.hutool.json.JSONUtil;
import com.scareers.gui.ths.simulation.interact.gui.component.JButtonV;
import com.scareers.gui.ths.simulation.interact.gui.component.fivecore.CorePanel;
import com.scareers.gui.ths.simulation.interact.gui.component.forlog.JDisplayForLog;
import com.scareers.gui.ths.simulation.interact.gui.factory.ButtonFactory;
import com.scareers.gui.ths.simulation.interact.gui.layout.VerticalFlowLayout;
import com.scareers.gui.ths.simulation.interact.gui.ui.TabbedPaneUIS;
import com.scareers.gui.ths.simulation.trader.Trader;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.colorThemeMain;
import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.colorThemeMinor;
import static com.scareers.gui.ths.simulation.interact.gui.util.GuiCommonUtil.createPlaceholderLabel;


/**
 * description: 模拟交易后台gui, 主要用于查看模拟交易进行各种状况.
 *
 * @key1 gui风格模仿idea;
 * @key2 主界面子组件:
 * 1.菜单栏 --> 常规实现
 * 2.工具栏 --> 常规实现
 * 3.路径栏(状态栏1) --> 多级label
 * 4.状态栏, 右侧含按钮 --> 常规实现
 * 5.核心组件: CorePanel --> 包含 左/右/下 功能按钮区. 以及 主功能区(idea项目文件树) + 主显示区(idea editor)
 * @author: admin
 * @date: 2022/1/4/004-17:03:03
 * @see CorePanel
 */
@Setter
@Getter
public class TraderGUI extends JFrame {
    public static int screenW; // 除去任务栏, 可用的全屏宽度/高度, 暂时未使用
    public static int screenH;

    static {
        initScreenBounds();
        try {
            initGlobalStyle();
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        main0(args);
    }

    public static void main0(String[] agrs) throws Exception {
        TraderGUI gui = new TraderGUI();
        gui.showAndStartTrader(); // 显示并开始程序
    }


    String iconPath = "gui/img/titleIcon0.png";
    JLabel pathLabel; // 路径栏, 待完善
    JLabel statusBar; // 状态栏, 待完善

    JPanel corePanel; // 核心组件

    public TraderGUI() throws Exception {
        super();
        init(); // 组装子控件
    }

    public void init() {
        this.setLayout(new BorderLayout());
        this.setUndecorated(false); // 标题栏显示,true 则类似专注模式
        ImageIcon imageIcon = new ImageIcon(ClassLoader.getSystemResource(iconPath));
        this.setIconImage(imageIcon.getImage()); // 图标

        // this.setBounds(0, 0, screenW, screenH); // 可不需要, 直接最大化
        this.setExtendedState(JFrame.MAXIMIZED_BOTH); // 直接最大化
        this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE); // 退出回调, 建议 HIDE_ON_CLOSE / EXIT_ON_CLOSE

        pathLabel = new JLabel("paths: ");
        pathLabel.setFont(new Font("宋体", Font.BOLD, 15));
        pathLabel.setForeground(Color.RED);
        pathLabel.setPreferredSize(new Dimension(100, 20));

        statusBar = new JLabel("Running");
        statusBar.setFont(new Font("宋体", Font.BOLD, 15));
        statusBar.setForeground(Color.RED);
        statusBar.setPreferredSize(new Dimension(100, 20));

        corePanel = buildCorePanel();
        this.add(pathLabel, BorderLayout.NORTH);
        this.add(corePanel, BorderLayout.CENTER);
        this.add(statusBar, BorderLayout.SOUTH);
    }

    private void showAndStartTrader() throws Exception {
        this.pack();
        this.setVisible(true);
        Trader.main0(null);
    }


    /**
     * 核心内容 Panel. 含 左右下 3个按钮列, 以及 JSplitPanel 的项目+编辑器 主界面
     *
     * @return
     */
    public JPanel buildCorePanel() {
        return new CorePanel(100, 10, 30, 30, 30,
                Arrays.asList(ButtonFactory.getButton("对象查看", true)),
                Arrays.asList(ButtonFactory.getButton("数据查看", true)),
                Arrays.asList(ButtonFactory.getButton("数据库", true)),
                Arrays.asList(ButtonFactory.getButton("书签", true)),
                Arrays.asList(ButtonFactory.getButton("终端命令行")),
                Arrays.asList(ButtonFactory.getButton("命令行2"))
        );
    }

    /**
     * 初始化各项默认 LookAndFeel设置, 可依需要修改
     *
     * @throws UnsupportedLookAndFeelException
     */
    public static void initGlobalStyle() throws UnsupportedLookAndFeelException {
        UIManager.setLookAndFeel(new MetalLookAndFeel()); // 重写ui类, 继承 Metal相关. 此为默认lookandfeel, 显式设置一下
        UIDefaults defs = UIManager.getDefaults();

        defs.put("TextPane.background", new ColorUIResource(colorThemeMain));
        defs.put("TextPane.inactiveBackground", new ColorUIResource(colorThemeMain));
        defs.put("SplitPane.background", new ColorUIResource(colorThemeMain));
        defs.put("SplitPane.inactiveBackground", new ColorUIResource(colorThemeMain));
        defs.put("TabbedPane.background", new ColorUIResource(colorThemeMinor));

        defs.put("Button.shadow", colorThemeMain);
        defs.put("Button.select", colorThemeMain);
        defs.put("Button.focus", colorThemeMain);
        defs.put("Button.background", new ColorUIResource(colorThemeMain));
        defs.put("Button.foreground", new ColorUIResource(colorThemeMain));//
        defs.put("Button.margin", new InsetsUIResource(2, 2, 2, 3));// 因为有竖直button,这里margin初始化
        defs.put("Button.gradient", null);// 将渐变去除

        defs.put("Panel.background", new ColorUIResource(colorThemeMinor));
        defs.put("Panel.inactiveBackground", new ColorUIResource(colorThemeMinor));
        // System.out.println(JSONUtil.toJsonPrettyStr(JSONUtil.parse(defs)));
    }

    /**
     * 假设状态栏在下方. 初始化可用的全屏幕的 宽,高
     */
    public static void initScreenBounds() {
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration());
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        screenW = screenSize.width - insets.left - insets.right;
        screenH = screenSize.height - insets.top - insets.bottom;
    }
}
