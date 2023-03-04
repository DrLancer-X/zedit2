package zedit2;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashSet;
import java.util.List;

import static zedit2.Util.keyMatches;

public class TileEditor {
    private int selected;
    private WorldEditor editor;
    private Tile tile;
    private boolean szzt;
    private String[] everyType;
    private TileEditorCallback callback;
    private JDialog tileEditorFrame = null;
    private static int internalTagTracker = 0;
    private JList<String> statList = null;
    private int tileX, tileY;
    private Board board;
    private boolean advanced;
    private Stat currentStat;
    private JPanel otherEditorPanel;
    private JPanel otherEditorPanelActive;

    private static final int PARAM_P1 = 1;
    private static final int PARAM_P2 = 2;
    private static final int PARAM_P3 = 3;

    // Edit: ID, Colour
    // +-------+
    // | Stats | X, Y, StepX, StepY, Cycle, P1, P2, P3, Follower, Leader, Uid, Uco, CurrentInstruction, Code / Bind, Order
    // +-------+

    // Type: [        ] Col: [ ]
    // ------------------------------------
    //  Stat  | X      | P1     | Uid    |
    //  List  | Y      | P2     | Uco    |
    // -------| StepX  | P3     | IP     |
    //  New   | StepY  | Follower| Code   |
    //          Cycle    Leader    Order

    public TileEditor(WorldEditor editor, Board board, Tile inputTile, List<Stat> stats, TileEditorCallback callback, int x, int y, boolean advanced, int selected) {
        internalTagTracker++;
        this.callback = callback;
        this.editor = editor;
        this.board = board;
        this.advanced = advanced;
        this.selected = selected;
        tileX = x;
        tileY = y;
        if (inputTile == null) {
            if (stats != null) {
                if (x >= 0 && y >= 0 && x < board.getWidth() && y < board.getHeight()) {
                    tile = board.getTile(x, y, false);
                } else {
                    tile = new Tile(-1, -1, stats);
                }
            } else {
                throw new RuntimeException("Must pass in stats or a tile");
            }
        } else {
            tile = inputTile.clone();
        }
        szzt = editor.getWorldData().isSuperZZT();
        if (advanced) {
            createAdvancedGUI();
        } else {
            editorSelect();
        }
    }
    private void editorSelect()
    {
        // Certain things are not editable except with the advanced editor.
        // These will result in the dialog not popping up
        var stats = tile.getStats();
        // First of all, multiple stats means you get the advanced editor, always
        if (stats.size() > 1) {
            createAdvancedGUI();
            return;
        }
        // On the other hand, no stats means no editor, except for text, which gets a char picker
        if (stats.size() == 0) {
            if (ZType.isText(szzt, tile.getId())) {
                editorText();
                return;
            }
        }

        if (stats.size() == 1) {
            currentStat = stats.get(0);

            // The type of editor we will present will depend on the id + stats of the thing being edited
            switch (tile.getId()) {
                case ZType.OBJECT:
                    editorObject();
                    return;
                case ZType.SCROLL:
                    editorScroll();
                    return;
                default:
                    break;
            }

            // May be some other type
            if (editorOther()) {
                return;
            }
        }

        // Nothing happened. Call the callback
        callback.callback(tile);
    }

