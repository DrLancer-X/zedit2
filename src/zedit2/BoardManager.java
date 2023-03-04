package zedit2;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class BoardManager {
    private JDialog dialog = null;
    private WorldEditor editor = null;
    private WorldData worldData = null;
    private java.util.List<Board> boards = null;
    private JTable table = null;
    private AbstractTableModel tableModel = null;
    private boolean szzt;
    private String[] boardSelectArray;
    private JButton upButton, downButton, delButton, exportButton;
    private boolean modal;

    private static final int COL_NUM = 0;
    private static final int COL_NAME = 1;
    private static final int COL_SHOTS = 2;
    private static final int COL_TIMELIMIT = 3;
    private static final int COL_PLAYERX = 4;
    private static final int COL_PLAYERY = 5;
    private static final int COL_EXITN = 6;
    private static final int COL_EXITS = 7;
    private static final int COL_EXITE = 8;
    private static final int COL_EXITW = 9;

    public static final String[] EXIT_NAMES = {"north", "south", "west", "east"};

    public BoardManager(WorldEditor editor, java.util.List<Board> boards, boolean modal) {
        this.modal = modal;
        this.editor = editor;
        this.boards = boards;
        worldData = editor.getWorldData();
        szzt = worldData.isSuperZZT();

        updateBoardSelectArray();
        generateTable();
    }

    public BoardManager(WorldEditor editor, java.util.List<Board> boards) {
        this(editor, boards, true);
    }

    public BoardManager(WorldEditor editor, java.util.List<Board> boards, int deleteBoard) {
        this(editor, boards, false);
        if (boards.size() > 1) {
            table.clearSelection();
            table.getRowSorter().setSortKeys(null);
            table.addRowSelectionInterval(deleteBoard, deleteBoard);
            SwingUtilities.invokeLater(this::delSelected);
        } else {
            JOptionPane.showMessageDialog(dialog, "Can't delete the only board.", "Board deletion error", JOptionPane.ERROR_MESSAGE);
        }
        dialog.dispose();
    }

    private void generateTable() {
        dialog = new JDialog();
        if (modal) dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        Util.addEscClose(dialog, dialog.getRootPane());
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setTitle("Board list");
        dialog.setIconImage(editor.getCanvas().extractCharImage(240, 0x1F, 2, 2, false, "$"));
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                editor.getCanvas().setIndicate(null, null);
            }
        });

        final int COL_DARK = szzt ? -1 : 10;
        final int COL_CAMERAX = szzt ? 10 : -1;
        final int COL_CAMERAY = szzt ? 11 : -1;
        final int COL_RESTART = szzt ? 12 : 11;
        final int NUM_COLS = szzt ? 13 : 12;

        tableModel = new AbstractTableModel() {
            @Override
            public int getRowCount() {
                return boards.size();
            }

            @Override
            public int getColumnCount() {
                return NUM_COLS;
            }

            @Override
            public String getColumnName(int columnIndex) {
                switch (columnIndex) {
                    case COL_NUM: return "#";
                    case COL_NAME: return "Board name";
                    case COL_SHOTS: return "Shots";
                    case COL_TIMELIMIT: return "Time limit";
                    case COL_PLAYERX: return "Player X";
                    case COL_PLAYERY: return "Player Y";
                    case COL_EXITN: return "North exit";
                    case COL_EXITS: return "South exit";
                    case COL_EXITE: return "East exit";
                    case COL_EXITW: return "West exit";
                    default:
                        if (columnIndex == COL_DARK) return "Dark";
                        if (columnIndex == COL_CAMERAX) return "Camera X";
                        if (columnIndex == COL_CAMERAY) return "Camera Y";
                        if (columnIndex == COL_RESTART) return "Restart if hurt";
                        return "?";
                }
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                    case COL_NUM:
                    case COL_SHOTS:
                    case COL_TIMELIMIT:
                    case COL_PLAYERX:
                    case COL_PLAYERY:
                    case COL_EXITN:
                    case COL_EXITS:
                    case COL_EXITE:
                    case COL_EXITW:
                        return Integer.class;
                    case COL_NAME: return String.class;
                    default:
                        if (columnIndex == COL_DARK) return Boolean.class;
                        if (columnIndex == COL_CAMERAX) return Integer.class;
                        if (columnIndex == COL_CAMERAY) return Integer.class;
                        if (columnIndex == COL_RESTART) return Boolean.class;
                        return null;
                }
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return columnIndex != COL_NUM;
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                Board board = boards.get(rowIndex);
                switch (columnIndex) {
                    case COL_NUM: return rowIndex;
                    case COL_NAME: return CP437.toUnicode(board.getName());
                    case COL_SHOTS: return board.getShots();
                    case COL_TIMELIMIT: return board.getTimeLimit();
                    case COL_PLAYERX: return board.getPlayerX();
                    case COL_PLAYERY: return board.getPlayerY();
                    case COL_EXITN:
                    case COL_EXITS:
                    case COL_EXITE:
                    case COL_EXITW: return board.getExit(columnToExit(columnIndex));
                    default:
                        if (columnIndex == COL_DARK) return board.isDark();
                        if (columnIndex == COL_CAMERAX) return board.getCameraX();
                        if (columnIndex == COL_CAMERAY) return board.getCameraY();
                        if (columnIndex == COL_RESTART) return board.isRestartOnZap();
                        return null;
                }
            }

            @Override
            public void setValueAt(Object value, int rowIndex, int columnIndex) {
                Board board = boards.get(rowIndex);
                switch (columnIndex) {
                    case COL_NAME: board.setName(CP437.toBytes((String) value)); return;
                    case COL_SHOTS: board.setShots((Integer) value); return;
                    case COL_TIMELIMIT: board.setTimeLimit((Integer) value); return;
                    case COL_PLAYERX: board.setPlayerX((Integer) value); return;
                    case COL_PLAYERY: board.setPlayerY((Integer) value); return;
                    case COL_EXITN:
                    case COL_EXITS:
                    case COL_EXITE:
                    case COL_EXITW:
                        int boardIdx = -1;
                        if (value instanceof String) {
                            String boardName = (String) value;

                            if (!boardName.equals("(no board)")) {
                                int dot = boardName.indexOf('.');
                                if (dot == -1) dot = boardName.length();
                                try {
                                    boardIdx = Integer.parseInt(boardName.substring(0, dot));
                                } catch (NumberFormatException ignored) {
                                }
                            } else {
                                boardIdx = 0;
                            }
                        } else {
                            boardIdx = (Integer) value;
                        }
                        if (boardIdx >= 0 && boardIdx <= 255) {
                            board.setExit(columnToExit(columnIndex), boardIdx);
                        }
                        return;
                    default:
                        if (columnIndex == COL_DARK) board.setDark((Boolean) value);
                        if (columnIndex == COL_CAMERAX) board.setCameraX((Integer) value);
                        if (columnIndex == COL_CAMERAY) board.setCameraY((Integer) value);
                        if (columnIndex == COL_RESTART) board.setRestartOnZap((Boolean) value);
                }
            }
        };
        table = new JTable(tableModel) {
            @Override
            public TableCellRenderer getCellRenderer(int rowIndex, int columnIndex) {
                TableCellRenderer renderer = new TableCellRenderer(){
                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        JLabel label;
                        int boardIdx = (Integer)value;
                        if (boardIdx >= boards.size()) {
                            label = new JLabel(String.format("%d. (invalid)", boardIdx));
                        } else {
                            label = new JLabel(boardSelectArray[boardIdx]);
                        }
                        if (isSelected) {
                            label.setOpaque(true);
                            label.setBackground(table.getSelectionBackground());
                            label.setForeground(table.getSelectionForeground());
                        }
                        return label;
                    }
                };

                switch (columnIndex) {
                    case COL_EXITN:
                    case COL_EXITS:
                    case COL_EXITE:
                    case COL_EXITW:
                        return renderer;
                    default:
                        return super.getCellRenderer(rowIndex, columnIndex);
                }
            }
            @Override
            public TableCellEditor getCellEditor(int rowIndex, int columnIndex) {
                switch (columnIndex) {
                    case COL_EXITN:
                    case COL_EXITS:
                    case COL_EXITE:
                    case COL_EXITW:
                        JComboBox<String> exitBox = new JComboBox<>(boardSelectArray);

                        Board board = boards.get(table.convertRowIndexToModel(rowIndex));
                        int boardExit = board.getExit(columnToExit(columnIndex));
                        if (boardExit >= 0 && boardExit < boardSelectArray.length) {
                            exitBox.setSelectedIndex(boardExit);
                        } else {
                            exitBox.setEditable(true);
                        }
                        return new DefaultCellEditor(exitBox);
                    default:
                        return super.getCellEditor(rowIndex, columnIndex);
                }
            }
        };
        table.setAutoCreateRowSorter(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getSelectionModel().addListSelectionListener(e -> {
            updateButtons();
        });

        JScrollPane scroll = new JScrollPane(table);
        JPanel buttonPanel = new JPanel(new GridLayout(0, 1));
        upButton = new JButton("↑");
        downButton = new JButton("↓");
        delButton = new JButton("×");
        exportButton = new JButton("Export");
        updateButtons();
        upButton.addActionListener(e -> moveSelected(-1));
        downButton.addActionListener(e -> moveSelected(1));
        delButton.addActionListener(e -> delSelected());
        exportButton.addActionListener(e -> exportSelected());
        String note = " (note: This will change board exits and passages.)";
        upButton.setToolTipText("Move selected boards up" + note);
        downButton.setToolTipText("Move selected boards down" + note);
        delButton.setToolTipText("Delete selected boards" + note);
        exportButton.setToolTipText("Export selected boards");

        buttonPanel.add(upButton);
        buttonPanel.add(downButton);
        buttonPanel.add(delButton);
        buttonPanel.add(exportButton);
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.add(scroll, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.EAST);
        dialog.pack();
        dialog.setLocationRelativeTo(editor.getFrameForRelativePositioning());
        dialog.setVisible(true);
    }

    private void exportSelected() {
        if (table.getSelectedRows().length == 0) return;
        var fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(editor.getGlobalEditor().getDefaultDirectory());
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);
        if (fileChooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
            File targetDir = fileChooser.getSelectedFile();
            var boards = editor.getBoards();
            for (int viewRow : table.getSelectedRows()) {
                int modelRow = table.convertRowIndexToModel(viewRow);
                File file = new File(targetDir, Integer.toString(modelRow) + ".brd");
                try {
                    boards.get(modelRow).saveTo(file);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(dialog, e, "Error exporting board", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void updateBoardSelectArray() {
        boardSelectArray = generateBoardSelectArray(boards, true);
    }

    public static String[] generateBoardSelectArray(java.util.List<Board> boards, boolean numPrefix) {
        String[] boardList = new String[boards.size()];
        boardList[0] = new String("(no board)");
        for (int i = 1; i < boardList.length; i++) {
            String name = CP437.toUnicode(boards.get(i).getName());
            boardList[i] = numPrefix ? String.format("%d. %s", i, name) : name;
        }
        return boardList;
    }

    private void updateButtons() {
        upButton.setEnabled(true);
        downButton.setEnabled(true);
        delButton.setEnabled(true);
        var rows = table.getSelectedRows();

        // Can't do anything if no rows are selected, or if all the rows are selected
        if (rows.length == 0 || rows.length == boards.size()) {
            upButton.setEnabled(false);
            downButton.setEnabled(false);
            delButton.setEnabled(false);
            return;
        }

        // Can't move up if the top row is selected or down if the bottom row is selected
        for (var viewRow : rows) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            if (modelRow == 0) upButton.setEnabled(false);
            if (modelRow == boards.size() - 1) downButton.setEnabled(false);
        }
    }

    private int[] defaultMapping() {
        var remapping = new int[boards.size()];
        for (int i = 0; i < remapping.length; i++) {
            remapping[i] = i;
        }
        return remapping;
    }

    private void delSelected() {
        table.getRowSorter().setSortKeys(null);

        var remapping = defaultMapping();
        for (int viewRow : table.getSelectedRows()) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            remapping[modelRow] = -1;
        }

        // Collapse it down
        int write_head = 0;
        for (int read_head = 0; read_head < remapping.length; read_head++) {
            if (remapping[read_head] == -1) continue;
            remapping[write_head] = remapping[read_head];
            write_head++;
        }
        while (write_head < remapping.length) {
            remapping[write_head++] = -1;
        }

        doRemap(remapping, "deletion");
    }

    private void doRemap(int[] remapping, String desc) {
        var newBoardList = new ArrayList<Board>();
        var newWorldData = worldData.clone();
        var oldToNew = new HashMap<Integer, Integer>();

        // Create new board list through cloning. Keep the old just in case
        for (int i = 0; i < remapping.length; i++) {
            if (remapping[i] != -1) {
                Board clonedBoard = boards.get(remapping[i]).clone();
                if (i != remapping[i]) clonedBoard.setDirty();
                newBoardList.add(clonedBoard);
                oldToNew.put(remapping[i], i);
            }
        }

        CompatWarning w = new CompatWarning(newWorldData.isSuperZZT());

        // Deleted first board?
        if (remapping[0] != 0) {
            w.warn(1, "This operation will change the title screen.");
        }

        // Update first board / current board
        if (oldToNew.containsKey(newWorldData.getCurrentBoard())) {
            newWorldData.setCurrentBoard(oldToNew.get(newWorldData.getCurrentBoard()));
        } else {
            w.warn(1, "Deleting world's first/current board. First/current board will be changed to title screen.");
            newWorldData.setCurrentBoard(0);
        }

        for (int boardIdx = 0; boardIdx < newBoardList.size(); boardIdx++) {
            Board board = newBoardList.get(boardIdx);
            int oldBoardIdx = remapping[boardIdx];
            w.setPrefix(String.format("Board #%d \"%s\"", oldBoardIdx, CP437.toUnicode(board.getName())));

            // Check exits. If we remap an exit to 0 or a nonexistent board we must give a warning
            for (int exit = 0; exit < 4; exit++) {
                int oldDestination = board.getExit(exit);
                if (oldDestination != 0 && oldDestination < boards.size()) {
                    if (oldToNew.containsKey(oldDestination)) {
                        int newDestination = oldToNew.get(oldDestination);
                        if (newDestination == 0) {
                            w.warn(1, String.format("'s %s exit will lead to the title screen. That exit will be disabled.", EXIT_NAMES[exit]));
                        }
                        if (newDestination != oldDestination) {
                            board.setExit(exit, newDestination);
                            board.setDirty();
                        }
                    } else {
                        w.warn(1, String.format("'s %s exit leads to a board being deleted. That exit will be disabled.", EXIT_NAMES[exit]));
                        board.setExit(exit, 0);
                        board.setDirty();
                    }
                }
            }

            // Check passages. We can only check passages that currently exist, so hopefully there's no stat muckery
            for (int statIdx = 0; statIdx < board.getStatCount(); statIdx++) {
                Stat stat = board.getStat(statIdx);
                int x = stat.getX() - 1;
                int y = stat.getY() - 1;
                if (x >= 0 && y >= 0 && x < board.getWidth() && y < board.getHeight()) {
                    int tid = board.getTileId(x, y);

                    // Passages are the same in ZZT and SuperZZT
                    if (tid == ZType.PASSAGE) {
                        int oldDestination = stat.getP3();
                        if (oldDestination < boards.size()) {
                            if (oldToNew.containsKey(oldDestination)) {
                                int newDestination = oldToNew.get(oldDestination);
                                if (newDestination != oldDestination) {
                                    stat.setP3(newDestination);
                                    board.dirtyStats();
                                }
                            } else {
                                w.warn(1, String.format(" has a passage at %d,%d pointing to a board being deleted. It will now point to the title screen.", x + 1, y + 1));
                                stat.setP3(0);
                                board.dirtyStats();
                            }
                        }
                    }
                }
            }
        }

        if (w.getWarningLevel() == 0) {
            for (int i : remapping) {
                if (i == -1) {
                    // For any deletion, warn the user
                    w.setPrefix("");
                    w.warn(1, "Are you sure?");
                    break;
                }
            }
        }

        if (w.getWarningLevel() > 0) {
            int result = JOptionPane.showConfirmDialog(dialog, w.getMessages(1),
                    "Board " + desc + " warning",  JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return;
        }

        // Now remap all the selected boards
        var selectedRows = new ArrayList<Integer>();

        for (int viewRow : table.getSelectedRows()) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            if (oldToNew.containsKey(modelRow)) {
                int remappedModelRow = oldToNew.get(modelRow);
                selectedRows.add(remappedModelRow);
            }
        }

        // Update data structures here and in WorldEditor
        boards = newBoardList;
        worldData = newWorldData;
        updateBoardSelectArray();
        editor.setWorldData(newWorldData);
        editor.replaceBoardList(newBoardList);

        int oldCurrentBoardIdx = editor.getBoardIdx();
        int newCurrentBoardIdx = 0;
        if (oldToNew.containsKey(oldCurrentBoardIdx)) {
            newCurrentBoardIdx = oldToNew.get(oldCurrentBoardIdx);
        }
        //if (newCurrentBoardIdx != oldCurrentBoardIdx) {
        editor.changeBoard(newCurrentBoardIdx);
        //}

        tableModel.fireTableDataChanged();
        table.clearSelection();

        for (int remappedModelRow : selectedRows) {
            int remappedViewRow = table.convertRowIndexToView(remappedModelRow);
            table.addRowSelectionInterval(remappedViewRow, remappedViewRow);
        }

        table.repaint();
    }

    private void moveSelected(int offset) {
        table.getRowSorter().setSortKeys(null);
        var remapping = defaultMapping();
        boolean[] selected = new boolean[remapping.length];
        for (int viewRow : table.getSelectedRows()) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            selected[modelRow] = true;
        }

        if (offset == 1) {
            // 0 1 2 3 4
            // 0[1 2]3 4
            // 0 3[1 2]4

            //   * *
            // 0 1 2 3 4
            // 0 1 2{3 4} - n
            // 0 1{2 3}4  - y
            // 0 1{3 2}4  - *
            // 0{1 3}2 4  - y
            // 0{3 1}2 4  - *
            //{0 3}1 2 4  - n
            for (int idx = remapping.length - 2; idx >= 0; idx--) {
                if (selected[idx]) {
                    int t = remapping[idx + 1];
                    remapping[idx + 1] = remapping[idx];
                    remapping[idx] = t;
                }
            }
        } else if (offset == -1) {
            // 0 1 2 3 4
            // 0[1 2]3 4
            // 0 1[2 3]4
            for (int idx = 1; idx < remapping.length; idx++) {
                if (selected[idx]) {
                    int t = remapping[idx - 1];
                    remapping[idx - 1] = remapping[idx];
                    remapping[idx] = t;
                }
            }
        }

        doRemap(remapping, "reordering");
    }

    private int columnToExit(int column) {
        switch (column) {
            case COL_EXITN: return 0;
            case COL_EXITS: return 1;
            case COL_EXITE: return 3;
            case COL_EXITW: return 2;
            default: return -1;
        }
    }
}
