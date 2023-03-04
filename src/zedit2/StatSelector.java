package zedit2;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.*;

public class StatSelector {
    private static final String[] colNames = {"", "#", "Type", "Colour", "X", "Y", "Name", "Code Len", "Cycle", "X-Step", "Y-Step",
            "P1", "P2", "P3", "Follower", "Leader", "Instr. Ptr", "Under ID", "Under Col", "Order"};
    private static final int COL_IMAGE = 0;
    private static final int COL_STATID = 1;
    private static final int COL_TYPE = 2;
    private static final int COL_COLOUR = 3;
    private static final int COL_X = 4;
    private static final int COL_Y = 5;
    private static final int COL_NAME = 6;
    private static final int COL_CODELEN = 7;
    private static final int COL_CYCLE = 8;
    private static final int COL_STEPX = 9;
    private static final int COL_STEPY = 10;
    private static final int COL_P1 = 11;
    private static final int COL_P2 = 12;
    private static final int COL_P3 = 13;
    private static final int COL_FOLLOWER = 14;
    private static final int COL_LEADER = 15;
    private static final int COL_IP = 16;
    private static final int COL_UID = 17;
    private static final int COL_UCO = 18;
    private static final int COL_ORDER = 19;

    /*
case COL_IMAGE:
case COL_STATID:
case COL_TYPE:
case COL_COLOUR:
case COL_X:
case COL_Y:
case COL_NAME:
case COL_CYCLE:
case COL_STEPX:
case COL_STEPY:
case COL_P1:
case COL_P2:
case COL_P3:
case COL_FOLLOWER:
case COL_LEADER:
case COL_IP:
case COL_CODELEN:
case COL_UID:
case COL_UCO:
     */
    private JDialog dialog = null;
    private WorldEditor editor = null;
    private ActionListener listener = null;
    private Board board = null;
    private JTable table = null;
    private AbstractTableModel tableModel = null;
    private String[] options;

    public StatSelector(WorldEditor editor, Board board, ActionListener listener, String[] options)
    {
        this.editor = editor;
        this.board = board;
        this.listener = listener;
        this.options = options;
        dialog = new JDialog();
        Util.addEscClose(dialog, dialog.getRootPane());
        dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setTitle("Stat list :: Board #" + editor.getBoardIdx() + " :: " + CP437.toUnicode(board.getName()) + " :: Double-click to select");
        dialog.setIconImage(editor.getCanvas().extractCharImage(240, 0x1F, 2, 2, false, "$"));
        dialog.addWindowListener(new WindowAdapter(){
            @Override
            public void windowClosed(WindowEvent e) {
                editor.getCanvas().setIndicate(null, null);
            }
        });

        tableModel = new AbstractTableModel(){

            @Override
            public int getRowCount() {
                return board.getStatCount();
            }

            @Override
            public int getColumnCount() {
                return colNames.length;
            }

            @Override
            public String getColumnName(int columnIndex) {
                return colNames[columnIndex];
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                    case COL_IMAGE:
                    case COL_COLOUR:
                    case COL_UCO:
                        return Icon.class;
                    case COL_STATID:
                    case COL_X:
                    case COL_Y:
                    case COL_CYCLE:
                    case COL_STEPX:
                    case COL_STEPY:
                    case COL_P1:
                    case COL_P2:
                    case COL_P3:
                    case COL_FOLLOWER:
                    case COL_LEADER:
                    case COL_IP:
                    case COL_CODELEN:
                    case COL_ORDER:
                        return Integer.class;
                    case COL_TYPE:
                    case COL_NAME:
                    case COL_UID:
                        return String.class;
                    default:
                        return null;
                }
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                var stat = board.getStat(rowIndex);
                int x = stat.getX() - 1;
                int y = stat.getY() - 1;
                Tile tile;
                if (x >= 0 && y >= 0 && x < board.getWidth() && y < board.getHeight()) {
                    tile = board.getTile(x, y, false);
                } else {
                    tile = new Tile(-1, -1, board.getStatsAt(x, y));
                }
                var worldData = editor.getWorldData();
                var szzt = worldData.isSuperZZT();
                var canvas = editor.getCanvas();

                switch (columnIndex) {
                    case COL_IMAGE: {
                        int chr = ZType.getChar(worldData.isSuperZZT(), tile);
                        int col = ZType.getColour(worldData.isSuperZZT(), tile);
                        return new ImageIcon(canvas.extractCharImage(chr, col, worldData.isSuperZZT() ? 2 : 1, 1, false, "$"));
                    }
                    case COL_STATID:
                        return stat.getStatId();
                    case COL_TYPE:
                        return ZType.getName(szzt, tile.getId());
                    case COL_COLOUR: {
                        int col = tile.getCol();
                        return new ImageIcon(canvas.extractCharImage(0, col, 1, 1, false, "_#_"));
                    }
                    case COL_X:
                        return x + 1;
                    case COL_Y:
                        return y + 1;
                    case COL_NAME: {
                        int codeLen = stat.getCodeLength();
                        if (codeLen >= 0) {
                            return stat.getName();
                        } else {
                            int boundTo = -codeLen;
                            if (boundTo < board.getStatCount()) {
                                return board.getStat(boundTo).getName();
                            }
                        }
                    }
                    case COL_CYCLE:
                        return stat.getCycle();
                    case COL_STEPX:
                        return stat.getStepX();
                    case COL_STEPY:
                        return stat.getStepY();
                    case COL_P1:
                        return stat.getP1();
                    case COL_P2:
                        return stat.getP2();
                    case COL_P3:
                        return stat.getP3();
                    case COL_FOLLOWER:
                        return stat.getFollower();
                    case COL_LEADER:
                        return stat.getLeader();
                    case COL_IP:
                        return stat.getIp();
                    case COL_CODELEN:
                        return stat.getCodeLength();
                    case COL_UID:
                        return ZType.getName(szzt, stat.getUid());
                    case COL_UCO: {
                        int col = stat.getUco();
                        return new ImageIcon(canvas.extractCharImage(0, col, 1, 1, false, "_#_"));
                    }
                    case COL_ORDER:
                        return stat.getOrder();
                    default:
                        return null;
                }
            }
        };
        table = new JTable(tableModel);
        table.setAutoCreateRowSorter(true);