    private boolean constructOtherDialog() {
        otherEditorPanel = new JPanel(new BorderLayout());
        otherEditorPanelActive = otherEditorPanel;

        var tileId = tile.getId();
        boolean szzt = editor.getWorldData().isSuperZZT();
        switch (tileId) {
            case ZType.PASSAGE:
                editorAddBoardSelect(PARAM_P3);
                return true;
            case ZType.DUPLICATOR:
                editorAddBar("Start time", 1, 6, PARAM_P1);
                editorAddDirection("Duplicate in direction");
                editorAddBar("Speed", 1, 9, PARAM_P2);
                return true;
            case ZType.BOMB:
                editorAddBar("Countdown (1=unlit 2=cleanup)", 1, 9, PARAM_P1);
                return true;
            case ZType.BLINKWALL:
                editorAddBar("Start time", 1, 9, PARAM_P1);
                editorAddDirection("Direction");
                editorAddBar("Period", 1, 9, PARAM_P2);
                return true;
            case ZType.TRANSPORTER:
                editorAddDirection("Direction");
                return true;
            case ZType.BEAR:
                editorAddBar("Sensitivity", 1, 9, PARAM_P1);
                return true;
            case ZType.RUFFIAN:
                editorAddBar("Intelligence", 1, 9, PARAM_P1);
                editorAddBar("Resting time", 1, 9, PARAM_P2);
                return true;
            case ZType.SLIME:
                editorAddBar("Speed", 1, 9, PARAM_P2);
                return true;
            case ZType.TIGER:
            case ZType.SPINNINGGUN:
                editorAddBar("Intelligence", 1, 9, PARAM_P1);
                editorAddBar("Firing rate", 1, 9, PARAM_P2, 127);
                editorAddRadio("Fires bullets", "Fires stars", PARAM_P2, 128);
                return true;
            case ZType.PUSHER:
                editorAddDirection("Direction");
                return true;
            case ZType.LION:
                editorAddBar("Intelligence", 1, 9, PARAM_P1);
                return true;
            case ZType.HEAD:
                editorAddBar("Intelligence", 1, 9, PARAM_P1);
                editorAddBar("Deviance", 1, 9, PARAM_P2);
                return true;
            default:
                break;
        }

        if ((!szzt && tileId == ZZTType.BULLET) || (szzt && tileId == SZZTType.BULLET)) {
            editorAddDirection("Direction");
            editorAddRadio("Player bullet", "Enemy bullet", PARAM_P1, 1);
            return true;
        }
        if ((!szzt && tileId == ZZTType.STAR) || (szzt && tileId == SZZTType.STAR)) {
            editorAddDirection("Direction");
            editorAddRadio("Player star", "Enemy star", PARAM_P1, 1);
            editorAddSpinner("Lifespan", 0, 255, PARAM_P2);
            return true;
        }
        if (!szzt && tileId == ZZTType.SHARK) {
            editorAddBar("Intelligence", 1, 9, PARAM_P1);
            return true;
        }
        if (szzt && tileId == SZZTType.ROTON) {
            editorAddBar("Intelligence", 1, 9, PARAM_P1);
            editorAddBar("Switch rate", 1, 9, PARAM_P2);
            return true;
        }
        if (szzt && tileId == SZZTType.DRAGONPUP) {
            editorAddBar("Intelligence", 1, 9, PARAM_P1);
            editorAddBar("Switch rate", 1, 9, PARAM_P2);
            return true;
        }
        if (szzt && tileId == SZZTType.PAIRER) {
            editorAddBar("Intelligence", 1, 9, PARAM_P1);
            return true;
        }
        if (szzt && tileId == SZZTType.SPIDER) {
            editorAddBar("Intelligence", 1, 9, PARAM_P1);
            return true;
        }

        return false;
    }

    private int getParam(int param) {
        switch (param) {
            case PARAM_P1: return currentStat.getP1();
            case PARAM_P2: return currentStat.getP2();
            case PARAM_P3: return currentStat.getP3();
            default: throw new UnsupportedOperationException();
        }
    }

    private void setParam(int param, int value) {
        switch (param) {
            case PARAM_P1:
                currentStat.setP1(value);
                break;
            case PARAM_P2:
                currentStat.setP2(value);
                break;
            case PARAM_P3:
                currentStat.setP3(value);
                break;
            default: throw new UnsupportedOperationException();
        }
    }

    private void appendToActivePanel(JComponent component) {
        otherEditorPanelActive.add(component, BorderLayout.NORTH);
        var newActivePanel = new JPanel(new BorderLayout());
        otherEditorPanelActive.add(newActivePanel, BorderLayout.CENTER);
        otherEditorPanelActive = newActivePanel;
    }

    private void editorAddSpinner(String label, int min, int max, int param) {
        var panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(label), BorderLayout.WEST);
        int initialValue = getParam(param);
        var spinner = new JSpinner(new SpinnerNumberModel(initialValue, min, max, 1));
        spinner.addChangeListener(e -> setParam(param, (Integer) spinner.getValue()));
        panel.add(spinner, BorderLayout.EAST);

