package zedit2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class BoardSelector {
    private JDialog dialog;
    private WorldEditor editor;
    private ActionListener listener;
    private JList<String> boardList;
    private ArrayList<Board> boards;
    public BoardSelector(WorldEditor editor, ArrayList<Board> boards, ActionListener listener) {
        this.editor = editor;
        this.listener = listener;
        this.boards = boards;
        dialog = new JDialog();
        Util.addEscClose(dialog, dialog.getRootPane());
        dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setTitle("Select a board");
        dialog.setIconImage(editor.getCanvas().extractCharImage(240, 0x0F, 2, 2, false, "$"));

        String[] boardNames = new String[boards.size() + 1];
        for (int i = 0; i < boards.size(); i++) {
            boardNames[i] = CP437.toUnicode(boards.get(i).getName());
            if (boardNames[i].length() < 20) boardNames[i] = (boardNames[i] + "                    ").substring(0, 20);
        }
        boardNames[boards.size()] = "(add board)";

        boardList = new JList<>(boardNames);
        boardList.setSelectedIndex(editor.getBoardIdx());
        boardList.setBackground(new Color(0x0000AA));
        boardList.setForeground(new Color(0xFFFFFF));
        boardList.setFont(CP437.getFont());
        boardList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        var boardSelector = this;
        boardList.addKeyListener(new KeyAdapter(){
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    boardSelector.selectBoard();
                    e.consume();
                }
            }
        });
        boardList.addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    boardSelector.selectBoard();
                    e.consume();
                }
            }
        });

        boardList.addListSelectionListener(e -> {
            int boardIdx = boardList.getSelectedIndex();
            if (boardIdx < boards.size()) {
                listener.actionPerformed(new ActionEvent(boardSelector, ActionEvent.ACTION_PERFORMED, String.valueOf(boardIdx)));
            }
        });

        JScrollPane scroll = new JScrollPane(boardList);
        dialog.getContentPane().add(scroll);
        dialog.pack();
        boardList.ensureIndexIsVisible(editor.getBoardIdx());
        dialog.setLocationRelativeTo(editor.getFrameForRelativePositioning());
        dialog.setVisible(true);
    }

    private void selectBoard() {
        int boardIdx = boardList.getSelectedIndex();
        dialog.dispose();
        if (boardIdx == boards.size()) {
            editor.operationAddBoard();
        } else {
            listener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, String.valueOf(boardIdx)));
        }
    }

}
