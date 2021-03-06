package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.wencai;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONArray;
import com.scareers.datasource.ths.wencai.WenCaiApi;
import com.scareers.datasource.ths.wencai.WenCaiResult;
import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.PlanReviewDateTimeDecider;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.MainDisplayWindow;
import com.scareers.gui.ths.simulation.interact.gui.component.simple.JXFindBarS;
import com.scareers.gui.ths.simulation.interact.gui.factory.ButtonFactory;
import com.scareers.gui.ths.simulation.interact.gui.ui.BasicScrollBarUIS;
import com.scareers.gui.ths.simulation.interact.gui.util.GuiCommonUtil;
import com.scareers.gui.ths.simulation.interact.gui.util.ManiLog;
import com.scareers.tools.stockplan.indusconcep.bean.IndustryConceptThsOfPlan;
import com.scareers.tools.stockplan.indusconcep.bean.dao.IndustryConceptThsOfPlanDao;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import lombok.Getter;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.JXRadioGroup;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Vector;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.*;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS;

/**
 * description: ????????????api; ????????????, ?????????????????????, ??? table??????; table???????????????, ???????????? ????????????/?????? ???
 * ??????????????????
 *
 * @author: admin
 * @date: 2022/3/28/028-21:20:34
 */
@Getter
public class WenCaiApiPanelForPlan extends DisplayPanel {
    private static WenCaiApiPanelForPlan INSTANCE;

    public static WenCaiApiPanelForPlan getInstance(MainDisplayWindow mainDisplayWindow) {
        if (INSTANCE == null) {
            INSTANCE = new WenCaiApiPanelForPlan(mainDisplayWindow);
        }
        INSTANCE.update();
        return INSTANCE;
    }

    MainDisplayWindow mainDisplayWindow;
    protected JScrollPane jScrollPane;
    protected JXTable jTable;

    JXPanel wenCaiSearchPanel;

    // ????????????????????????????????? NORTH, ?????????panel

    public WenCaiApiPanelForPlan(MainDisplayWindow mainDisplayWindow) {
        this.mainDisplayWindow = mainDisplayWindow;
        this.setBorder(null);
        this.setLayout(new BorderLayout());

        initTableScrollPanel();
        initSearchAndConditionParsePanel();

        this.add(wenCaiSearchPanel, BorderLayout.NORTH);
        this.add(jScrollPane, BorderLayout.CENTER);
        this.update();
    }

    JXFindBarS jxFindBarSForTable;
    JLabel tableLenthLabel; // ??????????????????