        appendToActivePanel(panel);
    }

    private void editorAddRadio(String option1, String option2, int param, int value) {
        boolean initialState = (getParam(param) & value) == value;
        var panel = new JPanel(new BorderLayout());
        var cb1 = new JRadioButton(option1, !initialState);
        var cb2 = new JRadioButton(option2, initialState);
        ChangeListener listener = e -> {
            int v = getParam(param) & (~value) | (cb2.isSelected() ? value : 0);
            setParam(param, v);
        };
        cb1.addChangeListener(listener);
        cb2.addChangeListener(listener);
        var bg = new ButtonGroup();
        bg.add(cb1);
        bg.add(cb2);

        panel.add(cb1, BorderLayout.NORTH);
        panel.add(cb2, BorderLayout.SOUTH);

        appendToActivePanel(panel);
    }

    private void editorAddDirection(String label) {
        var panel = new JPanel(new GridLayout(4, 1));
        panel.setBorder(BorderFactory.createTitledBorder(label));
        var cbN = new JRadioButton("North", (currentStat.getStepX() == 0) && (currentStat.getStepY() == -1));
        var cbS = new JRadioButton("South", (currentStat.getStepX() == 0) && (currentStat.getStepY() == 1));
        var cbE = new JRadioButton("East", (currentStat.getStepX() == 1) && (currentStat.getStepY() == 0));
        var cbW = new JRadioButton("West", (currentStat.getStepX() == -1) && (currentStat.getStepY() == 0));
        ChangeListener listener = e -> {
            if (cbN.isSelected()) { currentStat.setStepX(0); currentStat.setStepY(-1); }
            else if (cbS.isSelected()) { currentStat.setStepX(0); currentStat.setStepY(1); }
            else if (cbE.isSelected()) { currentStat.setStepX(1); currentStat.setStepY(0); }
            else if (cbW.isSelected()) { currentStat.setStepX(-1); currentStat.setStepY(0); }
        };
        cbN.addChangeListener(listener);
        cbS.addChangeListener(listener);
        cbE.addChangeListener(listener);
        cbW.addChangeListener(listener);
        var bg = new ButtonGroup();
        bg.add(cbN);
        bg.add(cbS);
        bg.add(cbE);
        bg.add(cbW);
        panel.add(cbN);
        panel.add(cbS);
        panel.add(cbE);
        panel.add(cbW);

        appendToActivePanel(panel);
    }

    private void editorAddBar(String label, int min, int max, int param) {
        editorAddBar(label, min, max, param, Integer.MAX_VALUE);
    }
    private void editorAddBar(String label, int min, int max, int param, int mask) {
        var panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(label), BorderLayout.NORTH);
        int initialValue = getParam(param) & mask;
        var slider = new JSlider(JSlider.HORIZONTAL, min, max, Util.clamp(initialValue + 1, min, max));
        slider.setMajorTickSpacing(1);
        slider.setPaintLabels(true);
        slider.addChangeListener(e -> {
            int v = slider.getValue() - 1;
            v = (getParam(param) & ~mask) | (v & mask);
            setParam(param, v);
        });
        panel.add(slider, BorderLayout.SOUTH);

        appendToActivePanel(panel);
    }

    private void editorAddBoardSelect(int param) {
        int destination = getParam(param);
        var boards = editor.getBoards();
        var boardNames = new String[boards.size()];
        for (int i = 0; i < boardNames.length; i++) {
            boardNames[i] = CP437.toUnicode(boards.get(i).getName());
        }
        var boardSelect = new JComboBox<String>(boardNames);
        if (destination < boards.size()) {
            boardSelect.setSelectedIndex(destination);
        } else {
            boardSelect.setEditable(true);
            boardSelect.getEditor().setItem(String.format("(invalid reference: %d)", destination));
        }
        boardSelect.setFont(CP437.getFont());
        boardSelect.addActionListener(e -> {
            int newDestination = boardSelect.getSelectedIndex();
            if (newDestination != -1) {
                setParam(param, newDestination);
            } else {
                try {
                    newDestination = Integer.parseInt((String) boardSelect.getEditor().getItem());
                    if (newDestination >= 0 && newDestination <= 255) {
                        setParam(param, newDestination);
                    }
                } catch (NumberFormatException ignored) {}
            }
        });

        appendToActivePanel(boardSelect);
    }

    private Component relativeFrame() {
        if (tileEditorFrame != null) return tileEditorFrame;
        return editor.getFrameForRelativePositioning();
    }

    private void editorText() {
        ColourSelector.createColourSelector(editor, tile.getCol(), relativeFrame(), e -> {
            tile.setCol(Integer.parseInt(e.getActionCommand()));
            callback.callback(tile);
        }, ColourSelector.CHAR);
    }
    private void editorObject() {
        // Objects get char selector (for P1) followed by code editor
        ColourSelector.createColourSelector(editor, currentStat.getP1(), relativeFrame(), e -> {
            currentStat.setP1(Integer.parseInt(e.getActionCommand()));
            codeEditor(currentStat, e1 -> {
                if (e1.getActionCommand().equals("update")) {
                    var source = (CodeEditor) e1.getSource();
                    currentStat.setCode(source.getCode());
                }
                callback.callback(tile);
            });
        }, ColourSelector.CHAR);
    }
    private void editorScroll() {
        codeEditor(currentStat, e -> {
            var source = (CodeEditor) e.getSource();

            currentStat.setCode(source.getCode());
            callback.callback(tile);
        });
    }

    private boolean editorOther() {
        if (constructOtherDialog()) {
            createGUI();
            setTitle();

            var buttonHolder = new JPanel(new FlowLayout());
            var buttons = new JPanel(new BorderLayout());
            otherEditorPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
            buttons.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
            tileEditorFrame.getContentPane().setLayout(new BorderLayout());
            tileEditorFrame.getContentPane().add(otherEditorPanel, BorderLayout.CENTER);
            tileEditorFrame.getContentPane().add(buttonHolder, BorderLayout.SOUTH);
            buttonHolder.add(buttons);

            buttons.add(okButton(), BorderLayout.WEST);
            buttons.add(cancelButton(), BorderLayout.EAST);

            finaliseGUI();
            return true;
        } else {
            return false;
        }
    }

    private void createGUI() {
        tileEditorFrame = new JDialog();
        Util.addEscClose(tileEditorFrame, tileEditorFrame.getRootPane());
        tileEditorFrame.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        tileEditorFrame.setResizable(false);
        tileEditorFrame.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    private void createAdvancedGUI() {
        everyType = getEveryType();
        createGUI();
        upd();
    }

    private void setTitle() {
        tileEditorFrame.setTitle("Set " + ZType.getName(szzt, tile.getId()));
        tileEditorFrame.setIconImage(getIcon());
    }

    private KeyListener setKeystrokes(JList statList) {
        KeyStroke k_PgUp = Util.getKeyStroke(editor.getGlobalEditor(), "PgUp");
        KeyStroke k_PgDn = Util.getKeyStroke(editor.getGlobalEditor(), "PgDn");

        return new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (keyMatches(e, k_PgUp)) {
                    statList.setSelectedIndex(statList.getSelectedIndex() - 1);
                    e.consume();
                } else if (keyMatches(e, k_PgDn)) {
                    statList.setSelectedIndex(statList.getSelectedIndex() + 1);
                    e.consume();
                }
            }

            @Override
            public void keyTyped(KeyEvent e) { }
            @Override
            public void keyReleased(KeyEvent e) { }
        };
    }

    private void upd() {
        String focusedElement = "";
        try {
            JComponent focusedComponent = (JComponent) tileEditorFrame.getMostRecentFocusOwner();
            if (focusedComponent != null) {
                focusedElement = focusedComponent.getToolTipText();
            }
        } catch (ClassCastException e) {
        }
        if (statList != null) {
            int statIdx = statList.getSelectedIndex();
            if (statIdx > -1 && statIdx < tile.getStats().size()) {
                ++internalTagTracker;
                tile.getStats().get(statIdx).setInternalTag(internalTagTracker);
            }
        }

        setTitle();

        tileEditorFrame.getContentPane().removeAll();
        tileEditorFrame.getContentPane().setLayout(new BorderLayout());

        if (tile.getId() != -1) {
            JPanel tileControls = new JPanel();
            tileControls.setBorder(BorderFactory.createTitledBorder("Edit tile:"));
            JLabel tileTypeLabel = new JLabel("Type:");
            JComboBox<String> tileTypeChoice = new JComboBox<>(everyType);
            tileTypeChoice.setToolTipText("Change this tile's type");
            tileTypeChoice.setSelectedIndex(tile.getId());
            tileTypeChoice.addActionListener(e -> {
                tile.setId(tileTypeChoice.getSelectedIndex());
                upd();
            });
            JLabel tileColLabel = new JLabel("Colour:");
            int selectorMode = ZType.isText(szzt, tile.getId()) ? ColourSelector.CHAR : ColourSelector.COLOUR;
            JButton colSelectButton = createColButton(tile.getCol(), selectorMode, e -> {
                tile.setCol(Integer.parseInt(e.getActionCommand()));
                upd();
            });
            colSelectButton.setToolTipText("Change this tile's colour");

            tileControls.add(tileTypeLabel);
            tileControls.add(tileTypeChoice);
            tileControls.add(tileColLabel);
            tileControls.add(colSelectButton);
            tileEditorFrame.getContentPane().add(tileControls, BorderLayout.NORTH);
        }

        JPanel statControls = new JPanel(new BorderLayout());
        statControls.setBorder(BorderFactory.createTitledBorder("Edit stats:"));

        statList = new JList<>(getStats(tile.getStats()));
        statList.setToolTipText("Select a stat to edit the properties of");
        restoreSelectedIndex(statList);
        var statListListener = new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                upd();
            }
        };
        statList.addListSelectionListener(statListListener);

        //statListButtons.add(addStatButton);
        //statListButtons.add(delStatButton);
        var statListScroll = new JScrollPane(statList);
        statListScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        statListScroll.setPreferredSize(new Dimension(50, 0));
        var selectedStatBounds = statList.getCellBounds(statList.getSelectedIndex(), statList.getSelectedIndex());
        if (selectedStatBounds != null) {
            selectedStatBounds.grow(0, selectedStatBounds.height);
            statList.scrollRectToVisible(selectedStatBounds);
        }

        statControls.add(statListScroll, BorderLayout.WEST);

        JPanel statParamPanel = new JPanel(new GridLayout(5, 3));
        statParamPanel.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
        fillStatParamPanel(statParamPanel, tile, statList.getSelectedIndex());

        statControls.add(statParamPanel, BorderLayout.CENTER);

        JPanel statListButtons = new JPanel(new GridLayout(1, 3));
        JButton addStatButton = new JButton("Add stat");
        addStatButton.setToolTipText("Add a new stat");
        addStatButton.addActionListener(e -> {
            addStat(statList, statListListener, null);
        });
        JButton dupStatButton = new JButton("Clone stat");
        dupStatButton.setToolTipText("Add a new stat, duplicating the currently selected stat");
        dupStatButton.addActionListener(e -> {
            addStat(statList, statListListener, tile.getStats().get(statList.getSelectedIndex()));
        });
        JButton delStatButton = new JButton("Delete stat");
        delStatButton.setToolTipText("Delete the currently selected stat");
        delStatButton.addActionListener(e -> {
            delStat(statList, statListListener, statList.getSelectedIndex());
        });
        statListButtons.add(addStatButton);

        JPanel bottomRow = new JPanel(new BorderLayout());
        bottomRow.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        bottomRow.add(statListButtons, BorderLayout.WEST);

        JPanel tileSubmitButtons = new JPanel(new GridLayout(1, 2));
        tileSubmitButtons.add(okButton());
        tileSubmitButtons.add(cancelButton());
        bottomRow.add(tileSubmitButtons, BorderLayout.EAST);

        if (tile.getStats().size() != 0) {
            statListButtons.add(dupStatButton);
            statListButtons.add(delStatButton);

            tileEditorFrame.getContentPane().add(statControls, BorderLayout.CENTER);
        }

        tileEditorFrame.getContentPane().add(bottomRow, BorderLayout.SOUTH);
        var keyListener = setKeystrokes(statList);
        restoreFocus(tileEditorFrame.getContentPane(), focusedElement, keyListener);
        finaliseGUI();
    }

    private JButton cancelButton() {
        var button = new JButton("Cancel");
        button.addActionListener(e -> tileEditorFrame.dispose());
        return button;
    }

    private JButton okButton() {
        var button = new JButton("OK");
        button.addActionListener(e -> {
            callback.callback(tile);
            tileEditorFrame.dispose();
        });
        return button;
    }

    private void finaliseGUI() {
        tileEditorFrame.pack();
        tileEditorFrame.setLocationRelativeTo(editor.getFrameForRelativePositioning());
        tileEditorFrame.setVisible(true);
    }

    private void addStat(JList<String> statList, ListSelectionListener statListListener, Stat copyFrom) {
        Stat newStat;
        if (copyFrom == null) {
            newStat = new Stat(szzt);
        } else {
            newStat = copyFrom.clone();
        }
        if (tileX != -1) {
            newStat.setX(tileX + 1);
            newStat.setY(tileY + 1);
        } else {
            newStat.setX(-1);
            newStat.setY(-1);
        }
        newStat.setStatId(-1);
        tile.addStat(newStat);
        statList.removeListSelectionListener(statListListener);
        statList.setListData(getStats(tile.getStats()));
        statList.setSelectedIndex(tile.getStats().size() - 1);
        upd();
    }

    private void delStat(JList<String> statList, ListSelectionListener statListListener, int statIdx) {
        if (tile.getStats().get(statIdx).getStatId() == 0) {
            JOptionPane.showMessageDialog(relativeFrame(), "You can't delete stat 0.");
            return;
        }
        tile.delStat(statIdx);
        statList.removeListSelectionListener(statListListener);
        statList.setListData(getStats(tile.getStats()));
        statList.setSelectedIndex(tile.getStats().size() - 1);
        upd();
    }

    private void restoreSelectedIndex(JList<String> statList) {
        int statCount = tile.getStats().size();
        if (statCount == 0) return;
        for (int i = 0; i < statCount; i++) {
            if (tile.getStats().get(i).getInternalTag() == internalTagTracker) {
                statList.setSelectedIndex(i);
                return;
            }
        }
        if (selected != -1) {
            for (int i = 0; i < statCount; i++) {
                if (tile.getStats().get(i).getStatId() == selected) {
                    statList.setSelectedIndex(i);
                    return;
                }
            }
        }
        statList.setSelectedIndex(0);
    }

    private void restoreFocus(Container container, String focusedElement, KeyListener listener) {
        var components = container.getComponents();
        for (var component : components) {
            if (component instanceof JComponent) {
                var jcomponent = (JComponent) component;
                jcomponent.addKeyListener(listener);
                var tooltip = jcomponent.getToolTipText();
                if (tooltip != null && tooltip.equals(focusedElement)) {
                    jcomponent.requestFocusInWindow();
                    return;
                }
            }
            if (component instanceof Container) {
                restoreFocus((Container) component, focusedElement, listener);
            }
        }
    }


    private JButton createColButton(int col, int selectorMode, ActionListener actionListener) {
        Image colSelectIcon;
        if (selectorMode != ColourSelector.CHAR) {
            colSelectIcon = editor.getCanvas().extractCharImage(0, col, 1, 1, false, "_#_");
        } else {
            colSelectIcon = editor.getCanvas().extractCharImage(col, 0x8F, 1, 1, false, "_$_");
        }
        JButton button = new JButton(new ImageIcon(colSelectIcon));
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ColourSelector.createColourSelector(editor, col, relativeFrame(), actionListener, selectorMode);
            }
        });
        return button;
    }

    private static final int MISSING_ITEM = Integer.MIN_VALUE;

    private void fillStatParamPanel(JPanel panel, Tile tile, int selectedIndex) {
        panel.removeAll();
        if (selectedIndex == -1) return;
        Stat stat = tile.getStats().get(selectedIndex);
        // Type: [        ] Col: [ ]
        // ------------------------------------
        //  Stat  | X      | P1     | Uid    |
        //  List  | Y      | P2     | Uco    |
        // -------| StepX  | P3     | IP     |
        //  New   | StepY  | Follower| Code   |
        //          Cycle    Leader    Order
        addPosSpin(panel, stat);
        //addInt8Spin(panel, "X:", stat.getX(), e -> stat.setX((Integer) ((JSpinner)e.getSource()).getValue()),
        //        "Edit stat's X position. Warning: this will not change the tile's position. Edit this only if you know what you are doing.");
        addInt8Spin(panel, "P1:", stat.getP1(), e -> stat.setP1((Integer) ((JSpinner)e.getSource()).getValue()),
                "Edit this stat's 1st parameter (function based on tile type)");
        addUidSelect(panel, "Under type:", stat.getUid(), e -> {
            @SuppressWarnings("unchecked")
            var src = (JComboBox<String>) e.getSource();
            stat.setUid(src.getSelectedIndex());
            upd();
        },  "Edit under type");
        //addInt8Spin(panel, "Y:", stat.getY(), e -> stat.setY((Integer) ((JSpinner)e.getSource()).getValue()),
        //        "Edit stat's Y position. Warning: this will not change the tile's position. Edit this only if you know what you are doing.");
        addInt16Spin(panel, "X-Step:", stat.getStepX(), e -> stat.setStepX((Integer) ((JSpinner)e.getSource()).getValue()),
                "Edit this stat's X-Step (function based on tile type)");
        addInt8Spin(panel, "P2:", stat.getP2(), e -> stat.setP2((Integer) ((JSpinner)e.getSource()).getValue()),
                "Edit this stat's 2nd parameter (function based on tile type)");
        addUcoBtn(panel, "Under colour:", stat, e -> {
            stat.setUco(Integer.parseInt(e.getActionCommand()));
            upd();
        }, "Edit the colour of the tile under this stat");
        addInt16Spin(panel, "Y-Step:", stat.getStepY(), e -> stat.setStepY((Integer) ((JSpinner)e.getSource()).getValue()),
                "Edit this stat's Y-Step (function based on tile type)");
        addInt8Spin(panel, "P3:", stat.getP3(), e -> stat.setP3((Integer) ((JSpinner)e.getSource()).getValue()),
                "Edit this stat's 3rd parameter (function based on tile type)");
        addInt16Spin(panel, "Instr. Ptr:", stat.getIp(), e -> stat.setIp((Integer) ((JSpinner)e.getSource()).getValue()),
                "Edit this stat's instruction pointer (-1 means ended)");
        addInt16Spin(panel, "Cycle:", stat.getCycle(), e -> stat.setCycle((Integer) ((JSpinner)e.getSource()).getValue()),
                "Edit this stat's cycle");
        addInt16SpinStatSel(panel, "Follower:", stat.getFollower(), e -> stat.setFollower((Integer) ((JSpinner)e.getSource()).getValue()),
                "follower");
        addCodeSelect(panel, stat);
        addInt16Spin(panel, "Order:", stat.getOrder(), e -> stat.setOrder((Integer) ((JSpinner)e.getSource()).getValue()),
                "Edit this stat's order (lower means its stat ID will be reduced)");
        addInt16SpinStatSel(panel, "Leader:", stat.getLeader(), e -> stat.setLeader((Integer) ((JSpinner)e.getSource()).getValue()),
                "leader");
        addStatFlags(panel, stat);
    }

    private void addCodeSelect(JPanel panel, Stat stat) {
        var selectPanel = new JPanel(new BorderLayout());
        selectPanel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        JButton codeButton, bindButton;
        ActionListener listener = e -> {
            if (e.getActionCommand().equals("update")) {
                var source = (CodeEditor) e.getSource();
                stat.setCode(source.getCode());
            }
            upd();
        };
        if (stat.getCodeLength() >= 0) {
            codeButton = new JButton("Edit code (" + stat.getCodeLength() + ")");
            codeButton.setToolTipText("Edit the code attached to this stat");
            codeButton.addActionListener(e -> {
                codeEditor(stat, listener);
            });
            bindButton = new JButton("Bind");
            bindButton.setToolTipText("Bind this stat's code to another");
            bindButton.addActionListener(e -> {
                int confirm = JOptionPane.OK_OPTION;
                if (stat.getCodeLength() > 0) {
                    confirm = JOptionPane.showConfirmDialog(relativeFrame(), "This will delete this object's code. Are you sure?", "Are you sure?", JOptionPane.OK_CANCEL_OPTION);
                }
                if (confirm == JOptionPane.OK_OPTION) {
                    new StatSelector(editor, board, e1 -> {
                        ((StatSelector)(e1.getSource())).close();
                        int val = StatSelector.getStatIdx(e1.getActionCommand());
                        if (val != stat.getStatId()) {
                            stat.setCodeLength(-val);
                        }
                        upd();
                    }, new String[]{"Select"});
                }
            });
        } else {
            codeButton = new JButton("View code (#" + -stat.getCodeLength() + ")");
            codeButton.setToolTipText("View the code attached to the bound stat (read-only)");
            codeButton.addActionListener(e -> {
                codeEditor(stat, listener);
            });
            bindButton = new JButton("Unbind");
            bindButton.setToolTipText("Break this object's bind");
            bindButton.addActionListener(e -> {
                stat.setCodeLength(0);
                upd();
            });
        }
        selectPanel.add(codeButton, BorderLayout.CENTER);
        selectPanel.add(bindButton, BorderLayout.EAST);
        panel.add(selectPanel);
    }

    private void codeEditor(Stat stat, ActionListener listener) {
        Stat followStat = stat;
        HashSet<Integer> followedStats = new HashSet<>();
        boolean success = false;
        int depth = 0;
        for (;;) {
            if (followedStats.contains(followStat.getStatId())) break;
            followedStats.add(followStat.getStatId());
            int codeLen = followStat.getCodeLength();
            if (codeLen >= 0) {
                success = true;
                break;
            } else {
                int boundTo = -codeLen;
                if (boundTo < board.getStatCount()) {
                    followStat = board.getStat(boundTo);
                    depth++;
                } else {
                    break;
                }
            }
        }
        if (success) {
            String txt;
            if (stat.getStatId() == -1) {
                txt = "tat";
            } else {
                txt = String.format("tat #%d", stat.getStatId());
            }
            if (depth > 0) {
                new CodeEditor(getIcon(), followStat, editor, listener, true, String.format("S%s, bound to stat #%d (read only)", txt, followStat.getStatId()));
            } else {
                new CodeEditor(getIcon(), followStat, editor, listener, false, String.format("Editing code of s%s", txt));
            }
        } else {
            JOptionPane.showMessageDialog(relativeFrame(), "Unable to reach this object's code.", "Unable to reach code", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Image getIcon() {
        var chr = ZType.getChar(szzt, tile);
        var col = ZType.getColour(szzt, tile);
        var editorIcon = editor.getCanvas().extractCharImage(chr, col, 4, 4, false, "$");
        return editorIcon;
    }

    private void addUcoBtn(JPanel panel, String label, Stat stat, ActionListener actionListener, String tooltip) {
        int initVal = stat.getUco();
        var selectPanel = new JPanel(new BorderLayout());
        selectPanel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        selectPanel.add(new JLabel(label), BorderLayout.WEST);
        int selectorMode = ZType.isText(szzt, stat.getUid()) ? ColourSelector.CHAR : ColourSelector.COLOUR;
        var btn = createColButton(initVal, selectorMode, actionListener);
        btn.setToolTipText(tooltip);
        selectPanel.add(btn, BorderLayout.EAST);
        panel.add(selectPanel);
    }

    private void addInt8Spin(JPanel panel, String label, int initVal, ChangeListener changeListener, String tooltip) {
        addSpin(panel, label, initVal, changeListener, 0, 255, false, tooltip);
    }
    private void addInt16Spin(JPanel panel, String label, int initVal, ChangeListener changeListener, String tooltip) {
        addSpin(panel, label, initVal, changeListener, -32768, 32767, false, tooltip);
    }
    private void addInt16SpinStatSel(JPanel panel, String label, int initVal, ChangeListener changeListener, String tooltip) {
        addSpin(panel, label, initVal, changeListener, -32768, 32767, true, tooltip);
    }
    private void addUidSelect(JPanel panel, String label, int initVal, ActionListener actionListener, String tooltip) {
        var selectPanel = new JPanel(new BorderLayout());
        selectPanel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        selectPanel.add(new JLabel(label), BorderLayout.WEST);
        var select = new JComboBox<>(everyType);
        select.setToolTipText(tooltip);
        select.setSelectedIndex(initVal);
        select.addActionListener(actionListener);
        selectPanel.add(select, BorderLayout.EAST);
        panel.add(selectPanel);
    }
    private void addSpin(JPanel panel, String label, int initVal, ChangeListener changeListener, int min, int max, boolean statSel, String tooltip) {
        var spinPanel = new JPanel(new BorderLayout());
        spinPanel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        spinPanel.add(new JLabel(label), BorderLayout.WEST);
        var spinner = new JSpinner(new SpinnerNumberModel(initVal, min, max, 1));
        if (!statSel) {
            spinner.setToolTipText(tooltip);
        } else {
            spinner.setToolTipText(String.format("Edit this stat's %s (used by centipedes. -1 means no %s)", tooltip, tooltip));
        }
        spinner.addChangeListener(changeListener);
        if (!statSel) {
            spinPanel.add(spinner, BorderLayout.EAST);
        } else {
            JPanel spinPanelSel = new JPanel(new BorderLayout());
            var spinPanelSearch = new JButton("\uD83D\uDD0D");
            spinPanelSearch.addActionListener(e -> {
                new StatSelector(editor, board, e1 -> {
                    ((StatSelector)(e1.getSource())).close();
                    int val = StatSelector.getStatIdx(e1.getActionCommand());
                    spinner.setValue(val);
                }, new String[]{"Select"});
            });
            spinPanelSearch.setToolTipText(String.format("Find a %s", tooltip));
            spinPanelSel.add(spinPanelSearch, BorderLayout.WEST);
            spinPanelSel.add(spinner, BorderLayout.EAST);
            spinPanel.add(spinPanelSel, BorderLayout.EAST);
        }
        panel.add(spinPanel);
    }
    private void addPosSpin(JPanel panel, Stat stat) {
        var spinPanelOuter = new JPanel(new BorderLayout());
        var toolTip = "<html>Edit stat's location. Warning: this will not move the tile itself, only the stat. <b>Edit this only if you know what you are doing.</b></html>";

        spinPanelOuter.add(new JLabel("Location:"), BorderLayout.WEST);
        JSpinner xSpinner, ySpinner;
        var bgColour = new Color(0xFFCCCC);
        if (stat.getX() >= 0) {
            xSpinner = new JSpinner(new SpinnerNumberModel(stat.getX(), 0, 255, 1));
            ySpinner = new JSpinner(new SpinnerNumberModel(stat.getY(), 0, 255, 1));
        } else {
            var model = new AbstractSpinnerModel() {

                @Override
                public Object getValue() {
                    return "N/A";
                }

                @Override
                public void setValue(Object value) { }

                @Override
                public Object getNextValue() {
                    return null;
                }

                @Override
                public Object getPreviousValue() {
                    return null;
                }
            };
            xSpinner = new JSpinner(model);
            ySpinner = new JSpinner(model);
            xSpinner.setEnabled(false);
            ySpinner.setEnabled(false);
        }
        xSpinner.getEditor().getComponent(0).setBackground(bgColour);
        ySpinner.getEditor().getComponent(0).setBackground(bgColour);
        xSpinner.addChangeListener(e -> stat.setX((int)xSpinner.getValue()));
        ySpinner.addChangeListener(e -> stat.setY((int)ySpinner.getValue()));
        xSpinner.setToolTipText(toolTip);
        ySpinner.setToolTipText(toolTip);

        var spinPanelInner = new JPanel(new BorderLayout());
        spinPanelOuter.add(spinPanelInner, BorderLayout.EAST);
        spinPanelInner.add(xSpinner, BorderLayout.WEST);
        spinPanelInner.add(ySpinner, BorderLayout.EAST);
        panel.add(spinPanelOuter);
    }
    private void addStatFlags(JPanel panel, Stat stat) {
        var flagPanel = new JPanel(new BorderLayout());
        var flagValues = new boolean[]{stat.isAutobind(), stat.isSpecifyId(), stat.isPlayer(), stat.isFlag4()};
        var flagNames = new String[]{"Autobind", "Set ID", "Player", "4"};
        var tooltips = new String[]{
                "Stat will automatically bind to other stats with the same code",
                "The Order field will be treated as a stat ID and this stat will be given that ID, if possible",
                "This is set to indicate that this is the real player, not a clone",
                "Flag #4"};
        var actions = new ActionListener[]{
                e -> stat.setAutobind(((JCheckBox)e.getSource()).isSelected()),
                e -> stat.setSpecifyId(((JCheckBox)e.getSource()).isSelected()),
                e -> stat.setIsPlayer(((JCheckBox)e.getSource()).isSelected()),
                e -> stat.setFlag4(((JCheckBox)e.getSource()).isSelected())};

        var currentPanel = flagPanel;
        //var checkBoxFont = new Font(Font.SANS_SERIF, Font.PLAIN, 8);
        int numBoxesToDraw = 3;
        for (int i = 0; i < numBoxesToDraw; i++) {
            var cb = new JCheckBox(flagNames[i], flagValues[i]);
            if (i == 2) cb.setEnabled(false);
            //cb.setVerticalTextPosition(SwingConstants.TOP);
            //cb.setHorizontalTextPosition(SwingConstants.CENTER);
            //cb.setFont(checkBoxFont);
            cb.setToolTipText(tooltips[i]);
            cb.addActionListener(actions[i]);
            var newPanel = new JPanel(new BorderLayout());
            currentPanel.add(cb, BorderLayout.WEST);
            currentPanel.add(newPanel, BorderLayout.CENTER);
            currentPanel = newPanel;
        }

        panel.add(flagPanel);
    }

    private String[] getStats(List<Stat> stats) {
        var statList = new String[stats.size()];
        for (int i = 0; i < stats.size(); i++) {
            int statId = stats.get(i).getStatId();
            statList[i] = statId == -1 ? "(new)" : String.valueOf(statId);
        }
        return statList;
    }

    private String[] getEveryType() {
        String[] allTypes = new String[256];
        for (int i = 0; i < 256; i++) {
            allTypes[i] = ZType.getName(szzt, i);
        }
        return allTypes;
    }
}
