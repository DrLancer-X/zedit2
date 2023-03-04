package zedit2;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.*;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

public class Settings {
    private static final int[] REVERSE_EXITS = {1, 0, 3, 2};
    private WorldEditor editor;
    private JDialog dialog;

    private static final String[][] preset_KevEdit = {
        {"Items", "Player(Cycle=1,StatId=0,IsPlayer=true)", "Ammo", "Torch", "Gem", "Key", "Door", "Scroll()", "Passage(Cycle=0)", "Duplicator(Cycle=2)", "Bomb(Cycle=6)", "Energizer", "Clockwise(Cycle=3)", "Counter(Cycle=2)"},
        {"Creatures", "Bear(Cycle=3)", "Ruffian(Cycle=1)", "Object(Cycle=3,P1=1)", "Slime(Cycle=3)", "Shark(Cycle=3)", "SpinningGun(Cycle=2)", "Pusher(Cycle=4)", "Lion(Cycle=2)", "Tiger(Cycle=2)", "Bullet()", "Star()", "Head(Cycle=2)", "Segment(Cycle=2)"},
        {"Terrain", "Water", "Forest", "Solid", "Normal", "Breakable", "Boulder", "SliderNS", "SliderEW", "Fake", "Invisible", "BlinkWall()", "Transporter(Cycle=2)", "Ricochet", "BoardEdge", "Monitor()", "HBlinkRay", "VBlinkRay", "Player!Dead Smiley"},
        {"Others", "Empty", "Floor", "Lava", "Web", "Line", "Stone(Cycle=1)", "Roton(Cycle=1)", "DragonPup(Cycle=2)", "Pairer(Cycle=2)", "Spider(Cycle=1)", "WaterN", "WaterS", "WaterE", "WaterW", "Messenger()"},
        {""},
        {""},
        {""},
        {""}
    };
    public static final String[][] preset_ZEdit = {
        {"Terrain", "Empty", "Solid", "Normal", "Breakable", "Water", "Floor", "Fake", "Invisible", "Line", "Forest", "Web", "Lava"},
        {"Items", "Ammo", "Torch", "Gem", "Key", "Door", "Energizer", "Stone(Cycle=1)"},
        {"Creatures", "Bear(Cycle=3)", "Ruffian(Cycle=1)", "Slime(Cycle=3)", "Shark(Cycle=3)", "SpinningGun(Cycle=2)", "Lion(Cycle=2)", "Tiger(Cycle=2)", "Head(Cycle=2)", "Segment(Cycle=2)", "Roton(Cycle=1)", "DragonPup(Cycle=2)", "Pairer(Cycle=2)", "Spider(Cycle=1)"},
        {"Puzzle pieces", "Boulder", "SliderNS", "SliderEW", "Ricochet", "Pusher(Cycle=4)", "Duplicator(Cycle=2)", "Bomb(Cycle=6)", "BlinkWall()"},
        {"Transport", "Passage(Cycle=0)", "Transporter(Cycle=2)", "Clockwise(Cycle=3)", "Counter(Cycle=2)", "WaterN", "WaterS", "WaterE", "WaterW", "BoardEdge"},
        {"Text", "BlackText!Black Text", "BlueText!Blue Text", "GreenText!Green Text", "CyanText!Cyan Text", "RedText!Red Text", "PurpleText!Purple Text", "BrownText!Brown Text", "BlackBText!Black Blinking Text", "BlueBText!Blue Blinking Text", "GreenBText!Green Blinking Text", "CyanBText!Cyan Blinking Text", "RedBText!Red Blinking Text", "PurpleBText!Purple Blinking Text", "BrownBText!Brown Blinking Text", "GreyBText!Grey Blinking Text", "GreyText!Grey Text"},
        {"Miscellaneous", "Messenger()", "Monitor()", "HBlinkRay", "VBlinkRay"},
        {"Objects", "Object(Cycle=1,P1=1)", "Player(Cycle=1,StatId=0,IsPlayer=true)", "Player(Cycle=1)Player Clone", "Scroll()", "Star()", "Bullet()"}
    };
    private static final String[][] preset_ZZT = {
            {"Items", "Player(Cycle=1,StatId=0,IsPlayer=true)", "Ammo", "Torch", "Gem", "Key", "Door", "Scroll()", "Passage(Cycle=0)", "Duplicator(Cycle=2)", "Bomb(Cycle=6)", "Energizer", "Clockwise(Cycle=3)", "Counter(Cycle=2)"},
            {"Creatures", "Bear(Cycle=3)", "Ruffian(Cycle=1)", "Object(Cycle=3,P1=1)", "Slime(Cycle=3)", "Shark(Cycle=3)", "SpinningGun(Cycle=2)", "Pusher(Cycle=4)", "Lion(Cycle=2)", "Tiger(Cycle=2)", "Head(Cycle=2)", "Segment(Cycle=2)"},
            {"Terrain", "Water", "Lava", "Forest", "Solid", "Normal", "Breakable", "Boulder", "SliderNS", "SliderEW", "Fake", "Invisible", "BlinkWall()", "Transporter(Cycle=2)", "Ricochet"},
            {"Uglies (SZZT)", "Roton(Cycle=1)", "DragonPup(Cycle=2)", "Pairer(Cycle=2)", "Spider(Cycle=1)"},
            {"Terrain (SZZT)", "Floor", "WaterN", "WaterS", "WaterW", "WaterE", "Web", "Stone(Cycle=1)"},
            {"Others", "Empty", "Line", "BoardEdge",  "Messenger()", "Monitor()", "HBlinkRay", "VBlinkRay", "Player(Cycle=1)Player Clone", "Star()", "Bullet()"},
            {""},
            {""}
    };

    public Settings(WorldEditor editor) {
        this.editor = editor;

        dialog = new JDialog();
        Util.addEscClose(dialog, dialog.getRootPane());
        dialog.setResizable(false);
        dialog.setIconImage(null);
        dialog.setTitle("ZEdit2 Settings");
        dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        //dialog.setResizable(false);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.add("Keystroke configuration", keystrokeConfig());
        tabbedPane.add("World testing configuration", testConfig());
        tabbedPane.add("Code editor configuration", codeEditConfig());
        tabbedPane.add("Element menu configuration", elementConfig());

        dialog.add(tabbedPane);
        dialog.pack();
        //tabbedPane.setPreferredSize(new Dimension(tabbedPane.getPreferredSize().width, 480));
        //dialog.pack();

        dialog.setLocationRelativeTo(editor.getFrameForRelativePositioning());
        dialog.setVisible(true);
    }

    private Component elementConfig() {
        var okButton = new JButton("OK");
        var cancelButton = new JButton("Cancel");
        var applyButton = new JButton("Apply");

        JPanel editPanel = new JPanel(new GridLayout(2, 4, 8, 8));
        var ge = editor.getGlobalEditor();

        TransferHandler trHandler = ElementListModel.getTransferHandler();

        Font titleFieldFont = new Font(Font.SANS_SERIF, Font.BOLD, 11);
        var cellRenderer = ElementListModel.getRenderer();
        applyButton.setEnabled(false);
        var listDataListener = new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) { upd(); }
            @Override
            public void intervalRemoved(ListDataEvent e) { upd(); }
            @Override
            public void contentsChanged(ListDataEvent e) { upd(); }

