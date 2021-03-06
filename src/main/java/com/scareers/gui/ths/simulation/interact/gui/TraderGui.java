package com.scareers.gui.ths.simulation.interact.gui;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.dailycrawler.CrawlerChainEm;
import com.scareers.datasource.eastmoney.dailycrawler.datas.simplenew.*;
import com.scareers.datasource.selfdb.HibernateSessionFactory;
import com.scareers.datasource.ths.dailycrawler.CrawlerChainThs;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.bond.BondGlobalSimulationPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.bond.BondReviseUtil;
import com.scareers.gui.ths.simulation.interact.gui.component.core.CorePanel;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.*;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.base.FuncFrameS;
import com.scareers.gui.ths.simulation.interact.gui.component.simple.FuncButton;
import com.scareers.gui.ths.simulation.interact.gui.factory.ButtonFactory;
import com.scareers.gui.ths.simulation.interact.gui.notify.BondBuyNotify;
import com.scareers.gui.ths.simulation.interact.gui.notify.EmPcNewsNotify;
import com.scareers.gui.ths.simulation.interact.gui.notify.NewConceptDiscover;
import com.scareers.gui.ths.simulation.interact.gui.util.GuiCommonUtil;
import com.scareers.gui.ths.simulation.trader.ConvertibleBondArbitrage;
import com.scareers.gui.ths.simulation.trader.Trader;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.log.LogUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.*;
import java.awt.event.*;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.*;
import static com.scareers.gui.ths.simulation.interact.gui.SmartFindDialog.*;
import static com.scareers.utils.CommonUtil.waitForever;


/**
 * description: ??????????????????gui, ????????????????????????????????????????????????.
 *
 * @key1 gui????????????idea;
 * @key2 ??????????????????:
 * 1.????????? --> ????????????
 * 2.????????? --> ????????????
 * 3.?????????(?????????1) --> ??????label
 * 4.?????????, ??????????????? --> ????????????
 * 5.????????????: CorePanel --> ?????? ???/???/??? ???????????????. ?????? ????????????(idea???????????????) + ????????????(idea editor)
 * @author: admin
 * @date: 2022/1/4/004-17:03:03
 * @see CorePanel
 */
@Setter
public class TraderGui extends JFrame {
    public static TraderGui INSTANCE;