    private void initSearchAndConditionParsePanel() {
        wenCaiSearchPanel = new JXPanel();
        wenCaiSearchPanel.setLayout(new BorderLayout());
        wenCaiSearchPanel.setBorder(BorderFactory.createLineBorder(Color.black, 1, true));

        // 1.?????????????????????
        JPanel questionPanel = new JPanel();
        questionPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        JTextField questionTextField = new JTextField();
        questionTextField.setColumns(80);
        questionTextField.setBackground(Color.black);
        questionTextField.setForeground(Color.gray);
        questionTextField.setCaretColor(Color.red);
        questionTextField.setFont(new Font("????????????", Font.PLAIN, 20));
        questionTextField.setBorder(null);
        questionTextField.setText("?????????????????????"); // todo: ??????
        questionTextField.requestFocus();

        JButton findButton = ButtonFactory.getButton("??????!  ");
        findButton.setFont(new Font("????????????", Font.PLAIN, 22));
        findButton.setForeground(Color.red);
        questionTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    findButton.doClick(); // ??????????????????
                }
            }
        });
        JButton clearButton = ButtonFactory.getButton("??????");
        clearButton.setFont(new Font("????????????", Font.PLAIN, 20));
        clearButton.setForeground(Color.red);
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                questionTextField.setText("");
            }
        });

        JButton openWebButton = ButtonFactory.getButton("??????web");
        openWebButton.setFont(new Font("????????????", Font.PLAIN, 20));
        openWebButton.setForeground(Color.red);


        questionPanel.add(findButton);
        questionPanel.add(questionTextField);
        questionPanel.add(clearButton);
        questionPanel.add(openWebButton);

        // 1.2: ??????: ???????????????????????????;
        JPanel jPanelQueryContainer = new JPanel(); // ????????? questionPanel,???????????????
        JXRadioGroup<String> jxRadioGroup = new JXRadioGroup<>(WenCaiApi.TypeStr.allTypeNames);
        // jxRadioGroup.setBackground(COLOR_THEME_MINOR); // ??????
        for (int i = 0; i < jxRadioGroup.getChildButtonCount(); i++) {
            jxRadioGroup.getChildButton(i).setBackground(COLOR_THEME_MINOR);
            jxRadioGroup.getChildButton(i).setForeground(Color.green);
        }
        jxRadioGroup.getChildButton(0).setSelected(true); // ????????????
        // questionPanel.setBorder(BorderFactory.createLineBorder(Color.red,1));


        jPanelQueryContainer.setPreferredSize(new Dimension(300, 80));
        JLabel placeHolderLabel = new JLabel();
        placeHolderLabel.setPreferredSize(new Dimension(90, 60));
        jPanelQueryContainer.setLayout(new BorderLayout());
        jPanelQueryContainer.add(questionPanel, BorderLayout.NORTH);
        jPanelQueryContainer.add(jxRadioGroup, BorderLayout.CENTER);
        jPanelQueryContainer.add(placeHolderLabel, BorderLayout.WEST);


        // 2.?????????????????????
        JPanel conditionsPanel = new JPanel();
        conditionsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        conditionsPanel.setPreferredSize(new Dimension(2560, 80));

        // 3.jTable gui??????????????????
        JPanel tableRelatePanel = new JPanel();
        tableRelatePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        jxFindBarSForTable = new JXFindBarS(Color.pink);

        tableLenthLabel = new JLabel();
        tableLenthLabel.setForeground(Color.green);
        tableRelatePanel.add(jxFindBarSForTable);
        tableRelatePanel.add(tableLenthLabel);

        // 4.??????
        wenCaiSearchPanel.add(jPanelQueryContainer, BorderLayout.NORTH);
        wenCaiSearchPanel.add(conditionsPanel, BorderLayout.CENTER);
        wenCaiSearchPanel.add(tableRelatePanel, BorderLayout.SOUTH);

        openWebButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object currentSelectType = WenCaiApi.TypeStr.allTypesMap.get(jxRadioGroup.getSelectedValue());
                String typeStr = "";
                if (currentSelectType != null) {
                    typeStr = currentSelectType.toString();
                }
                String encode = URLEncoder.encode(questionTextField.getText(), StandardCharsets.UTF_8);
                String url;
                if ("".equals(typeStr)) {
                    url = StrUtil.format("http://www.iwencai.com/unifiedwap/result?w={}",
                            encode
                    );
                } else {
                    url = StrUtil.format("http://www.iwencai.com/unifiedwap/result?w={}&querytype={}",
                            encode, typeStr
                    );
                }
                CommonUtil.openUrlWithDefaultBrowser(url);
            }
        });
        WenCaiApiPanelForPlan panelTemp = this;
        findButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String question = questionTextField.getText();
                JDialog dialog = new JDialog(TraderGui.INSTANCE, "???????????????...", true);
                ThreadUtil.execAsync(new Runnable() {
                    @Override
                    public void run() {
                        Object currentSelectType = WenCaiApi.TypeStr.allTypesMap.get(jxRadioGroup.getSelectedValue());
                        String typeStr = "";
                        if (currentSelectType != null) {
                            typeStr = currentSelectType.toString();
                        }
                        WenCaiResult wenCaiResult = WenCaiApi.wenCaiQueryResult(question, typeStr);
                        if (wenCaiResult == null) {
                            return;
                        }

                        JSONArray conditionsParsed = wenCaiResult.getChunksInfo();
                        conditionsPanel.removeAll(); // ????????????, ????????????
                        if (conditionsParsed != null) {
                            for (Object condition : conditionsParsed) {
                                JLabel conditionLabel = new JLabel(condition.toString());
                                conditionLabel.setForeground(Color.red);
                                conditionLabel.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
                                conditionLabel.setBackground(Color.black);
                                conditionLabel.setFont(new Font("????????????", Font.PLAIN, 18));

                                JPanel jPanel = new JPanel();
                                jPanel.setLayout(new BorderLayout());
                                jPanel.setBorder(BorderFactory.createLineBorder(Color.black, 1, true));
                                jPanel.add(conditionLabel, BorderLayout.CENTER);
                                conditionsPanel.add(jPanel);
                            }
                        }
                        panelTemp.updateTable(wenCaiResult.getDfResult());
                        mainDisplayWindow.flushBounds(); // ?????????, ??????????????????label??????
                        ManiLog.put(StrUtil.format("??????????????????: {}", question));
                        dialog.dispose();
                        questionTextField.requestFocus();
                    }
                }, true);
                JPanel jPanel1 = new JPanel();
                jPanel1.setLayout(new BorderLayout());
                JLabel jLabel = new JLabel(GuiCommonUtil.buildDialogShowStr("????????????????????????:", question));
                jPanel1.add(jLabel, BorderLayout.CENTER);
                dialog.setContentPane(jPanel1);
                dialog.setSize(new Dimension(500, 300));
                dialog.setLocationRelativeTo(dialog.getParent());
                dialog.setVisible(true);

            }
        });


    }

    private void initTableScrollPanel() {
        jScrollPane = new JScrollPane();
        jScrollPane.setBorder(null);
        JLabel label = new JLabel("???????????????"); // ??????????????????
        label.setForeground(Color.red);
        jScrollPane.setViewportView(label); // ??????
//        jScrollPane.getViewport().setBackground(COLOR_THEME_MINOR);
        jScrollPane.getViewport().setBackground(COLOR_CHART_BG_EM);
        BasicScrollBarUIS
                .replaceScrollBarUI(jScrollPane, COLOR_THEME_TITLE, COLOR_SCROLL_BAR_THUMB); // ??????????????? barUi
        jScrollPane.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_ALWAYS); // ???????????????
        jScrollPane.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS); // ???????????????

        JLabel tempLabel = new JLabel("????????????");
        tempLabel.setForeground(Color.red);
        jScrollPane.setViewportView(tempLabel);
    }


    /**
     * @return
     */
    public void updateTable(DataFrame newData) {
        this.newDf = newData;
        this.update();
    }

    protected DataFrame<Object> newDf; // ????????????df??????

    public static HashSet<String> highlightColNameSet = new HashSet<>(Arrays.asList(
            "????????????", "????????????"
    ));

    JPopupMenu popupForTable; // ??????????????????
    int popupPointRow = -1; // ???????????????????????????, ??????????????????; ???JTable????????????????????????

    private void initPopupForTable() {
        popupForTable = new JPopupMenu();
        popupForTable.setBackground(Color.white);
//        popupForTable.add(new JSeparator());
        JMenuItem generateIndustryItem1 = new JMenuItem("????????????[?????????]");
        generateIndustryItem1.setForeground(Color.black);
        generateIndustryItem1.setBackground(Color.white);
        generateIndustryItem1.setBorder(null);
        generateIndustryItem1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (popupPointRow == -1 || jTable == null) {
                    return;
                }
                RowSorter<? extends TableModel> rowSorter = jTable.getRowSorter();
                int index = rowSorter.convertRowIndexToModel(popupPointRow); // ????????????row; ???????????? newDf ?????????

                String name = null;
                try {
                    name = newDf.get(index, "????????????").toString();
                } catch (Exception ex) {
                    ManiLog.put("????????????[?????????]??????: ????????????????????????");
                    return;
                }
                IndustryConceptThsOfPlan bean;
                try {
                    bean = IndustryConceptThsOfPlanDao
                            .getOrInitBeanForPlan(name, PlanReviewDateTimeDecider.getUniqueDatetime(),
                                    IndustryConceptThsOfPlan.Type.INDUSTRY);
                    try {
                        IndustryConceptThsOfPlanDao.syncEditableAttrByLatestNSameBean(bean,
                                IndustryConceptThsOfPlanDao
                                        .decideDateStrForPlan(PlanReviewDateTimeDecider.getUniqueDatetime()), 10);
                    } catch (SQLException ex) {
                        ManiLog.put(StrUtil.format("???????????????????????????: {}", bean.getName()));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    ManiLog.put("????????????[?????????]??????: ??????bean????????????!");
                    return;
                }
                ManiLog.put(StrUtil.format("????????????[?????????]??????: {}", bean.getName()));
            }
        });
        JMenuItem generateIndustryItem2 = new JMenuItem("????????????[????????????]");
        generateIndustryItem2.setForeground(Color.black);
        generateIndustryItem2.setBackground(Color.white);
        generateIndustryItem2.setBorder(null);
        generateIndustryItem2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ThreadUtil.execAsync(new Runnable() {
                    @Override
                    public void run() {

                        if (popupPointRow == -1 || jTable == null) {
                            return;
                        }
                        int[] selectedRows = jTable.getSelectedRows();
                        for (int selectedRow : selectedRows) {
                            RowSorter<? extends TableModel> rowSorter = jTable.getRowSorter();
                            int index = rowSorter.convertRowIndexToModel(selectedRow); // ????????????row; ???????????? newDf ?????????
                            String name = null;
                            try {
                                name = newDf.get(index, "????????????").toString();
                            } catch (Exception ex) {
                                ManiLog.put("????????????[?????????]??????: ????????????????????????");
                                continue;
                            }
                            IndustryConceptThsOfPlan bean;
                            try {
                                bean = IndustryConceptThsOfPlanDao
                                        .getOrInitBeanForPlan(name, PlanReviewDateTimeDecider.getUniqueDatetime(),
                                                IndustryConceptThsOfPlan.Type.INDUSTRY);
                                try {
                                    IndustryConceptThsOfPlanDao.syncEditableAttrByLatestNSameBean(bean,
                                            IndustryConceptThsOfPlanDao
                                                    .decideDateStrForPlan(
                                                            PlanReviewDateTimeDecider.getUniqueDatetime()), 10);
                                } catch (SQLException ex) {
                                    ManiLog.put(StrUtil.format("???????????????????????????: {}", bean.getName()));
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                ManiLog.put("????????????[?????????]??????: ??????bean????????????!");
                                continue;
                            }
                            ManiLog.put(StrUtil.format("????????????[?????????]??????: {}", bean.getName()));
                        }
                    }
                }, true);

            }
        });
        popupForTable.add(generateIndustryItem1);
        popupForTable.add(generateIndustryItem2);
        popupForTable.add(new JSeparator());

        JMenuItem generateConceptItem1 = new JMenuItem("????????????[?????????]");
        generateConceptItem1.setForeground(Color.red);
        generateConceptItem1.setBackground(Color.white);
        generateConceptItem1.setBorder(null);
        generateConceptItem1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (popupPointRow == -1 || jTable == null) {
                    return;
                }
                RowSorter<? extends TableModel> rowSorter = jTable.getRowSorter();
                int index = rowSorter.convertRowIndexToModel(popupPointRow); // ????????????row; ???????????? newDf ?????????

                String name = null;
                try {
                    name = newDf.get(index, "????????????").toString();
                } catch (Exception ex) {
                    ManiLog.put("????????????[?????????]??????: ????????????????????????");
                    return;
                }
                IndustryConceptThsOfPlan bean;
                try {
                    bean = IndustryConceptThsOfPlanDao
                            .getOrInitBeanForPlan(name, PlanReviewDateTimeDecider.getUniqueDatetime(),
                                    IndustryConceptThsOfPlan.Type.CONCEPT);
                    try {
                        IndustryConceptThsOfPlanDao.syncEditableAttrByLatestNSameBean(bean,
                                IndustryConceptThsOfPlanDao
                                        .decideDateStrForPlan(PlanReviewDateTimeDecider.getUniqueDatetime()), 10);
                    } catch (SQLException ex) {
                        ManiLog.put(StrUtil.format("???????????????????????????: {}", bean.getName()));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    ManiLog.put("????????????[?????????]??????: ??????bean????????????!");
                    return;
                }
                ManiLog.put(StrUtil.format("????????????[?????????]??????: {}", bean.getName()));
            }
        });
        JMenuItem generateConceptItem2 = new JMenuItem("????????????[????????????]");
        generateConceptItem2.setForeground(Color.red);
        generateConceptItem2.setBackground(Color.white);
        generateConceptItem2.setBorder(null);
        generateConceptItem2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ThreadUtil.execAsync(new Runnable() {
                    @Override
                    public void run() {

                        if (popupPointRow == -1 || jTable == null) {
                            return;
                        }
                        int[] selectedRows = jTable.getSelectedRows();
                        for (int selectedRow : selectedRows) {
                            RowSorter<? extends TableModel> rowSorter = jTable.getRowSorter();
                            int index = rowSorter.convertRowIndexToModel(selectedRow); // ????????????row; ???????????? newDf ?????????
                            String name = null;
                            try {
                                name = newDf.get(index, "????????????").toString();
                            } catch (Exception ex) {
                                ManiLog.put("????????????[?????????]??????: ????????????????????????");
                                continue;
                            }
                            IndustryConceptThsOfPlan bean;
                            try {
                                bean = IndustryConceptThsOfPlanDao
                                        .getOrInitBeanForPlan(name, PlanReviewDateTimeDecider.getUniqueDatetime(),
                                                IndustryConceptThsOfPlan.Type.CONCEPT);
                                try {
                                    IndustryConceptThsOfPlanDao.syncEditableAttrByLatestNSameBean(bean,
                                            IndustryConceptThsOfPlanDao
                                                    .decideDateStrForPlan(
                                                            PlanReviewDateTimeDecider.getUniqueDatetime()), 10);
                                } catch (SQLException ex) {
                                    ManiLog.put(StrUtil.format("???????????????????????????: {}", bean.getName()));
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                ManiLog.put("????????????[?????????]??????: ??????bean????????????!");
                                continue;
                            }
                            ManiLog.put(StrUtil.format("????????????[?????????]??????: {}", bean.getName()));
                        }
                    }
                }, true);
            }
        });

        popupForTable.add(generateConceptItem1);
        popupForTable.add(generateConceptItem2);
        popupForTable.add(new JSeparator());

        JMenuItem cancel = new JMenuItem("??????");
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                popupForTable.setVisible(false);
            }
        });
        popupForTable.add(new JSeparator());
        popupForTable.add(cancel);
    }

    @Override
    public void update() {
        if (newDf == null) {
            return;
        }

        // ????????????????????????!

        Vector<Vector<Object>> datas = new Vector<>();
        for (int i = 0; i < newDf.length(); i++) {
            datas.add(new Vector<>(newDf.row(i)));
        }
        Vector<Object> cols = new Vector<>(newDf.columns());
        DefaultTableModel model = new DefaultTableModel(datas, cols) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // ????????????!
            }
        };
        jTable = new JXTable();
        jTable.setModel(model);
        removeEnterKeyDefaultAction();
        jTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int row = jTable.getSelectedRow();

            }
        });
        initJTableStyle();
        jScrollPane.setViewportView(jTable); // ????????????"???????????????", ???????????????
        jTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        fitTableColumns(jTable);

        jTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent me) {
                if (SwingUtilities.isRightMouseButton(me)) {
                    popupPointRow = jTable.rowAtPoint(me.getPoint());
                    if (popupPointRow != -1) {
                        // final int column = jTable.columnAtPoint(me.getPoint());
                        if (popupForTable == null) {
                            initPopupForTable();
                        }
                        popupForTable.show(me.getComponent(), me.getX(), me.getY());
                    }
                }
            }
        });


        for (Object column : newDf.columns()) {
            if (highlightColNameSet.contains(column)) {
                DefaultTableCellRenderer cellRendererOfTitle = new DefaultTableCellRenderer();
                cellRendererOfTitle.setForeground(COLOR_LIST_RAISE_EM);
                jTable.getColumn(column).setCellRenderer(cellRendererOfTitle);
            }
        }

        jxFindBarSForTable.setSearchable(jTable.getSearchable());
        tableLenthLabel.setText(StrUtil.format("  ????????????: {}", newDf.length()));

