package com.scareers.gui.ths.simulation.interact.gui.component.funcs;

import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.base.FuncFrameS;
import com.scareers.gui.ths.simulation.interact.gui.component.simple.FuncButton;

import javax.swing.*;
import javax.swing.plaf.basic.BasicInternalFrameUI;
import java.awt.*;
import java.util.List;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/1/14/014-07:16:15
 */
public class DatabaseFuncWindow extends FuncFrameS {
    private static DatabaseFuncWindow INSTANCE;

    public static DatabaseFuncWindow getInstance() {
        return INSTANCE;
    }

    public static DatabaseFuncWindow getInstance(Type type, String title, TraderGui mainWindow,
                                                 FuncButton belongBtn, boolean resizable, boolean closable,
                                                 boolean maximizable,
                                                 boolean iconifiable, int autoMaxWidthOrHeight,
                                                 int autoMinWidthOrHeight,
                                                 double preferScale,
                                                 int funcToolsWidthOrHeight, boolean halfWidthOrHeight, Integer layer) {
        if (INSTANCE == null) {
            INSTANCE = new DatabaseFuncWindow(type, title, mainWindow, belongBtn, resizable, closable, maximizable,
                    iconifiable, autoMaxWidthOrHeight, autoMinWidthOrHeight, preferScale, funcToolsWidthOrHeight,
                    halfWidthOrHeight, layer);
            ((BasicInternalFrameUI) INSTANCE.getUI()).setNorthPane(null);
        }
        return INSTANCE;
    }

    private DatabaseFuncWindow(Type type, String title, TraderGui mainWindow,
                              FuncButton belongBtn, boolean resizable, boolean closable, boolean maximizable,
                              boolean iconifiable, int autoMaxWidthOrHeight, int autoMinWidthOrHeight,
                              double preferScale,
                              int funcToolsWidthOrHeight, boolean halfWidthOrHeight, Integer layer) {
        super(type, title, mainWindow, belongBtn, resizable, closable, maximizable, iconifiable,
                autoMaxWidthOrHeight,
                autoMinWidthOrHeight, preferScale, funcToolsWidthOrHeight, halfWidthOrHeight, layer);
    }

    @Override
    public void initCenterPanel() { // ????????????
        JLabel label = new JLabel("???????????????");
        JPanel jPanel = new JPanel();
        jPanel.add(label);
        setCenterPanel(jPanel);
    }

    @Override
    protected List<FuncButton> getToolButtons1() { // ??????????????????(????????????)
        return super.defaultToolsButtonList1();
    }

    @Override
    protected List<FuncButton> getToolButtons2() {
        return super.defaultToolsButtonList2();
    }
}
