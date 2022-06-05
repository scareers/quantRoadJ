package com.scareers.gui.ths.simulation.interact.gui;

import cn.hutool.core.lang.Console;
import lombok.Data;
import org.jdesktop.swingx.JXList;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;

/**
 * description: 智能查找功能实现, 对话框!
 * 对话框单例; 设置在静态属性
 *
 * @author: admin
 * @date: 2022/6/6/006-06:58:36
 */
@Data
public class SmartFindDialog extends JDialog {
    // 1.标志智能查找模式; 在该模式开始时, 一下按键监听生效, 否则不生效
    public static volatile boolean smartFinderMode = true; // 可通过修改此值, 关闭只能查找概念
    // 2.查找map; 切换功能应当清空它, 并填充它;
    public static volatile Hashtable<String, Object> findingMap = new Hashtable<>();
    // 3.标志进入了单次只能查找模式, 该flag在监听到第一个字母后设置true, 在一次查找退出后, 设置false!
    public static volatile boolean smartFindingEntered = false; // 单例单锁逻辑

    public static SmartFindDialog INSTANCE; // 静态属性单例逻辑

    /*
    静态数据
     */
    public static int widthDefault0 = 300;
    public static int heightDefault0 = 400;
    public static HashSet<Integer> smartFinderStartKeySet; // A-Z, 0-9; 监听到这些键, 才开启 一次只能查找! 初始化后一般不变

    static {
        initSmartFinderStartKeySet();
    }

    TraderGui parentS; // 自定义属性, 不使用父类 owner属性
    JPanel contentPanel;
    JTextField findInput; // 查找内容输入框!
    JXList findResList; // 显示瞬间的查找结果, 并默认选中第一项, 并且监听 enter键, 确定选择

    public SmartFindDialog(TraderGui parent, String title, boolean modal) {
        super(parent, title, modal);
        this.parentS = parent;
        this.setSize(widthDefault0, heightDefault0); // 随后只需要设置 location 即可!
        this.setResizable(true);

        initContentPanel();
        this.setContentPane(contentPanel);
    }


    public void initContentPanel() {
        contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout());

        findInput = new JTextField();
        findInput.setText("测试内容");
        findInput.setPreferredSize(new Dimension(widthDefault0, 40));
        findResList = new JXList();

        contentPanel.add(findInput, BorderLayout.NORTH);
        contentPanel.add(findResList, BorderLayout.CENTER);
    }

    /**
     * 重置位置, 将自身放在父亲右下角!
     */
    public void resetLocation() {
        if (this.parentS == null) {
            return;
        }

        this.setLocation(parentS.getX() + parentS.getWidth() - this.getWidth(),
                parentS.getY() + parentS.getHeight() - this.getHeight());
    }

    /**
     * 可作为只能查找开始一次查找的键 集合;  A-Z + 0-9; 其他键不可作为智能查找初始! 且不可组合键
     */
    public static void initSmartFinderStartKeySet() {
        java.util.List<Integer> keys = new ArrayList<>();
        for (int i = 0x41; i <= 0x5A; i++) { // A-Z, 无小写!
            keys.add(i);
        }
        for (int i = 0x30; i <= 0x39; i++) { // 0-9
            keys.add(i);
        }
        smartFinderStartKeySet = new HashSet<>(keys);
    }


    /**
     * @key3 : 全局唯一智能查找器, 在不同的功能区情况下, 请自行刷新 查找 Map! 一律返回查找结果 Object 类型;
     * 不同功能gui下, 显然返回结果可能不同 !
     * 全局只能查找器, 类似于同花顺 智能查找!
     */
    public static void addGlobalSmartFinder() {
        // 1.控件初始化, 使用 对话框, + 内部 Panel


        // 添加全局查找
        KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        manager.addKeyEventPostProcessor(new KeyEventPostProcessor() {
            @Override
            public synchronized boolean postProcessKeyEvent(KeyEvent e) {
                // 1.需要开启智能查找模式设置下生效!
                if (!smartFinderMode) {
                    return true;
                }
                // 2.只监控按下事件, 无视释放事件
                if (e.getID() != KeyEvent.KEY_PRESSED) { // 需要按下事件, 否则一次按放回触发两次;
                    return true;
                }
                // 3. 3大组合键  任意按下状态, 也不会进入智能查找模式
                if (
                        (e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0
                                || (e.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) != 0
                                || (e.getModifiersEx() & KeyEvent.ALT_DOWN_MASK) != 0

                ) {
                    return true;
                }
                // 4. 如果非 26 + 10 字符, 无视掉!
                if (!smartFinderStartKeySet.contains(e.getKeyCode())) {
                    return true;
                }

                // 5. 进入或者维持智能查找模式! -- 因为各种输入框, 也会除非本回调, 因此进入只能查找模式后, 不继续监听!
                if (smartFindingEntered) {
                    Console.log("此前已经进入查找模式, 需要退出才能再次进入!");
                } else { // 首次按下字母数字键, 进入智能查找模式! 设置flag!
                    smartFindingEntered = true;
                    JOptionPane.showMessageDialog(null, "按键进入智能查找模式: " + KeyEvent.VK_ENTER);
                }


                return true;
            }
        });
    }

}