    private static final Log log = LogUtil.getLogger();
    public static int screenW; // ???????????????, ?????????????????????/??????, ???????????????
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
        ThreadUtil.execAsync(new Runnable() {
            @Override
            public void run() {
                HibernateSessionFactory.getSessionFactoryOfEastMoney();
            }
        }, true);
        TraderGui gui = new TraderGui();
        INSTANCE = gui;
        gui.setVisible(true);
        gui.showSystemTray();
        waitForever();
    }


    JLabel pathLabel; // ?????????, ?????????
    JLabel statusBar; // ?????????, ?????????
    CorePanel corePanel; // ????????????
    JDesktopPane mainPane;

    private ImageIcon imageIcon; // ??????
    private TrayIcon trayIcon; // ????????????

    public TraderGui() throws Exception {
        super();
        init(); // ???????????????
        initTrayIcon();
    }

    /**
     * ?????????, ??????gui???????????? ??????????????? ??????gui??????;;
     * ?????????gui??????
     * ?????????????????? ???????????????, ????????????????????????, ??? gui?????????????????????, ???????????????????????? ????????????
     */
    public static enum FunctionGuiCurrent {
        BOND_REVISE // ??????????????????
    }

    public FunctionGuiCurrent functionGuiCurrent = null; // getter????????????, ??????public???

    public void init() {
        this.setLayout(new BorderLayout());
        this.setUndecorated(false); // ???????????????,true ?????????????????????
        imageIcon = new ImageIcon(ResourceUtil.getResource(ICON_TITLE_PATH));
        this.setIconImage(imageIcon.getImage()); // ??????

        if (MAXIMIZE_DEFAULT) {
            this.setExtendedState(JFrame.MAXIMIZED_BOTH); // ???????????????
        } else {
            centerSelf();
        }

        // ????????????, ?????? HIDE_ON_CLOSE / EXIT_ON_CLOSE/ DO_NOTHING_ON_CLOSE(??????????????????),
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        addListeners(); // ???????????????


        statusBar = new JLabel("Running");
        statusBar.setFont(new Font("??????", Font.BOLD, 15));
        statusBar.setForeground(Color.RED);
        statusBar.setPreferredSize(new Dimension(100, 20));

        corePanel = buildCorePanel();
        this.mainPane = corePanel.getMainPane();
//
        this.add(corePanel, BorderLayout.CENTER);
        this.add(statusBar, BorderLayout.SOUTH);

        // initPathLabel(); // paths ?????????,
        initMenuBar(); // ?????????

        this.pack();
    }

    public void initPathLabel() {
        pathLabel = new JLabel("paths: ");
        pathLabel.setFont(new Font("??????", Font.BOLD, 15));
        pathLabel.setForeground(Color.RED);
        pathLabel.setPreferredSize(new Dimension(100, 20));
        pathLabel.setBorder(BorderFactory.createLineBorder(Color.black, 1, false));
        this.add(pathLabel, BorderLayout.NORTH);
    }


    /*
    ???????????????
     */
    JMenuBar menuBar;
    JMenu startMenu;

    public void initMenuBar() {
        // ?????????
        menuBar = new JMenuBar();
        menuBar.setBackground(COLOR_THEME_MINOR);
        menuBar.setBorder(BorderFactory.createEmptyBorder());
        // ??????
        startMenu = new JMenu("??????");
        startMenu.setForeground(COLOR_GRAY_COMMON);
        // ?????????
        JMenuItem startTraderItem = new JMenuItem("??????Trader");
        startMenu.add(startTraderItem);
        startTraderItem.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            Trader.getAndStartInstance();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
        );

        // ?????????
        startMenu.addSeparator();
        JMenuItem bondTtsItem = new JMenuItem("????????????????????????");
        bondTtsItem.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        ThreadUtil.execAsync(new Runnable() {
                            @Override
                            public void run() {
                                ConvertibleBondArbitrage.main0();
                            }
                        }, true);
                    }
                }
        );
        startMenu.add(bondTtsItem);

        JMenuItem bondTtsItem2 = new JMenuItem("??????????????????????????????");
        bondTtsItem2.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        ThreadUtil.execAsync(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    BondBuyNotify.changeEnvironmentToActualTrading(); // ????????????
                                    BondBuyNotify.main1();
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }, true);
                    }
                }
        );
        startMenu.add(bondTtsItem2);
        JMenuItem bondTtsItem3 = new JMenuItem("????????????????????????!");
        bondTtsItem3.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        ThreadUtil.execAsync(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    BondBuyNotify.stopBroadcast();
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }, true);
                    }
                }
        );
        startMenu.add(bondTtsItem3);

        JMenuItem bondTtsItem4 = new JMenuItem("????????????????????????????????????");
        bondTtsItem4.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        ThreadUtil.execAsync(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    BondReviseUtil.recoverNuclearKeyBoardSettingToThs();
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }, true);
                    }
                }
        );
        startMenu.add(bondTtsItem4);


        JMenuItem bondTtsItem5 = new JMenuItem("????????????????????????");
        bondTtsItem5.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        ThreadUtil.execAsync(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    CrawlerChainThs.main1();
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }, true);
                    }
                }
        );
        startMenu.add(bondTtsItem5);
        JMenuItem bondTtsItem6 = new JMenuItem("?????????????????????");
        bondTtsItem6.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        ThreadUtil.execAsync(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    CrawlerChainEm.main1();
                                } catch (Exception ex) {
                                    ex.printStackTrace( )   ;
                                }
                            }
                        }, true);
                    }
                }
        );
        startMenu.add(bondTtsItem6);

        startMenu.add(new JMenuItem("??????"));


        // ???????????????????????????
        menuBar.add(startMenu);
        // ?????????????????????frame??????????????????set??????add
        this.setJMenuBar(menuBar);
    }

    /**
     * ?????????????????????, ?????????????????????????????????
     */
    private void whenWindowOpened() {
        SmartFindDialog.addGlobalSmartFinder(); // ???????????????, ???????????????????????????, ?????????
        BondReviseUtil.initNuclearKeyBoardSettingForRevise(); // ??????????????????????????????

        ThreadUtil.execAsync(() -> {
            try {
                this.setExtendedState(JFrame.MAXIMIZED_BOTH); // ?????????
                this.getCorePanel().flushAllFuncFrameBounds(); // ????????????,??????????????????????????????????????????
                ThreadUtil.sleep(200);
                if (autoOpenLogsWindow) {
                    this.getCorePanel().getBottomLeftButtonList().get(0).doClick(); // ???????????????
                }
                if (autoOpenManiLogsWindow) {
                    this.getCorePanel().getBottomLeftButtonList().get(1).doClick(); // ?????????????????????
                }
                if (autoOpenFuncTree) {
                    this.getCorePanel().getLeftTopButtonList().get(0).doClick();
                }

                if (autoStartTrader) {
                    Trader.getAndStartInstance();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, true);

        if (autoStartEmNewFetcher) {
            ThreadUtil.execAsync(new Runnable() {
                @Override
                public void run() {
                    CronUtil.schedule("*/50 * * * * *", new Task() {
                        @Override
                        public void execute() {
                            ThreadUtil.sleep(2000);
                            new CaiJingDaoDuCrawlerEm().run(); // ??????????????????
                            new ZiXunJingHuaCrawlerEm().run(); // ??????????????????
                            new CompanyMajorIssuesCrawlerEm().run(); // ??????????????????
                            new CompanyGoodNewsCrawlerEm().run(); // ????????????
                            new NewsFeedsCrawlerEm().run(); // ??????????????????
                            new FourPaperNewsCrawlerEm().run(); // ??????????????????
                        }
                    });
                    CronUtil.setMatchSecond(true); // ???????????????, ????????????
                    CronUtil.start();
                }
            });
        }

        if (autoNewConceptDiscover) {
            ThreadUtil.execAsync(new Runnable() {
                @Override
                public void run() {
                    NewConceptDiscover.newConceptDiscoverStarter(5, 5);
                }
            }, true);
        }

        if (autoEmPc724NewsNotify) {
            ThreadUtil.execAsync(new Runnable() {
                @Override
                public void run() {
                    EmPcNewsNotify.notifyFast724New(); // 7*24
                }
            }, true);
        }

        if (autoEmPcHotNewsNotify) {
            ThreadUtil.execAsync(new Runnable() {
                @Override
                public void run() {
                    EmPcNewsNotify.notifyNewestHotNew(); //
                }
            }, true);
        }

    }


    FuncTreeWindow funcTreeWindow;
    public FuncButton objectsBtn;
    public FuncButton maniLogFunc;
    public FuncButton logsFunc;
    // AnalyzeRealtimeWindow analyzeRealtimeWindow;
    static volatile Trader trader;

    private void addListeners() {
        TraderGui mainWindow = this;
        // ???????????????????????????
        this.addWindowListener(new WindowAdapter() {
            @SneakyThrows
            @Override
            public void windowOpened(WindowEvent e) {
                MainDisplayWindow mainDisplayWindow = MainDisplayWindow.getInstance(
                        "?????????", mainWindow, false, false, true,
                        4096, 100, 1.0, 0, layerOfMainDisplay
                );
                mainWindow.getCorePanel().setMainDisplayWindow(mainDisplayWindow); // ??????????????????

                corePanel.getFuncPool().put(ButtonFactory.getButton("mainDisplay"), mainDisplayWindow); // ????????????,
                // ?????????button
                mainDisplayWindow.flushBounds(true);
                mainDisplayWindow.setAutoMaxWidthOrHeight(corePanel.getWidth());
                mainDisplayWindow.show();


                logsFunc = ButtonFactory.getButton("????????????");
                logsFunc.registerKeyboardAction(e1 -> logsFunc.doClick(), LOGS_BTN,
                        JComponent.WHEN_IN_FOCUSED_WINDOW);
                corePanel.registerFuncBtnWithoutFuncFrame(logsFunc, FuncFrameS.Type.BOTTOM_LEFT);
                logsFunc.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        LogFuncWindow logFuncWindow =  // ??????????????????????????????, ?????????????????????????????????
                                LogFuncWindow
                                        .getInstance(FuncFrameS.Type.BOTTOM_LEFT, "logs", mainWindow, logsFunc, true,
                                                true,
                                                false,
                                                true,
                                                1200, 100, 0.3, 30, false, layerOfLogFuncWindow);
                        corePanel.registerFuncBtnAndCorrespondFuncFrame(logsFunc, logFuncWindow);
                        if (logFuncWindow.isVisible()) {
                            logFuncWindow.flushBounds();
                            logFuncWindow.hide();
                        } else {
                            logFuncWindow.flushBounds();
                            logFuncWindow.show();
                        }
                    }
                });

                // ManipulateLogWindow

                maniLogFunc = ButtonFactory.getButton("????????????");
                maniLogFunc.registerKeyboardAction(e1 -> maniLogFunc.doClick(), MANI_LOG_BTN,
                        JComponent.WHEN_IN_FOCUSED_WINDOW);
                corePanel.registerFuncBtnWithoutFuncFrame(maniLogFunc, FuncFrameS.Type.BOTTOM_LEFT);
                maniLogFunc.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        ManipulateLogWindow manipulateLogWindow =  // ??????????????????????????????, ?????????????????????????????????
                                ManipulateLogWindow
                                        .getInstance(FuncFrameS.Type.BOTTOM_LEFT, "manis", mainWindow, maniLogFunc,
                                                true,
                                                true,
                                                false,
                                                true,
                                                1200, 100, 0.35, 30, true, layerOfManiLogFuncWindow);
                        corePanel.registerFuncBtnAndCorrespondFuncFrame(maniLogFunc, manipulateLogWindow);
                        if (manipulateLogWindow.isVisible()) {
                            manipulateLogWindow.flushBounds();
                            manipulateLogWindow.hide();
                        } else {
                            manipulateLogWindow.flushBounds();
                            manipulateLogWindow.show();
                        }
                    }
                });


                FuncButton databaseFunc = ButtonFactory.getButton("?????????", true);
                corePanel.registerFuncBtnWithoutFuncFrame(databaseFunc, FuncFrameS.Type.RIGHT_TOP);
                databaseFunc.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        DatabaseFuncWindow databaseFuncWindow = DatabaseFuncWindow
                                .getInstance(FuncFrameS.Type.RIGHT_TOP,
                                        "database", mainWindow, databaseFunc, true,
                                        true, false, true,
                                        1500, 100, 0.2, 30, false, layerOfDatabaseFuncWindow);
                        corePanel.registerFuncBtnAndCorrespondFuncFrame(databaseFunc, databaseFuncWindow);
                        if (databaseFuncWindow.isVisible()) {
                            databaseFuncWindow.flushBounds();
                            databaseFuncWindow.hide();
                        } else {
                            databaseFuncWindow.flushBounds();
                            databaseFuncWindow.show();
                        }
                    }
                });

                objectsBtn = ButtonFactory.getButton("?????????", true);
                // objectsBtn.setMnemonic(KeyEvent.VK_O);
                objectsBtn.registerKeyboardAction(e1 -> objectsBtn.doClick(), OBJECT_TREE_KS,
                        JComponent.WHEN_IN_FOCUSED_WINDOW);
                corePanel.registerFuncBtnWithoutFuncFrame(objectsBtn, FuncFrameS.Type.LEFT_TOP);
                objectsBtn.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        funcTreeWindow = FuncTreeWindow
                                .getInstance(FuncFrameS.Type.LEFT_TOP, "?????????",
                                        mainWindow, objectsBtn, true, false, false, true, 1000, 100, 0.12, 30,
                                        false,
                                        layerOfObjectsTree + 1); // ????????????null, ??????
                        corePanel.registerFuncBtnAndCorrespondFuncFrame(objectsBtn, funcTreeWindow);
                        if (funcTreeWindow.isVisible()) {
                            funcTreeWindow.flushBounds();
                            funcTreeWindow.hide();
                        } else {
                            funcTreeWindow.flushBounds(true);
                            funcTreeWindow.show();
                        }
                    }
                });

                mainWindow.whenWindowOpened();


            }

            //????????????????????????
            @SneakyThrows
            @Override
            public void windowClosing(WindowEvent e) {
                int res = JOptionPane.showConfirmDialog(mainWindow, GuiCommonUtil.buildDialogShowStr("????????????", "???????????????",
                        "yellow", "red"),
                        "??????????????????",
                        JOptionPane.YES_NO_OPTION);
                if (res == JOptionPane.YES_OPTION) {
                    BondReviseUtil.recoverNuclearKeyBoardSettingToThs(); // ?????????????????????
                    SystemTray.getSystemTray().remove(trayIcon); // ????????????
                    if (Trader.getInstance() != null) {
                        Trader.getInstance().stopTrade();
                    }
                    System.exit(0);
                }
            }

            //???????????????????????????
            @Override
            public void windowIconified(WindowEvent e) {
//                if (SystemTray.isSupported()) {
//                    setVisible(false);
//
//                } else {
//                     ????????????, ?????????????????????
//                }
            }
        });

        // ????????????
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // ????????????bounds, ???????????????

            }
        });

    }


    /**
     * ????????????, ???????????????????????? 3/4
     */
    private void centerSelf() {
        this.setLocation(screenW / 8, screenH / 8);
        this.setPreferredSize(new Dimension((int) (screenW * 0.75), (int) (screenH * 0.75)));
    }


    /**
     * ???????????? Panel. ??? ????????? 3????????????, ?????? JSplitPanel ?????????+????????? ?????????
     *
     * @return
     */
    public CorePanel buildCorePanel() {
        return new CorePanel(30, 30, 30, this);
    }


    private void initTrayIcon() {
        PopupMenu popup = new PopupMenu();
        MenuItem exitItem = new MenuItem("??????");
        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(true);
                setExtendedState(Frame.NORMAL);
            }
        };
        exitItem.addActionListener(listener);
        MenuItem exitItem2 = new MenuItem("????????????");
        exitItem2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SystemTray.getSystemTray().remove(trayIcon); // ????????????
                System.exit(0);
            }
        });
        popup.add(exitItem);
        popup.add(exitItem2);

        //??????image????????????????????????TrayIcon
        this.trayIcon = new TrayIcon(
                imageIcon.getImage(),
                "Scareers", popup);
        trayIcon.setImageAutoSize(true); // ????????????, ??????????????????
        this.trayIcon.addActionListener(listener);
    }

    public void showSystemTray() {
        SystemTray tray = SystemTray.getSystemTray();
        try {
            tray.add(this.trayIcon);
        } catch (AWTException ex) {
            ex.printStackTrace();
        }
    }


    /**
     * ????????????????????? LookAndFeel??????, ??????????????????
     *
     * @throws UnsupportedLookAndFeelException
     */
    public static void initGlobalStyle() throws UnsupportedLookAndFeelException {
        ToolTipManager.sharedInstance().setDismissDelay(50000000); // tooptip????????????
        UIManager.setLookAndFeel(new MetalLookAndFeel()); // ??????ui???, ?????? Metal??????. ????????????lookandfeel, ??????????????????
        UIDefaults defs = UIManager.getDefaults();

        defs.put("TextPane.background", new ColorUIResource(COLOR_THEME_MAIN));
        defs.put("TextPane.inactiveBackground", new ColorUIResource(COLOR_THEME_MAIN));
        defs.put("SplitPane.background", new ColorUIResource(COLOR_THEME_MAIN));
        defs.put("SplitPane.inactiveBackground", new ColorUIResource(COLOR_THEME_MAIN));
        defs.put("TabbedPane.background", new ColorUIResource(COLOR_THEME_MINOR));

        defs.put("Button.shadow", COLOR_THEME_MAIN);
        defs.put("Button.select", COLOR_THEME_MAIN);
        defs.put("Button.focus", COLOR_THEME_MAIN);
        defs.put("Button.background", new ColorUIResource(COLOR_THEME_MAIN));
        defs.put("Button.foreground", new ColorUIResource(COLOR_THEME_MAIN));//
        defs.put("Button.margin", new InsetsUIResource(2, 2, 2, 3));// ???????????????button,??????margin?????????
        defs.put("Button.gradient", null);// ???????????????

        defs.put("Panel.background", new ColorUIResource(COLOR_THEME_MINOR));
        defs.put("Panel.inactiveBackground", new ColorUIResource(COLOR_THEME_MINOR));

        defs.put("activeCaption", new javax.swing.plaf.ColorUIResource(Color.orange));
        defs.put("activeCaptionText", new javax.swing.plaf.ColorUIResource(Color.red));
        // System.out.println(JSONUtilS.toJsonPrettyStr(JSONUtilS.parse(defs)));

        UIManager.put("InternalFrame.activeTitleBackground", new javax.swing.plaf.ColorUIResource(COLOR_THEME_MINOR));
        UIManager.put("InternalFrame.activeTitleForeground", new javax.swing.plaf.ColorUIResource(COLOR_THEME_MINOR));
        UIManager.put("InternalFrame.inactiveTitleBackground", new javax.swing.plaf.ColorUIResource(COLOR_THEME_MINOR));
        UIManager.put("InternalFrame.inactiveTitleForeground", new javax.swing.plaf.ColorUIResource(COLOR_THEME_MINOR));

        // ????????????
        UIManager.put("SplitPaneDivider.draggingColor", new javax.swing.plaf.ColorUIResource(Color.red));
        UIManager.put("SplitPaneDivider.border", null);

        // ?????????
        UIManager.put("ScrollBar.width", 12); // ???????????????
        UIManager.put("ScrollBar.thumb", new javax.swing.plaf.ColorUIResource(Color.black)); // ??????????????????????????????

        // ?????????
        // ??????????????????????????????????????????, ??????????????????
        UIManager.put("OptionPane.background", new javax.swing.plaf.ColorUIResource(COLOR_THEME_MINOR));

    }

    /**
     * ????????????????????????. ?????????????????????????????? ???,???
     */
    public static void initScreenBounds() {
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration());
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        screenW = screenSize.width - insets.left - insets.right;
        screenH = screenSize.height - insets.top - insets.bottom;
    }


    public JLabel getPathLabel() {
        return pathLabel;
    }

    public JLabel getStatusBar() {
        return statusBar;
    }

    public CorePanel getCorePanel() {
        return corePanel;
    }

    public JDesktopPane getMainPane() {
        return mainPane;
    }

    public ImageIcon getImageIcon() {
        return imageIcon;
    }

    public TrayIcon getTrayIcon() {
        return trayIcon;
    }

    public JMenu getStartMenu() {
        return startMenu;
    }

    public FuncTreeWindow getFuncTreeWindow() {
        return funcTreeWindow;
    }
}