            private void upd() {
                applyButton.setEnabled(true);
            }
        };

        HashMap<Integer, JTextField> menuTitleFields = new HashMap<>();
        HashMap<Integer, ElementListModel> menuModels = new HashMap<>();

        for (int f = 3; f <= 10; f++) {
            JPanel elementMenuPanel = new JPanel(new BorderLayout());
            editPanel.add(elementMenuPanel);
            JTextField elementMenuTitle = new JTextField();
            elementMenuTitle.setFont(titleFieldFont);
            var elText = ge.getString(String.format("F%d_MENU", f), "");
            menuTitleFields.put(f, elementMenuTitle);
            elementMenuTitle.setText(elText);
            var elementMenuTitlePane = new JPanel(new BorderLayout());

            var flabel = new JLabel(String.format("F%d", f));
            flabel.setFont(CP437.getFont());
            flabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
            var yellow = new JPanel();

            elementMenuTitlePane.add(flabel, BorderLayout.WEST);
            elementMenuTitlePane.add(elementMenuTitle, BorderLayout.CENTER);

            elementMenuPanel.add(elementMenuTitlePane, BorderLayout.NORTH);


            var itemVec = new Vector<String>();
            for (int i = 0;; i++) {
                var key = String.format("F%d_MENU_%d", f, i);
                var val = ge.getString(key, "");
                if (val.isEmpty()) break;
                itemVec.add(val);
            }
            var menuModel = new ElementListModel(ge, f);
            menuModels.put(f, menuModel);
            var list = new JList<>(menuModel);
            list.setCellRenderer(cellRenderer);

            list.setDropMode(DropMode.ON);
            list.setDragEnabled(true);
            list.setTransferHandler(trHandler);
            list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
            list.getModel().addListDataListener(listDataListener);
            var scrollList = new JScrollPane(list);
            scrollList.setPreferredSize(new Dimension(0, 0));
            scrollList.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            scrollList.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            //list.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            elementMenuPanel.add(scrollList, BorderLayout.CENTER);
        }

        cancelButton.addActionListener(e -> {
            dialog.dispose();
        });
        applyButton.addActionListener(e -> {
            for (int f = 3; f <= 10; f++) {
                var elementMenuTitle = menuTitleFields.get(f);
                var menuModel = menuModels.get(f);
                if (elementMenuTitle.getText().isEmpty()) {
                    if (menuModel.getSize() != 0) {
                        elementMenuTitle.setText("F" + f);
                    }
                }
                // Unset this entire menu
                for (int i = 0;; i++) {
                    var key = String.format("F%d_MENU_%d", f, i);
                    var s = ge.getString(key, "");
                    ge.removeKey(key);
                    if (s.isEmpty()) {
                        break;
                    }
                }

                var title = elementMenuTitle.getText();
                ge.setString(String.format("F%d_MENU", f), title);
                for (int i = 0; i < menuModel.getSize(); i++) {
                    String element = menuModel.getElementAt(i);
                    ge.setString(String.format("F%d_MENU_%d", f, i), element);
                }
                ge.setString(String.format("F%d_MENU_%d", f, menuModel.getSize()), "");
            }
            editor.createMenu();
            editor.updateMenu();
            applyButton.setEnabled(false);
        });
        okButton.addActionListener(e -> {
            applyButton.doClick();
            dialog.dispose();
        });

        JButton zztPreset = new JButton("(Super)ZZT Preset");
        JButton keveditPreset = new JButton("KevEdit Preset");
        JButton zeditPreset = new JButton("ZEdit Preset");
        zztPreset.addActionListener(e -> loadPreset(menuTitleFields, menuModels, preset_ZZT));
        keveditPreset.addActionListener(e -> loadPreset(menuTitleFields, menuModels, preset_KevEdit));
        zeditPreset.addActionListener(e -> loadPreset(menuTitleFields, menuModels, preset_ZEdit));
        ArrayList<JButton> presetButtons = new ArrayList<>();
        presetButtons.add(zztPreset);
        presetButtons.add(keveditPreset);
        presetButtons.add(zeditPreset);

        return mainPanel(editPanel, okButton, cancelButton, applyButton, presetButtons);
    }

    private void loadPreset(HashMap<Integer, JTextField> menuTitleFields, HashMap<Integer, ElementListModel> menuModels, String[][] preset) {
        for (int f = 3; f <= 10; f++) {
            var presetArray = preset[f - 3];
            var elementMenuTitle = menuTitleFields.get(f);
            var menuModel = menuModels.get(f);

            elementMenuTitle.setText(presetArray[0]);
            menuModel.clear();
            ArrayList<String> items = new ArrayList<>();
            for (int i = 1; i < presetArray.length; i++) {
                items.add(presetArray[i]);
            }
            menuModel.insert(0, items, false);
        }
    }

    private Component codeEditConfig() {
        HashMap<String, Object> cfgMap = new HashMap<>();
        var okButton = new JButton("OK");
        var cancelButton = new JButton("Cancel");
        var applyButton = new JButton("Apply");

        okButton.addActionListener(e -> {
            writeToConfig(cfgMap);
            dialog.dispose();
        });
        cancelButton.addActionListener(e -> {
            dialog.dispose();
        });
        applyButton.addActionListener(e -> {
            writeToConfig(cfgMap);
            applyButton.setEnabled(false);
        });

        var ge = editor.getGlobalEditor();
        JPanel editPanel = multiFrame(null, null);

        String desc, tt;
        desc = "Automatically insert newline at the end of object code (if missing):";
        tt = "Editors like ZZT itself and KevEdit force a newline at the end of object code. Some programs malfunction without it, but if you know what you are doing and are desperate for space...";
        var autoInsertNewline = chkConfig("AUTO_INSERT_NEWLINE", cfgMap, desc, tt, applyButton);
        autoInsertNewline.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        JPanel ctd = multiFrame(editPanel, autoInsertNewline);

        JPanel grid = new JPanel(new GridLayout(1, 2));
        ctd = multiFrame(ctd, grid);
        var gridLeft = multiFrame(null, null);
        var gridRight = multiFrame(null, null);
        grid.add(gridLeft);
        grid.add(gridRight);
        gridLeft.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 16));
        gridRight.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 0));
        var l = gridLeft;
        var r = gridRight;

        desc = "Code editor default width:";
        tt = "Code editor default horizontal resolution (in pixels)";
        l = multiFrame(l, spinConfig("CODEEDITOR_WIDTH", cfgMap, desc, tt, 1, 9999, applyButton));
        desc = "Code editor default height:";
        tt = "Code editor default vertical resolution (in pixels)";
        r = multiFrame(r, spinConfig("CODEEDITOR_HEIGHT", cfgMap, desc, tt, 1, 9999, applyButton));
        desc = "Background colour:";
        tt = "Code editor normal background colour";
        l = multiFrame(l, colConfig("EDITOR_BG", cfgMap, desc, tt, applyButton));
        desc = "Background colour (music mode):";
        tt = "Code editor background colour used in music mode";
        r = multiFrame(r, colConfig("EDITOR_MUSIC_BG", cfgMap, desc, tt, applyButton));
        desc = "Background colour (bound):";
        tt = "Code editor background colour used for bound stats";
        l = multiFrame(l, colConfig("EDITOR_BIND_BG", cfgMap, desc, tt, applyButton));
        desc = "Selection colour:";
        tt = "Colour to use for the selection highlight";
        r = multiFrame(r, colConfig("EDITOR_SELECTION_COL", cfgMap, desc, tt, applyButton));
        desc = "Selected text colour:";
        tt = "Colour to use for the selected text";
        l = multiFrame(l, colConfig("EDITOR_SELECTED_TEXT_COL", cfgMap, desc, tt, applyButton));
        desc = "Cursor colour:";
        tt = "Colour to use for the code editor cursor";
        r = multiFrame(r, colConfig("EDITOR_CARET_COL", cfgMap, desc, tt, applyButton));

        desc = "Enable syntax highlighting of ZZT-OOP";
        tt = "Enables syntax highlighting. This includes indicating the length of #play statements, #char tooltips and long lines.";
        var toggleSyntax = chkConfig("SYNTAX_HIGHLIGHTING", cfgMap, desc, tt, applyButton);
        toggleSyntax.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        ctd = multiFrame(ctd, toggleSyntax);

        var syntaxPanel = new JPanel(new BorderLayout());
        syntaxPanel.setBorder(BorderFactory.createTitledBorder("Syntax highlighter colours"));

        final String[] syntax = {
                "HL_HASH", "Command symbol",
                "HL_COMMAND", "Command",
                "HL_COLON", "Label symbol",
                "HL_LABEL", "Label",
                "HL_ZAPPED", "Zapped label symbol",
                "HL_ZAPPEDLABEL", "Zapped label",
                "HL_CONDITION", "Conditional",
                "HL_COUNTER", "Counter",
                "HL_DIRECTION", "Direction",
                "HL_EXCLAMATION", "Message box choice",
                "HL_FLAG", "Flag",
                "HL_GOTOLABEL", "Label destination",
                "HL_MUSICNOTE", "#play (notes)",
                "HL_MUSICDRUM", "#play (drums)",
                "HL_MUSICOCTAVE", "#play (octave changes)",
                "HL_MUSICREST", "#play (rests)",
                "HL_MUSICSHARPFLAT", "#play (sharps/flats)",
                "HL_MUSICTIMING", "#play (durations)",
                "HL_MUSICTIMINGMOD", "#play (triplets/dots)",
                "HL_NUMBER", "Integer literal",
                "HL_OBJECTLABELSEPARATOR", "Object-label separator",
                "HL_OBJECTNAME", "Object name",
                "HL_SLASH", "Movement symbol",
                "HL_TEXT", "Message text",
                "HL_DOLLAR", "Centered text symbol",
                "HL_CENTEREDTEXT", "Centered text",
                "HL_ERROR", "ZZT-OOP error",
                "HL_TEXTWARN", "Warning",
                "HL_TEXTWARNLIGHT", "Warning (mild)",
                "HL_NOOP", "Operation with no effect",
                "HL_THING", "Element type",
                "HL_BLUE", "Blue",
                "HL_GREEN", "Green",
                "HL_CYAN", "Cyan",
                "HL_RED", "Red",
                "HL_PURPLE", "Purple",
                "HL_YELLOW", "Yellow",
                "HL_WHITE", "White"};

        var syntaxPanelGrid = new JPanel(new GridLayout(0, 2));

        for (int i = 0; i < syntax.length; i += 2) {
            String cfgString = syntax[i];
            String cfgName = syntax[i + 1];
            var cfg = colConfig(cfgString, cfgMap, cfgName, cfgName, applyButton);
            cfg.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            syntaxPanelGrid.add(cfg);
        }
        var syntaxPanelScroll = new JScrollPane(syntaxPanelGrid);
        syntaxPanelScroll.setPreferredSize(new Dimension(300, 220));

        syntaxPanel.add(syntaxPanelScroll, BorderLayout.CENTER);
        ctd.add(syntaxPanel, BorderLayout.CENTER);

        return mainPanel(editPanel, okButton, cancelButton, applyButton);
    }

    private Component testConfig() {
        var ge = editor.getGlobalEditor();
        var testChangeBoard = new JCheckBox("Start the tested world on the current board", ge.getBoolean("TEST_SWITCH_BOARD", false));
        testChangeBoard.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        var configPanel = multiFrame(null, null);
        var configCtd = multiFrame(configPanel, testChangeBoard);

        HashMap<String, Object> cfgMap = new HashMap<>();

        var okButton = new JButton("OK");
        var cancelButton = new JButton("Cancel");
        var applyButton = new JButton("Apply");

        testChangeBoard.addItemListener(e -> {
            applyButton.setEnabled(true);
            cfgMap.put("TEST_SWITCH_BOARD", testChangeBoard.isSelected());
        });

        okButton.addActionListener(e -> {
            writeToConfig(cfgMap);
            dialog.dispose();
        });
        cancelButton.addActionListener(e -> {
            dialog.dispose();
        });
        applyButton.addActionListener(e -> {
            writeToConfig(cfgMap);
            applyButton.setEnabled(false);
        });

        String desc, tt;
        var zztPanel = new JPanel(new GridLayout(0, 1));
        zztPanel.setBorder(BorderFactory.createTitledBorder("ZZT world testing settings"));
        desc = "Test directory:";
        tt = "Directory where ZZT and your ZZT engine (e.g. zeta) is found. This is also where the test files will be created, so ensure you have write access.";
        zztPanel.add(dirConfig("ZZT_TEST_PATH", cfgMap, desc, tt, applyButton));
        desc = "Command to run:";
        tt = "This is the command that will be run to launch ZZT.";
        zztPanel.add(textConfig("ZZT_TEST_COMMAND", cfgMap, desc, tt, applyButton));
        desc = "Command parameters:";
        tt = "Parameters to be passed to the command before the filename";
        zztPanel.add(textConfig("ZZT_TEST_PARAMS", cfgMap, desc, tt, applyButton));
        desc = "Base name:";
        tt = "The base name used for naming temporary .ZZT (and optionally .CHR and .PAL) files for testing. May be clipped to fit DOS filename limits.";
        zztPanel.add(textConfig("ZZT_TEST_FILENAME", cfgMap, desc, tt, applyButton));
        desc = "P";
        tt = "Optionally inject the 'P' character after a delay. Useful in conjunction with Zeta '-t' argument to begin playing the test world automatically. May not be 100% reliable.";
        zztPanel.add(injectConfig("ZZT_TEST_INJECT_P", cfgMap, desc, tt, applyButton));
        desc = "Use charset and palette, if modified";
        tt = "This appends Zeta charset and palette arguments to load whatever charset/palette the world is currently using if not the default.";
        zztPanel.add(chkConfig("ZZT_TEST_USE_CHARPAL", cfgMap, desc, tt, applyButton));
        desc = "If blinking is disabled, pass -b";
        tt = "This appends Zeta's 'disable blinking' argument if blinking is disabled.";
        zztPanel.add(chkConfig("ZZT_TEST_USE_BLINK", cfgMap, desc, tt, applyButton));

        configCtd = multiFrame(configCtd, zztPanel);

        var szztPanel = new JPanel(new GridLayout(0, 1));
        szztPanel.setBorder(BorderFactory.createTitledBorder("Super ZZT world testing settings"));
        desc = "Test directory:";
        tt = "Directory where Super ZZT and your ZZT engine (e.g. zeta) is found. This is also where the test files will be created, so ensure you have write access.";
        szztPanel.add(dirConfig("SZZT_TEST_PATH", cfgMap, desc, tt, applyButton));
        desc = "Command to run:";
        tt = "This is the command that will be run to launch Super ZZT.";
        szztPanel.add(textConfig("SZZT_TEST_COMMAND", cfgMap, desc, tt, applyButton));
        desc = "Command parameters:";
        tt = "Parameters to be passed to the command before the filename";
        szztPanel.add(textConfig("SZZT_TEST_PARAMS", cfgMap, desc, tt, applyButton));
        desc = "Base name:";
        tt = "The base name used for naming temporary .SZT (and optionally .CHR and .PAL) files for testing. May be clipped to fit DOS filename limits.";
        szztPanel.add(textConfig("SZZT_TEST_FILENAME", cfgMap, desc, tt, applyButton));
        desc = "P";
        tt = "Optionally inject the 'P' character after a delay. Useful in conjunction with Zeta '-t' argument and the below option to begin playing the test world automatically. May not be 100% reliable.";
        szztPanel.add(injectConfig("SZZT_TEST_INJECT_P", cfgMap, desc, tt, applyButton));
        desc = "Enter";
        tt = "Optionally inject the 'Enter' character after a further delay. Useful in conjunction with Zeta '-t' argument and the above option to begin playing the test world automatically. May not be 100% reliable.";
        szztPanel.add(injectConfig("SZZT_TEST_INJECT_ENTER", cfgMap, desc, tt, applyButton));
        desc = "Use charset and palette, if modified";
        tt = "This appends Zeta charset and palette arguments to load whatever charset/palette the world is currently using if not the default.";
        szztPanel.add(chkConfig("SZZT_TEST_USE_CHARPAL", cfgMap, desc, tt, applyButton));
        desc = "If blinking is disabled, pass -b";
        tt = "This appends Zeta's 'disable blinking' argument if blinking is disabled.";
        szztPanel.add(chkConfig("SZZT_TEST_USE_BLINK", cfgMap, desc, tt, applyButton));
        configCtd = multiFrame(configCtd, szztPanel);

        return mainPanel(configPanel, okButton, cancelButton, applyButton);
    }

    private void writeToConfig(HashMap<String, Object> cfgMap) {
        var ge = editor.getGlobalEditor();
        for (var cfgKey : cfgMap.keySet()) {
            var cfgVal = cfgMap.get(cfgKey);
            if (cfgVal instanceof Integer) ge.setInt(cfgKey, (Integer) cfgVal);
            else if (cfgVal instanceof Boolean) ge.setBoolean(cfgKey, (Boolean) cfgVal);
            else if (cfgVal instanceof String) ge.setString(cfgKey, (String) cfgVal);
            else throw new RuntimeException("Invalid config field");
        }
        cfgMap.clear();
    }

    private Icon colIcon(Color col) {
        int w = 48, h = 16;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        var g = img.getGraphics();
        g.setColor(col);
        g.fillRect(0, 0, w, h);
        return new ImageIcon(img);
    }

    private JPanel colConfig(String cfgString, HashMap<String, Object> cfgMap, String desc, String tt, JButton ea) {
        var ge = editor.getGlobalEditor();
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel(desc);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));
        JButton btn = new JButton();

        Color col = new Color(Integer.parseInt(ge.getString(cfgString, "000000"), 16));
        btn.setIcon(colIcon(col));
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                var returnedCol = JColorChooser.showDialog(dialog, desc, col);
                if (returnedCol != null) {
                    var colString = String.format("%06X", returnedCol.getRGB() & 0xFFFFFF);
                    btn.setIcon(colIcon(returnedCol));
                    cfgMap.put(cfgString, colString);
                    ea.setEnabled(true);
                }
            }
        });

        panel.add(label, BorderLayout.WEST);
        var panel2 = new JPanel(new BorderLayout());
        panel2.add(btn, BorderLayout.EAST);
        panel.add(panel2, BorderLayout.CENTER);
        label.setToolTipText(tt);
        btn.setToolTipText(tt);
        return panel;
    }

    private Component spinConfig(String cfgString, HashMap<String, Object> cfgMap, String desc, String tt, int min, int max, JButton ea) {
        var ge = editor.getGlobalEditor();
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel(desc);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));
        JSpinner spin = new JSpinner(new SpinnerNumberModel(ge.getInt(cfgString, 0), min, max, 1));
        spin.addChangeListener(e -> {
            ea.setEnabled(true);
            cfgMap.put(cfgString, spin.getValue());
        });
        panel.add(label, BorderLayout.WEST);
        var panel2 = new JPanel(new BorderLayout());
        panel2.add(spin, BorderLayout.EAST);
        panel.add(panel2, BorderLayout.CENTER);
        label.setToolTipText(tt);
        spin.setToolTipText(tt);
        return panel;
    }

    private JPanel chkConfig(String cfgString, HashMap<String, Object> cfgMap, String desc, String tt, JButton ea) {
        var ge = editor.getGlobalEditor();
        JPanel panel = new JPanel(new BorderLayout());

        boolean isSelected = ge.getBoolean(cfgString, false);

        JLabel lbl = new JLabel(desc);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));

        JCheckBox a = new JCheckBox();
        a.setSelected(isSelected);
        a.setBorder(BorderFactory.createEmptyBorder());
        a.addItemListener(e -> {
            ea.setEnabled(true);
            cfgMap.put(cfgString, a.isSelected());
        });

        lbl.setToolTipText(tt);
        a.setToolTipText(tt);

        panel.add(lbl, BorderLayout.WEST);
        panel.add(a, BorderLayout.CENTER);
        return panel;
    }

    private Component textConfig(String cfgString, HashMap<String, Object> cfgMap, String desc, String tt, JButton ea) {
        var ge = editor.getGlobalEditor();
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel(desc);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));
        JTextField tf = new JTextField(ge.getString(cfgString, ""));
        tf.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                upd();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                upd();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                upd();
            }
            private void upd() {
                ea.setEnabled(true);
                cfgMap.put(cfgString, tf.getText());
            }
        });
        panel.add(label, BorderLayout.WEST);
        panel.add(tf, BorderLayout.CENTER);
        label.setToolTipText(tt);
        tf.setToolTipText(tt);
        return panel;
    }

    private Component dirConfig(String cfgString, HashMap<String, Object> cfgMap, String desc, String tt, JButton ea) {
        var ge = editor.getGlobalEditor();
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel(desc);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));
        var path = ge.getString(cfgString, "");
        var file = new File(path);
        boolean dirOk = file.isDirectory();
        JTextField tf = new JTextField(path);
        tf.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                upd();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                upd();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                upd();
            }
            private void upd() {
                ea.setEnabled(true);
                cfgMap.put(cfgString, tf.getText());
            }
        });
        JButton chdir = new JButton("\uD83D\uDCC2");
        chdir.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (dirOk) {
                fc.setCurrentDirectory(file);
                fc.setSelectedFile(file);
            } else {
                fc.setCurrentDirectory(ge.getDefaultDirectory());
            }
            int r = fc.showOpenDialog(editor.getFrameForRelativePositioning());
            if (r == JFileChooser.APPROVE_OPTION) {
                tf.setText(fc.getSelectedFile().toString());
            }
        });

        panel.add(label, BorderLayout.WEST);
        panel.add(tf, BorderLayout.CENTER);
        panel.add(chdir, BorderLayout.EAST);
        label.setToolTipText(tt);
        tf.setToolTipText(tt);
        return panel;
    }

    private Component injectConfig(String cfgString, HashMap<String, Object> cfgMap, String desc, String tt, JButton ea) {
        var ge = editor.getGlobalEditor();
        JPanel panel = new JPanel(new BorderLayout());

        boolean isSelected = ge.getBoolean(cfgString, false);
        int delay = ge.getInt(cfgString + "_DELAY", 0);

        JCheckBox a = new JCheckBox("After", isSelected);
        a.setBorder(BorderFactory.createEmptyBorder());
        a.addItemListener(e -> {
            ea.setEnabled(true);
            cfgMap.put(cfgString, a.isSelected());
        });
        JSpinner b = new JSpinner(new SpinnerNumberModel(delay, 0, 999999, 10));
        //b.setBorder(BorderFactory.createEmptyBorder(1, 1, 1,1));
        b.addChangeListener(e -> {
            ea.setEnabled(true);
            cfgMap.put(cfgString + "_DELAY", b.getValue());
        });
        JLabel c = new JLabel(String.format("milliseconds, inject '%s' into the input stream", desc));
        a.setToolTipText(tt);
        b.setToolTipText(tt);
        c.setToolTipText(tt);

        panel.add(a, BorderLayout.WEST);
        JPanel mid = new JPanel(new BorderLayout());
        mid.add(b, BorderLayout.WEST);
        panel.add(mid, BorderLayout.CENTER);
        JPanel right = new JPanel(new BorderLayout());
        right.add(c, BorderLayout.WEST);
        mid.add(right, BorderLayout.CENTER);

        return panel;
    }

    private JPanel multiFrame(JPanel panel, Component addTo)
    {
        if (panel == null) return new JPanel(new BorderLayout());
        panel.add(addTo, BorderLayout.NORTH);
        var extra = new JPanel(new BorderLayout());
        panel.add(extra, BorderLayout.CENTER);
        return extra;
    }

    private Component mainPanel(Component contentPanel, JButton okButton, JButton cancelButton, JButton applyButton) {
        ArrayList<JButton> emptyList = new ArrayList<>();
        return mainPanel(contentPanel, okButton, cancelButton, applyButton, emptyList);
    }
    private Component mainPanel(Component contentPanel, JButton okButton, JButton cancelButton, JButton applyButton, ArrayList<JButton> extraButtons) {
        var mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        var buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(new JPanel(), BorderLayout.CENTER);
        var buttonGrid = new JPanel(new GridLayout(1, 3));
        buttonGrid.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        buttonPanel.add(buttonGrid, BorderLayout.EAST);

        var extraButtonGrid = new JPanel(new GridLayout(1, 0));
        buttonPanel.add(extraButtonGrid, BorderLayout.WEST);
        extraButtonGrid.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        for (var button : extraButtons) {
            extraButtonGrid.add(button);
        }

        buttonGrid.add(okButton);
        buttonGrid.add(cancelButton);
        buttonGrid.add(applyButton);
        okButton.setEnabled(true);
        cancelButton.setEnabled(true);
        applyButton.setEnabled(false);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        return mainPanel;
    }

    private Component keystrokeConfig() {
        var panel = new JPanel(new GridLayout(0, 2));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        var scrollPane = new JScrollPane(panel);
        scrollPane.setPreferredSize(new Dimension(540, 360));

        String[] keymappings = {"Escape", "Cancel operation / Close editor",
                "Up", "Move cursor up",
                "Down", "Move cursor down",
                "Left", "Move cursor left",
                "Right", "Move cursor right",
                "Alt-Up", "Move cursor up 10 tiles",
                "Alt-Down", "Move cursor down 10 tiles",
                "Alt-Left", "Move cursor left 10 tiles",
                "Alt-Right", "Move cursor right 10 tiles",
                "Shift-Up", "Switch to board at north exit",
                "Shift-Down", "Switch to board at south exit",
                "Shift-Left", "Switch to board at west exit",
                "Shift-Right", "Switch to board at east exit",
                "Ctrl-Shift-Up", "Create board at north exit",
                "Ctrl-Shift-Down", "Create board at south exit",
                "Ctrl-Shift-Left", "Create board at west exit",
                "Ctrl-Shift-Right", "Create board at east exit",
                "Tab", "Toggle drawing mode",
                "Home", "Move cursor to top-left",
                "End", "Move cursor to bottom-right",
                "PgUp", "Select previous stat (in tile editor)",
                "PgDn", "Select next stat (in tile editor)",
                "Insert", "Store in buffer",
                "Space", "Place from buffer to cursor location",
                "Delete", "Delete tile at cursor",
                "Enter", "Grab and modify tile at cursor / Finish marking block",
                "Ctrl-Enter", "Grab and modify tile at cursor (advanced)",
                "Ctrl-=", "Zoom in",
                "Ctrl--", "Zoom out",
                "A", "Add board",
                "B", "Switch board",
                "C", "Select colour",
                "D", "Delete board",
                "F", "Flood-fill",
                "G", "World settings",
                "I", "Board settings",
                "L", "Load world",
                "P", "Modify tile in buffer",
                "S", "Save as",
                "X", "Board exits",
                "Ctrl-A", "Create atlas / remove current atlas",
                "Ctrl-B", "Open buffer manager",
                "Ctrl-D", "Duplicate line",
                "Ctrl-E", "Erase player from board",
                "Ctrl-F", "Find",
                "Ctrl-H", "Replace",
                "Ctrl-S", "Save",
                "Ctrl-P", "Modify tile in buffer (advanced)",
                "Ctrl-R", "Remove current board from atlas",
                "Ctrl-V", "Import image from clipboard",
                "Ctrl-X", "Exchange buffer's fg/bg colours",
                "Ctrl-Y", "Redo",
                "Ctrl-Z", "Undo",
                "Ctrl--", "Transpose down (in code editor)",
                "Ctrl-=", "Transpose up (in code editor)",
                "Alt-B", "Begin block operation",
                "Alt-F", "Gradient fill",
                "Alt-I", "Import board",
                //"Alt-M", "Modify tile under cursor / Music mode (in code editor)",
                "Alt-M", "Modify tile under cursor",
                "Alt-S", "Stat list",
                "Alt-T", "Test world",
                "Alt-X", "Export board",
                "Shift-B", "Board list",
                "Ctrl-Alt-M", "Modify tile under cursor (advanced)",
                "F1", "Help",
                "F2", "Enter text",
                "F3", "Access F3 elements menu, insert char in code editor",
                "F4", "Access F4 elements menu",
                "F5", "Access F5 elements menu",
                "F6", "Access F6 elements menu",
                "F7", "Access F7 elements menu",
                "F8", "Access F8 elements menu",
                "F9", "Access F9 elements menu",
                "F10", "Access F10 elements menu",
                "F12", "Take screenshot (to disk)",
                "Alt-F12", "Take screenshot (to clipboard)",
                "Shift-F1", "Show stats",
                "Shift-F2", "Show objects",
                "Shift-F3", "Show invisibles",
                "Shift-F4", "Show empties",
                "Shift-F5", "Show fakes",
                "Shift-F6", "Show empties as text",
                "0", "Load from buffer slot 0",
                "1", "Load from buffer slot 1",
                "2", "Load from buffer slot 2",
                "3", "Load from buffer slot 3",
                "4", "Load from buffer slot 4",
                "5", "Load from buffer slot 5",
                "6", "Load from buffer slot 6",
                "7", "Load from buffer slot 7",
                "8", "Load from buffer slot 8",
                "9", "Load from buffer slot 9",
                "Ctrl-0", "Store in buffer slot 0",
                "Ctrl-1", "Store in buffer slot 1",
                "Ctrl-2", "Store in buffer slot 2",
                "Ctrl-3", "Store in buffer slot 3",
                "Ctrl-4", "Store in buffer slot 4",
                "Ctrl-5", "Store in buffer slot 5",
                "Ctrl-6", "Store in buffer slot 6",
                "Ctrl-7", "Store in buffer slot 7",
                "Ctrl-8", "Store in buffer slot 8",
                "Ctrl-9", "Store in buffer slot 9"};
        var ge = editor.getGlobalEditor();

        HashMap<String, KeyStroke> keyMap = new HashMap<>();
        for (int i = 0; i < keymappings.length; i += 2) {
            var actionName = keymappings[i];
            var keyStroke = Util.getKeyStroke(ge, actionName);
            keyMap.put(actionName, keyStroke);
        }

        var okButton = new JButton("OK");
        var resetButton = new JButton("Reset");
        var cancelButton = new JButton("Cancel");
        var applyButton = new JButton("Apply");
        okButton.addActionListener(e -> {
            applyKeymap(keyMap);
            dialog.dispose();
            resetButton.setEnabled(false);
            applyButton.setEnabled(false);
        });
        cancelButton.addActionListener(e -> {
            dialog.dispose();
        });
        applyButton.addActionListener(e -> {
            applyKeymap(keyMap);
            resetButton.setEnabled(false);
            applyButton.setEnabled(false);
        });

        FocusListener btnFocus = new FocusListener(){
            Color storedBg = Color.MAGENTA;
            @Override
            public void focusGained(FocusEvent e) {
                JTextField tf = (JTextField) e.getSource();
                storedBg = tf.getBackground();
                tf.setBackground(Color.WHITE);
            }

            @Override
            public void focusLost(FocusEvent e) {
                JTextField tf = (JTextField) e.getSource();
                tf.setBackground(storedBg);
            }
        };

        for (int i = 0; i < keymappings.length; i += 2) {
            var actionName = keymappings[i];
            var keyDescription = keymappings[i + 1];
            var keyDescriptionLabel = new JLabel(keyDescription);
            var keyStrokeName = Util.keyStrokeString(Util.getKeyStroke(ge, actionName));
            var keyBind = new JPanel(new BorderLayout());
            var clearButton = new JButton("Clear");

            var keyBinder = new JTextField(keyStrokeName);
            clearButton.addActionListener(e -> {
                keystrokeSet(actionName, null, keyBinder, keyMap);
                resetButton.setEnabled(true);
                applyButton.setEnabled(true);
            });
            KeyListener keyListener = new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    var mods = e.getModifiersEx();
                    mods = mods & ~(InputEvent.ALT_GRAPH_DOWN_MASK);
                    var ks = KeyStroke.getKeyStroke(e.getKeyCode(), mods);
                    resetButton.setEnabled(true);
                    applyButton.setEnabled(true);
                    keystrokeSet(actionName, ks, keyBinder, keyMap);
                    e.consume();
                }
            };

            keyBinder.setEditable(false);
            keyBinder.addKeyListener(keyListener);
            keyBinder.setFocusable(true);
            keyBinder.addFocusListener(btnFocus);

            keyBind.add(keyBinder, BorderLayout.CENTER);
            keyBind.add(clearButton, BorderLayout.EAST);

            panel.add(keyDescriptionLabel);
            panel.add(keyBind);
        }

        return mainPanel(scrollPane, okButton, cancelButton, applyButton);
    }

    private void applyKeymap(HashMap<String, KeyStroke> keyMap) {
        var ge = editor.getGlobalEditor();
        for (var actionName : keyMap.keySet()) {
            var keyStrokeString = keyMap.get(actionName);
            Util.setKeyStroke(ge, actionName, keyStrokeString);
        }
        editor.refreshKeymapping();
    }

    private void keystrokeSet(String actionName, KeyStroke keyStroke, JTextField keyBinder, HashMap<String, KeyStroke> keyMap) {
        for (var ks : keyMap.values()) {
            if (ks != null && ks.equals(keyStroke)) return;
        }

        var keyStrokeString = Util.keyStrokeString(keyStroke);
        keyBinder.setText(keyStrokeString);
        keyMap.put(actionName, keyStroke);
    }

    public static void board(JFrame frame, Board currentBoard, WorldData worldData) {
        if (currentBoard == null) return;

        var settings = new JDialog();
        Util.addEscClose(settings, settings.getRootPane());
        Util.addKeyClose(settings, settings.getRootPane(), KeyEvent.VK_ENTER, 0);
        settings.setResizable(false);
        settings.setTitle("Board Settings");
        settings.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        settings.setModalityType(JDialog.ModalityType.APPLICATION_MODAL);
        settings.getContentPane().setLayout(new BorderLayout());
        var cp = new JPanel(new GridLayout(0, 1));
        cp.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        settings.getContentPane().add(cp, BorderLayout.CENTER);

        int boardNameLimit = currentBoard.isSuperZZT() ? 60 : 50;
        var boardNameLabel = new JLabel();
        boardNameLabel.setText("Board name: " + currentBoard.getName().length + "/" + boardNameLimit);

        cp.add(boardNameLabel);
        var boardNameField = new JTextField(CP437.toUnicode(currentBoard.getName()));
        boardNameField.setFont(CP437.getFont());
        boardNameField.setToolTipText("Board name");

        ((AbstractDocument)boardNameField.getDocument()).setDocumentFilter(new LimitDocFilter(boardNameLimit));
        boardNameField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { upd(); }
            public void removeUpdate(DocumentEvent e) { upd(); }
            public void changedUpdate(DocumentEvent e) { upd(); }
            private void upd() {
                currentBoard.setName(CP437.toBytes(boardNameField.getText()));
                boardNameLabel.setText("Board name: " + currentBoard.getName().length + "/" + boardNameLimit);
            }
        });
        cp.add(boardNameField);

        var shotsPanel = new JPanel(new BorderLayout());
        var shotsSpinner = new JSpinner(new SpinnerNumberModel(currentBoard.getShots(), 0, 255, 1));
        shotsPanel.add(new JLabel("Max player shots:    "), BorderLayout.WEST);
        shotsPanel.add(shotsSpinner, BorderLayout.EAST);
        shotsSpinner.addChangeListener(e -> currentBoard.setShots((Integer) shotsSpinner.getValue()));
        cp.add(shotsPanel);

        var timePanel = new JPanel(new BorderLayout());
        var timeSpinner = new JSpinner(new SpinnerNumberModel(currentBoard.getTimeLimit(), -32768, 32767, 1));
        timePanel.add(new JLabel("Time limit (secs):    "), BorderLayout.WEST);
        timePanel.add(timeSpinner, BorderLayout.EAST);
        timeSpinner.addChangeListener(e -> currentBoard.setTimeLimit((Integer) timeSpinner.getValue()));
        cp.add(timePanel);

        var xPanel = new JPanel(new BorderLayout());
        var xSpinner = new JSpinner(new SpinnerNumberModel(currentBoard.getPlayerX(), 0, 255, 1));
        xPanel.add(new JLabel("Player entry X:    "), BorderLayout.WEST);
        xPanel.add(xSpinner, BorderLayout.EAST);
        xSpinner.addChangeListener(e -> currentBoard.setPlayerX((Integer) xSpinner.getValue()));
        cp.add(xPanel);

        var yPanel = new JPanel(new BorderLayout());
        var ySpinner = new JSpinner(new SpinnerNumberModel(currentBoard.getPlayerY(), 0, 255, 1));
        yPanel.add(new JLabel("Player entry Y:    "), BorderLayout.WEST);
        yPanel.add(ySpinner, BorderLayout.EAST);
        ySpinner.addChangeListener(e -> currentBoard.setPlayerY((Integer) ySpinner.getValue()));
        cp.add(yPanel);

        if (!worldData.isSuperZZT()) {
            var darkPanel = new JPanel(new BorderLayout());
            var darkBox = new JCheckBox();
            darkBox.setSelected(currentBoard.isDark());
            darkPanel.add(new JLabel("Dark:"), BorderLayout.WEST);
            darkPanel.add(darkBox, BorderLayout.EAST);
            darkBox.addChangeListener(e -> currentBoard.setDark(darkBox.isSelected()));
            cp.add(darkPanel);
        } else {
            var cxPanel = new JPanel(new BorderLayout());
            var cxSpinner = new JSpinner(new SpinnerNumberModel(currentBoard.getCameraX(), -32768, 32767, 1));
            cxPanel.add(new JLabel("Camera X:    "), BorderLayout.WEST);
            cxPanel.add(cxSpinner, BorderLayout.EAST);
            cxSpinner.addChangeListener(e -> currentBoard.setCameraX((Integer) cxSpinner.getValue()));
            cp.add(cxPanel);

            var cyPanel = new JPanel(new BorderLayout());
            var cySpinner = new JSpinner(new SpinnerNumberModel(currentBoard.getCameraY(), -32768, 32767, 1));
            cyPanel.add(new JLabel("Camera Y:    "), BorderLayout.WEST);
            cyPanel.add(cySpinner, BorderLayout.EAST);
            cySpinner.addChangeListener(e -> currentBoard.setCameraY((Integer) cySpinner.getValue()));
            cp.add(cyPanel);
        }

        var restartPanel = new JPanel(new BorderLayout());
        var restartBox = new JCheckBox();
        restartBox.setSelected(currentBoard.isRestartOnZap());
        restartPanel.add(new JLabel("Restart if hurt:"), BorderLayout.WEST);
        restartPanel.add(restartBox, BorderLayout.EAST);
        restartBox.addChangeListener(e -> currentBoard.setRestartOnZap(restartBox.isSelected()));
        cp.add(restartPanel);

        settings.pack();
        settings.setLocationRelativeTo(frame);
        settings.setVisible(true);
    }

    public static void boardExits(JFrame frame, Board currentBoard, java.util.List<Board> boards, int currentBoardIdx) {
        if (currentBoard == null) return;

        var settings = new JDialog();
        Util.addEscClose(settings, settings.getRootPane());
        Util.addKeyClose(settings, settings.getRootPane(), KeyEvent.VK_ENTER, 0);
        settings.setResizable(false);
        settings.setTitle("Board Exits");
        settings.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        settings.setModalityType(JDialog.ModalityType.APPLICATION_MODAL);
        settings.getContentPane().setLayout(new GridLayout(0, 1));

        var boardNames = BoardManager.generateBoardSelectArray(boards, false);

        for (int exit : new int[]{0, 1, 3, 2}) {
            String exitName = BoardManager.EXIT_NAMES[exit];
            var checkbox = new JCheckBox("Reciprocated");
            var boardPanel = new JPanel(new BorderLayout());
            //exitPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 4));
            boardPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            var exitLabel = new JLabel("Exit to " + exitName + ":");
            exitLabel.setVerticalAlignment(SwingConstants.BOTTOM);
            var boardSelect = new JComboBox<String>(boardNames);
            int exitBoard = currentBoard.getExit(exit);
            reciprocalCheckboxStatus(checkbox, boards, exitBoard, exit, currentBoardIdx);
            if (exitBoard < boards.size()) {
                boardSelect.setSelectedIndex(exitBoard);
            } else {
                boardSelect.setEditable(true);
                boardSelect.getEditor().setItem(String.format("(invalid reference: %d)", exitBoard));
            }
            boardSelect.setFont(CP437.getFont());
            boardSelect.addActionListener(e -> {
                int newDestination = boardSelect.getSelectedIndex();
                if (newDestination != -1) {
                    currentBoard.setExit(exit, newDestination);
                    reciprocalCheckboxStatus(checkbox, boards, newDestination, exit, currentBoardIdx);
                } else {
                    try {
                        newDestination = Integer.parseInt((String) boardSelect.getEditor().getItem());
                        if (newDestination >= 0 && newDestination <= 255) {
                            currentBoard.setExit(exit, newDestination);
                            reciprocalCheckboxStatus(checkbox, boards, newDestination, exit, currentBoardIdx);
                        }
                    } catch (NumberFormatException ignored) {}
                }

            });

            boardPanel.add(exitLabel, BorderLayout.NORTH);
            boardPanel.add(boardSelect, BorderLayout.CENTER);

            checkbox.setToolTipText("Links the destination board to this one in the opposite direction");
            boardPanel.add(checkbox, BorderLayout.SOUTH);

            settings.getContentPane().add(boardPanel);
        }

        settings.pack();
        settings.setLocationRelativeTo(frame);
        settings.setVisible(true);
    }

    private static void setReciprocated(boolean selected, Board otherBoard, int exit, int currentBoardIdx) {
        int re = REVERSE_EXITS[exit];
        if (selected) {
            otherBoard.setExit(re, currentBoardIdx);
        } else {
            if (otherBoard.getExit(re) == currentBoardIdx) {
                otherBoard.setExit(re, 0);
            }
        }
    }

    private static void reciprocalCheckboxStatus(JCheckBox checkbox, java.util.List<Board> boards,
                                                 int exitBoard, int exit, int boardIdx) {
        while (checkbox.getChangeListeners().length > 0) {
            checkbox.removeChangeListener(checkbox.getChangeListeners()[0]);
        }
        if (exitBoard == 0 || boardIdx == 0 || exitBoard >= boards.size()) {
            checkbox.setEnabled(false);
        } else {
            checkbox.setEnabled(true);
            boolean isReciprocated = boards.get(exitBoard).getExit(REVERSE_EXITS[exit]) == boardIdx;
            checkbox.setSelected(isReciprocated);

            checkbox.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    setReciprocated(checkbox.isSelected(), boards.get(exitBoard), exit, boardIdx);
                }
            });
        }
    }


    public static void world(JFrame frame, java.util.List<Board> boards, WorldData worldData, DosCanvas canvas) {
        var settings = new JDialog();
        Util.addEscClose(settings, settings.getRootPane());
        Util.addKeyClose(settings, settings.getRootPane(), KeyEvent.VK_ENTER, 0);
        settings.setResizable(false);
        settings.setTitle("World Settings");
        settings.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        settings.setModalityType(JDialog.ModalityType.APPLICATION_MODAL);
        settings.getContentPane().setLayout(new BorderLayout());

        var topPanel = new JPanel(new GridLayout(2, 1));
        topPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        var startingBoardPanel = new JPanel(new BorderLayout());
        var startingBoardLabel = new JLabel("Starting board:   ");
        var boardNames = new String[boards.size()];
        for (int i = 0; i < boards.size(); i++) {
            boardNames[i] = CP437.toUnicode(boards.get(i).getName());
        }

        var startingBoardDropdown = new JComboBox<String>(boardNames);
        startingBoardDropdown.setFont(CP437.getFont());
        startingBoardDropdown.setToolTipText("Select the starting board (or current board, for a saved game)");
        if (worldData.getCurrentBoard() < boards.size()) {
            startingBoardDropdown.setSelectedIndex(worldData.getCurrentBoard());
        }
        startingBoardDropdown.addActionListener(e -> worldData.setCurrentBoard(startingBoardDropdown.getSelectedIndex()));

        var worldNamePanel = new JPanel(new BorderLayout());
        var worldNameLabel = new JLabel("World name:   ");
        var worldNameBox = new JTextField(CP437.toUnicode(worldData.getName()));
        worldNameBox.setFont(CP437.getFont());
        worldNameBox.setToolTipText("This world name field is used to reload the world after exiting.\nNormally it should be the same as the filename.");
        worldNameBox.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { upd(); }
            public void removeUpdate(DocumentEvent e) { upd(); }
            public void changedUpdate(DocumentEvent e) { upd(); }
            private void upd() {
                worldData.setName(CP437.toBytes(worldNameBox.getText()));
            }
        });

        startingBoardPanel.add(startingBoardLabel, BorderLayout.WEST);
        startingBoardPanel.add(startingBoardDropdown, BorderLayout.CENTER);
        worldNamePanel.add(worldNameLabel, BorderLayout.WEST);
        worldNamePanel.add(worldNameBox, BorderLayout.CENTER);
        topPanel.add(startingBoardPanel);
        topPanel.add(worldNamePanel);
        settings.getContentPane().add(topPanel, BorderLayout.NORTH);

        var eastPanel = new JPanel(new GridLayout(0, 1));
        eastPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        for (int i = 0; i < worldData.getNumFlags(); i++) {
            int flagNum = i;
            var flagPanel = new JPanel(new BorderLayout());
            var flagLabel = new JLabel(String.format("Flag #%d:", flagNum));
            var flagBox = new JTextField(CP437.toUnicode(worldData.getFlag(flagNum)));
            flagLabel.setPreferredSize(new Dimension(50, flagLabel.getPreferredSize().height));
            flagBox.setPreferredSize(new Dimension(100, flagBox.getPreferredSize().height));
            flagBox.setFont(CP437.getFont());
            flagBox.setToolTipText("Edit flag #" + flagNum + " (note: ZZT can only read/write flags with the characters A-Z 0-9 : and _)");
            flagBox.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { upd(); }
                public void removeUpdate(DocumentEvent e) { upd(); }
                public void changedUpdate(DocumentEvent e) { upd(); }
                private void upd() {
                    worldData.setFlag(flagNum, CP437.toBytes(flagBox.getText()));
                }
            });

            flagPanel.add(flagLabel, BorderLayout.WEST);
            flagPanel.add(flagBox, BorderLayout.EAST);
            eastPanel.add(flagPanel);
        }
        settings.getContentPane().add(eastPanel, BorderLayout.EAST);

        var westPanel = new JPanel(new GridLayout(0, 1));
        westPanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));

        var healthPanel = new JPanel(new BorderLayout());
        var healthSpinner = new JSpinner(new SpinnerNumberModel(worldData.getHealth(), -32768, 32767, 1));
        healthPanel.add(new JLabel("Health:"), BorderLayout.WEST);
        healthPanel.add(healthSpinner, BorderLayout.EAST);
        healthSpinner.addChangeListener(e -> worldData.setHealth((Integer) healthSpinner.getValue()));
        westPanel.add(healthPanel);

        var ammoPanel = new JPanel(new BorderLayout());
        var ammoSpinner = new JSpinner(new SpinnerNumberModel(worldData.getAmmo(), -32768, 32767, 1));
        ammoPanel.add(new JLabel("Ammo:"), BorderLayout.WEST);
        ammoPanel.add(ammoSpinner, BorderLayout.EAST);
        ammoSpinner.addChangeListener(e -> worldData.setAmmo((Integer) ammoSpinner.getValue()));
        westPanel.add(ammoPanel);

        var gemsPanel = new JPanel(new BorderLayout());
        var gemsSpinner = new JSpinner(new SpinnerNumberModel(worldData.getGems(), -32768, 32767, 1));
        gemsPanel.add(new JLabel("Gems:"), BorderLayout.WEST);
        gemsPanel.add(gemsSpinner, BorderLayout.EAST);
        gemsSpinner.addChangeListener(e -> worldData.setGems((Integer) gemsSpinner.getValue()));
        westPanel.add(gemsPanel);

        var scorePanel = new JPanel(new BorderLayout());
        var scoreSpinner = new JSpinner(new SpinnerNumberModel(worldData.getScore(), -32768, 32767, 1));
        scorePanel.add(new JLabel("Score:"), BorderLayout.WEST);
        scorePanel.add(scoreSpinner, BorderLayout.EAST);
        scoreSpinner.addChangeListener(e -> worldData.setScore((Integer) scoreSpinner.getValue()));
        westPanel.add(scorePanel);

        if (!worldData.isSuperZZT()) {
            var torchesPanel = new JPanel(new BorderLayout());
            var torchesSpinner = new JSpinner(new SpinnerNumberModel(worldData.getTorches(), -32768, 32767, 1));
            torchesPanel.add(new JLabel("Torches:"), BorderLayout.WEST);
            torchesPanel.add(torchesSpinner, BorderLayout.EAST);
            torchesSpinner.addChangeListener(e -> worldData.setTorches((Integer) torchesSpinner.getValue()));
            westPanel.add(torchesPanel);

            var torchTimerPanel = new JPanel(new BorderLayout());
            var torchTimerSpinner = new JSpinner(new SpinnerNumberModel(worldData.getTorchTimer(), -32768, 32767, 1));
            torchTimerPanel.add(new JLabel("Torch cycles:"), BorderLayout.WEST);
            torchTimerPanel.add(torchTimerSpinner, BorderLayout.EAST);
            torchTimerSpinner.addChangeListener(e -> worldData.setTorchTimer((Integer) torchTimerSpinner.getValue()));
            westPanel.add(torchTimerPanel);
        } else {
            var zPanel = new JPanel(new BorderLayout());
            var zSpinner = new JSpinner(new SpinnerNumberModel(worldData.getZ(), -32768, 32767, 1));
            zPanel.add(new JLabel("Z:"), BorderLayout.WEST);
            zPanel.add(zSpinner, BorderLayout.EAST);
            zSpinner.addChangeListener(e -> worldData.setZ((Integer) zSpinner.getValue()));
            westPanel.add(zPanel);
        }

        var energiserPanel = new JPanel(new BorderLayout());
        var energiserSpinner = new JSpinner(new SpinnerNumberModel(worldData.getEnergiser(), -32768, 32767, 1));
        energiserPanel.add(new JLabel("Energiser:"), BorderLayout.WEST);
        energiserPanel.add(energiserSpinner, BorderLayout.EAST);
        energiserSpinner.addChangeListener(e -> worldData.setEnergiser((Integer) energiserSpinner.getValue()));
        westPanel.add(energiserPanel);

        var timePanel = new JPanel(new BorderLayout());
        var timeSpinner = new JSpinner(new SpinnerNumberModel(worldData.getTimeSeconds(), -32768, 32767, 1));
        timePanel.add(new JLabel("Time elapsed:  "), BorderLayout.WEST);
        timePanel.add(timeSpinner, BorderLayout.EAST);
        timeSpinner.addChangeListener(e -> worldData.setTimeSeconds((Integer) timeSpinner.getValue()));
        westPanel.add(timePanel);

        var subsecsPanel = new JPanel(new BorderLayout());
        var subsecsSpinner = new JSpinner(new SpinnerNumberModel(worldData.getTimeTicks(), -32768, 32767, 1));
        subsecsPanel.add(new JLabel("Subseconds:"), BorderLayout.WEST);
        subsecsPanel.add(subsecsSpinner, BorderLayout.EAST);
        subsecsSpinner.addChangeListener(e -> worldData.setTimeTicks((Integer) subsecsSpinner.getValue()));
        westPanel.add(subsecsPanel);

        var savegamePanel = new JPanel(new BorderLayout());
        var savegameBox = new JCheckBox();
        savegameBox.setSelected(worldData.getLocked());
        savegamePanel.add(new JLabel("Savegame / Locked:"), BorderLayout.WEST);
        savegamePanel.add(savegameBox, BorderLayout.EAST);
        savegameBox.addChangeListener(e -> worldData.setLocked(savegameBox.isSelected()));
        westPanel.add(savegamePanel);

        JPanel centerPanel = new JPanel(new BorderLayout());

        var keysPanel = new JPanel(new GridLayout(2, 7));
        keysPanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
        for (int i = 0; i < 7; i++) {
            var ico = new ImageIcon(canvas.extractCharImage(12, i + 9, 1, 1, false, "_$_"));
            var keyLabel = new JLabel(ico);
            keyLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
            keyLabel.setHorizontalAlignment(SwingConstants.CENTER);
            keyLabel.setVerticalAlignment(SwingConstants.CENTER);
            keysPanel.add(keyLabel);
        }
        for (int i = 0; i < 7; i++) {
            int keyIdx = i;
            var keyCheckbox = new JCheckBox();
            keyCheckbox.setSelected(worldData.getKey(keyIdx));
            keyCheckbox.addChangeListener(e -> worldData.setKey(keyIdx, keyCheckbox.isSelected()));
            keyCheckbox.setHorizontalAlignment(SwingConstants.CENTER);
            keyCheckbox.setVerticalAlignment(SwingConstants.CENTER);
            keysPanel.add(keyCheckbox);
        }

        centerPanel.add(westPanel, BorderLayout.CENTER);
        centerPanel.add(keysPanel, BorderLayout.SOUTH);

        settings.getContentPane().add(centerPanel, BorderLayout.CENTER);

        settings.pack();
        settings.setLocationRelativeTo(frame);
        settings.setVisible(true);
    }
}