        // Set column preferred widths
        table.getColumnModel().getColumn(COL_IMAGE).setPreferredWidth(editor.getWorldData().isSuperZZT() ? 16 : 8);
        table.getColumnModel().getColumn(COL_STATID).setPreferredWidth(26);
        table.getColumnModel().getColumn(COL_TYPE).setPreferredWidth(64);
        table.getColumnModel().getColumn(COL_COLOUR).setPreferredWidth(25);
        table.getColumnModel().getColumn(COL_X).setPreferredWidth(20);
        table.getColumnModel().getColumn(COL_Y).setPreferredWidth(20);
        table.getColumnModel().getColumn(COL_NAME).setPreferredWidth(64);
        table.getColumnModel().getColumn(COL_CYCLE).setPreferredWidth(36);
        table.getColumnModel().getColumn(COL_STEPX).setPreferredWidth(42);
        table.getColumnModel().getColumn(COL_STEPY).setPreferredWidth(42);
        table.getColumnModel().getColumn(COL_P1).setPreferredWidth(27);
        table.getColumnModel().getColumn(COL_P2).setPreferredWidth(27);
        table.getColumnModel().getColumn(COL_P3).setPreferredWidth(27);
        table.getColumnModel().getColumn(COL_FOLLOWER).setPreferredWidth(50);
        table.getColumnModel().getColumn(COL_LEADER).setPreferredWidth(43);
        table.getColumnModel().getColumn(COL_IP).setPreferredWidth(55);
        table.getColumnModel().getColumn(COL_CODELEN).setPreferredWidth(55);
        table.getColumnModel().getColumn(COL_UID).setPreferredWidth(64);
        table.getColumnModel().getColumn(COL_UCO).setPreferredWidth(25);
        table.getColumnModel().getColumn(COL_ORDER).setPreferredWidth(38);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        var statSelector = this;
        table.addKeyListener(new KeyAdapter(){
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) > 0) {
                        statSelector.selectRow(1);
                    } else {
                        statSelector.selectRow(0);
                    }
                    e.consume();
                }
            }
        });
        table.addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (e.getClickCount() == 2) {
                        statSelector.selectRow(0);

                    }
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row == -1) return;
                    table.clearSelection();
                    table.addRowSelectionInterval(row, row);
                    var popupMenu = new JPopupMenu();
                    for (int i = 0; i < options.length; i++) {
                        var optionIdx = i;
                        var option = options[i];
                        var menuItem = new JMenuItem(option);
                        menuItem.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                statSelector.selectRow(optionIdx);
                            }
                        });
                        popupMenu.add(menuItem);
                    }
                    popupMenu.show(dialog, e.getXOnScreen() - dialog.getLocationOnScreen().x,
                            e.getYOnScreen() - dialog.getLocationOnScreen().y);
                }
            }
        });
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                var canvas = editor.getCanvas();
                var rows = table.getSelectedRows();
                int[] indicateX = new int[rows.length];
                int[] indicateY = new int[rows.length];
                for (int i = 0; i < rows.length; i++) {
                    int row = table.convertRowIndexToModel(rows[i]);
                    int x = board.getStat(row).getX() - 1;
                    int y = board.getStat(row).getY() - 1;
                    if (x >= 0 && y >= 0 && x < board.getWidth() && y < board.getHeight()) {
                        x += editor.getBoardXOffset();
                        y += editor.getBoardYOffset();
                        indicateX[i] = x;
                        indicateY[i] = y;
                    } else {
                        indicateX[i] = -1;
                        indicateY[i] = -1;
                    }
                }
                canvas.setIndicate(indicateX, indicateY);
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        dialog.getContentPane().add(scroll);
        dialog.pack();
        dialog.setLocationRelativeTo(editor.getFrameForRelativePositioning());
        dialog.setVisible(true);
    }

    public static int getStatIdx(String actionCommand) {
        return Integer.parseInt(actionCommand.split("\\|")[1]);
    }

    public static int getOption(String actionCommand) {
        return Integer.parseInt(actionCommand.split("\\|")[0]);
    }

    private void selectRow(int option) {
        option = Util.clamp(option, 0, options.length - 1);
        if (table.getSelectedRow() == -1) return;
        int selectedRow = table.convertRowIndexToModel(table.getSelectedRow());
        if (listener != null) {
            String command = String.format("%d|%d", option, selectedRow);
            var actionEvent = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, command);
            this.listener.actionPerformed(actionEvent);
        }
    }

    public void close() {
        dialog.dispose();
    }
    public void dataChanged() {
        int selectedRow = table.convertRowIndexToModel(table.getSelectedRow());
        tableModel.fireTableDataChanged();
        if (selectedRow < table.getRowCount()) {
            int viewRow = table.convertRowIndexToView(selectedRow);
            table.addRowSelectionInterval(viewRow, viewRow);
        }
    }
}
