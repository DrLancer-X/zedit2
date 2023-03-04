package zedit2;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.*;
import java.io.IOException;

public class BufferManager extends JDialog implements WindowListener, MouseListener {
    private WorldEditor editor;
    private JList<String> list;
    private BufferModel listModel;
    private JScrollPane scrollPane;
    private static final int MAX_HEIGHT = 640;
    public BufferManager(WorldEditor editor) {
        this.editor = editor;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(this);
        setIconImage(editor.getCanvas().extractCharImage(228, 0x5F, 1, 1, false, "$"));
        setTitle("Buffer Manager");
        getContentPane().setLayout(new BorderLayout());
        listModel = new BufferModel(this, editor);
        list = new JList<>(listModel);
        list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        list.setCellRenderer(listModel);
        list.addMouseListener(this);
        list.setDropMode(DropMode.ON);
        list.setDragEnabled(true);
        // http://www.java2s.com/example/java/swing/drag-and-drop-custom-transferhandler-for-a-jlist.html
        list.setTransferHandler(new TransferHandler() {
            @Override
            public int getSourceActions(JComponent c) {
                return TransferHandler.COPY_OR_MOVE;
            }

            @Override
            protected Transferable createTransferable(JComponent source) {
                @SuppressWarnings("unchecked")
                JList<String> sourceList = (JList<String>) source;
                String data = sourceList.getSelectedValue();
                if (data == null) return null;
                Transferable t = new StringSelection(String.format("%d:%s", sourceList.getSelectedIndex(), data));
                return t;
            }

            @Override
            protected void exportDone(JComponent source, Transferable data, int action) {
                @SuppressWarnings("unchecked")
                JList<String> sourceList = (JList<String>) source;
                if (data == null) return;
                int from = -1;
                try {
                    var stringData = (String)(data.getTransferData(DataFlavor.stringFlavor));
                    from = BufferManager.getBufferDataIdx(stringData);
                } catch (UnsupportedFlavorException | IOException e) {
                    e.printStackTrace();
                }

                if (action == TransferHandler.MOVE && from != -1) {
                    BufferModel listModel = (BufferModel) sourceList.getModel();
                    listModel.remove(from);
                }
            }

            @Override
            public boolean canImport(TransferHandler.TransferSupport support) {
                if (!support.isDrop()) {
                    return false;
                }
                return support.isDataFlavorSupported(DataFlavor.stringFlavor);
            }

            @Override
            public boolean importData(TransferHandler.TransferSupport support) {
                if (!this.canImport(support)) {
                    return false;
                }
                Transferable t = support.getTransferable();
                String data = null;
                try {
                    data = (String) t.getTransferData(DataFlavor.stringFlavor);
                } catch (UnsupportedFlavorException | IOException e) {
                    e.printStackTrace();
                    return false;
                }
                int from = getBufferDataIdx(data);

                JList.DropLocation dropLocation = (JList.DropLocation) support.getDropLocation();

                int dropIndex = dropLocation.getIndex();
                @SuppressWarnings("unchecked")
                JList<String> targetList = (JList<String>) support.getComponent();

                // Do insert

                BufferModel listModel = (BufferModel) targetList.getModel();

                if (dropLocation.isInsert()) {
                    return listModel.add(dropIndex, from, data);
                } else {
                    return listModel.set(dropIndex, from, data);
                }
            }
        });
        //addWindowFocusListener(this);
        setResizable(false);
        scrollPane = new JScrollPane(list);
        //scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        //scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        resizeList();
        setLocationRelativeTo(editor.getFrameForRelativePositioning());
        setFocusableWindowState(false);
        setAlwaysOnTop(true);
        setVisible(true);
    }

    public static int getBufferDataIdx(String stringData) {
        int colonPos = stringData.indexOf(':');
        return Integer.parseInt(stringData.substring(0, colonPos));
    }
    public static String getBufferDataString(String stringData) {
        int colonPos = stringData.indexOf(':');
        return stringData.substring(colonPos + 1);
    }

    public void updateBuffer(int num) {
        listModel.updateBuffer(num);
    }

    public void updateSelected(int num) {
        listModel.updateSelected(num);
    }

    public void windowClosed(WindowEvent e) {
        editor.removeBufferManager();
    }

    public void mouseClicked(MouseEvent e) {
        int cell = list.locationToIndex(e.getPoint());
        if (cell == -1) return;
        var bounds = list.getCellBounds(cell, cell);
        if (bounds == null) return;
        int x = e.getX();
        int y = e.getY();
        if (x >= bounds.x && y >= bounds.y && x < bounds.x + bounds.width && y < bounds.y + bounds.height) {
            editor.operationGetFromBuffer(listModel.idxToBufNum(cell));
        }

        e.consume();
    }

    public void windowOpened(WindowEvent e) { }
    public void windowClosing(WindowEvent e) { }
    public void windowIconified(WindowEvent e) { }
    public void windowDeiconified(WindowEvent e) { }
    public void windowActivated(WindowEvent e) { }
    public void windowDeactivated(WindowEvent e) { }

    public void mousePressed(MouseEvent e) { }
    public void mouseReleased(MouseEvent e) { }
    public void mouseEntered(MouseEvent e) { }
    public void mouseExited(MouseEvent e) { }

    public void resizeList() {
        if (list != null) {
            //System.out.println("Before (actual): " + scrollPane.getSize());
            //int beforeHeight = scrollPane.getHeight();
            //int beforeWidth = scrollPane.getWidth();
            list.setVisibleRowCount((listModel.getSize() + 4) / 5);
            //int afterHeight = scrollPane.getPreferredSize().height;
            //int afterWidth = scrollPane.getPreferredSize().width;
            //int maxHeight = Math.max(beforeHeight, afterHeight);
            //int maxWidth = Math.max(beforeWidth, afterWidth);

            //if (beforeHeight < MAX_HEIGHT && afterHeight > MAX_HEIGHT) {
//                maxHeight = Math.min(maxHeight, MAX_HEIGHT);
//            }
//            scrollPane.setPreferredSize(new Dimension(maxWidth, maxHeight));
            pack();
            //if (maxHeight > MAX_HEIGHT) {
            //    scrollPane.setPreferredSize(new Dimension(maxWidth, maxHeight));
            //}

        }
    }
}