//        initDefaultRenderer();
//        for (int i = 0; i < model.getColumnCount(); i++) {
//            String columnName = model.getColumnName(i);
//            jTable.getColumn(columnName).setCellRenderer(dataCellRenderer);
//        }
    }

    /**
     * ???????????????
     */
    private void initJTableStyle() {
        initDefaultRenderer();


        // ???????????????????????????
        jTable.getTableHeader().setBackground(Color.BLACK);
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
        cellRenderer.setBackground(COLOR_LIST_BK_EM);
        cellRenderer.setForeground(COLOR_LIST_HEADER_FORE_EM);
        for (int i = 0; i < newDf.size(); i++) {
            //i???????????????
            TableColumn column = jTable.getTableHeader().getColumnModel().getColumn(i);
            column.setHeaderRenderer(cellRenderer);
            //??????????????????
            cellRenderer.setHorizontalAlignment(DefaultTableCellRenderer.CENTER);
        }

        jTable.setForeground(COLOR_LIST_FLAT_EM);
        jTable.setBackground(COLOR_LIST_BK_EM);


        jTable.setRowHeight(30);
        jTable.setFont(new Font("????????????", Font.PLAIN, 18));

    }

    DefaultTableCellRenderer dataCellRenderer;

    private void initDefaultRenderer() {
        if (dataCellRenderer == null) {
            // ?????????????????????
            dataCellRenderer = new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                               boolean hasFocus, int row, int column) {
                    Component tableCellRendererComponent = super
                            .getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    JLabel label = (JLabel) tableCellRendererComponent;

                    Double number = null;
                    try {
                        number = Double.valueOf(value.toString());
                    } catch (Exception e) {

                    }
                    if (number == null) {
                        label.setForeground(Color.white);

                    } else if (number > 0) {
                        label.setForeground(Color.red);
                    } else if (number < 0) {
                        label.setForeground(Color.green);
                    } else {
                        label.setForeground(Color.white);
                    }
                    return label;
                }
            };
        }
    }

    private void removeEnterKeyDefaultAction() {
        ActionMap am = jTable.getActionMap();
        am.getParent().remove("selectNextRowCell"); // ???????????????: ????????????????????????????????????
        jTable.setActionMap(am);
    }

    private static final Log log = LogUtil.getLogger();


    /**
     * ?????????????????????
     *
     * @param myTable
     */
    protected void fitTableColumns(JTable myTable) {
        JTableHeader header = myTable.getTableHeader();
        int rowCount = myTable.getRowCount();

        Enumeration columns = myTable.getColumnModel().getColumns();

        int dummyIndex = 0;

        while (columns.hasMoreElements()) {
//        if (columns.hasMoreElements()) {
            TableColumn column = (TableColumn) columns.nextElement();
            int col = header.getColumnModel().getColumnIndex(column.getIdentifier());
            int width = (int) myTable.getTableHeader().getDefaultRenderer()
                    .getTableCellRendererComponent(myTable, column.getIdentifier()
                            , false, false, -1, col).getPreferredSize().getWidth();
            for (int row = 0; row < rowCount; row++) {
                int preferedWidth = (int) myTable.getCellRenderer(row, col).getTableCellRendererComponent(myTable,
                        myTable.getValueAt(row, col), false, false, row, col).getPreferredSize().getWidth();
                width = Math.max(width, preferedWidth);
            }
            header.setResizingColumn(column); // ???????????????

            int actualWidth = width + myTable.getIntercellSpacing().width + 2;
            actualWidth = Math.min(700, actualWidth); // ??????????????????
            if (dummyIndex <= 20 && dummyIndex > 8) {
                column.setWidth(Math.min(actualWidth, 80)); // 12????????????????????????
            } else {
                column.setWidth(Math.max(actualWidth, 80)); // ???5
            }
//            break; // ??????????????????. ???????????????
//
//            if (dummyIndex == 5) {
//                column.setWidth(5); // ???5
//            }
//            if (dummyIndex == 8) {
//                column.setWidth(5); // ???5
//            }
//            if (dummyIndex == 9) {
//                column.setWidth(5); // ???5
//            }


            dummyIndex++;
        }
    }

    public void showInMainDisplayWindow() {
        // 9.???????????????????????????
        mainDisplayWindow.setCenterPanel(this);
    }
}
