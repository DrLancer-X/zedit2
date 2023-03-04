package zedit2;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import java.util.function.Predicate;

public class ElementListModel extends AbstractListModel<String> {
    private int f;
    private ArrayList<String> itemVec;
    public ElementListModel(GlobalEditor ge, int f) {
        this.f = f;

        itemVec = new ArrayList<>();
        for (int i = 0;; i++) {
            var key = String.format("F%d_MENU_%d", f, i);
            var val = ge.getString(key, "");
            if (val.isEmpty()) break;
            itemVec.add(val);
        }
    }

    public static ListCellRenderer<? super String> getRenderer() {
        ListCellRenderer<? super String> renderer;
        renderer = (ListCellRenderer<String>) (list, value, index, isSelected, cellHasFocus) -> {
            if (!list.hasFocus()) {
                isSelected = false;
            }
            var panel = new JPanel(new BorderLayout());

            int openBrace = value.indexOf('(');
            if (openBrace != -1) {
                int closeBrace = value.lastIndexOf(')');
                if (closeBrace + 1 == value.length()) {
                    value = value.substring(0, openBrace);
                } else {
                    value = value.substring(closeBrace + 1);
                }
            } else {
                int exclPoint = value.indexOf('!');
                if (exclPoint != -1) {
                    value = value.substring(exclPoint + 1);
                }
            }

            var lbl = new JLabel(value);
            if (isSelected) {
                lbl.setForeground(Color.WHITE);
                panel.setBackground(Color.BLUE);
            } else {
                panel.setBackground(Color.WHITE);
            }
            if (cellHasFocus) {
                panel.setBorder(BorderFactory.createEtchedBorder());
            }
            panel.add(lbl, BorderLayout.WEST);
            return panel;
        };
        return renderer;
    }

    public static TransferHandler getTransferHandler() {
        TransferHandler transferHandler = new TransferHandler() {
            @Override
            public int getSourceActions(JComponent c) {
                return TransferHandler.MOVE;
            }

            @Override
            protected Transferable createTransferable(JComponent source) {
                @SuppressWarnings("unchecked")
                JList<String> sourceList = (JList<String>) source;
                var data = sourceList.getSelectedValuesList();
                if (data == null) return null;
                return new StringsSelection(data);
            }

            @Override
            protected void exportDone(JComponent source, Transferable data, int action) {
                if (data == null) return;
                if (action != TransferHandler.MOVE) return;
                @SuppressWarnings("unchecked")
                JList<String> sourceList = (JList<String>) source;
                ElementListModel model = (ElementListModel) sourceList.getModel();
                model.remove(sourceList.getSelectedIndices());
            }

            @Override
            public boolean canImport(TransferHandler.TransferSupport support) {
                return support.isDataFlavorSupported(StringsSelection.flavor);
            }

            @Override
            public boolean importData(TransferHandler.TransferSupport support) {
                System.out.println("importData");
                if (!canImport(support)) return false;
                System.out.println("passed");
                @SuppressWarnings("unchecked")
                var dropOn = (JList<String>) support.getComponent();
                var model = (ElementListModel) dropOn.getModel();
                var loc = support.getDropLocation().getDropPoint();
                var dropIdx = dropOn.locationToIndex(loc);
                boolean dropOnto = false;
                if (dropIdx != -1) {
                    dropOnto = dropOn.getCellBounds(dropIdx, dropIdx).contains(loc);
                }
                var tr = support.getTransferable();
                try {
                    var strings = (List<String>) tr.getTransferData(StringsSelection.flavor);
                    model.insert(dropIdx, strings, dropOnto);
                } catch (UnsupportedFlavorException | IOException e) {
                    return false;
                }
                return true;
            }

        };
        var img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);

        transferHandler.setDragImage(img);
        return transferHandler;
    }

    public void clear() {
        if (itemVec.size() == 0) return;
        int lastItem = itemVec.size() - 1;
        itemVec.clear();
        fireIntervalRemoved(this, 0, lastItem);
    }

    private void remove(int[] toRemove) {
        if (toRemove.length == 0) return;
        for (int i = toRemove.length - 1; i >= 0; i--) {
            int idx = toRemove[i];
            itemVec.remove(idx);
        }
        fireIntervalRemoved(this, toRemove[0], toRemove[toRemove.length - 1]);
    }

    public void insert(int dropIdx, List<String> insertItems, boolean dropOnto) {
        if (insertItems.isEmpty()) return;
        int posStart, posEnd;
        if (!dropOnto) {
            for (var str : insertItems) {
                itemVec.add(str);
            }
            posStart = itemVec.size() - insertItems.size();
            posEnd = itemVec.size() - 1;
        } else {
            posStart = dropIdx;
            posEnd = dropIdx;
            for (var str : insertItems) {
                itemVec.add(dropIdx, str);
                posEnd = dropIdx;
                dropIdx++;
            }
        }
        fireIntervalAdded(this, posStart, posEnd);
    }

    @Override
    public int getSize() {
        return itemVec.size();
    }

    @Override
    public String getElementAt(int index) {
        return itemVec.get(index);
    }
}
