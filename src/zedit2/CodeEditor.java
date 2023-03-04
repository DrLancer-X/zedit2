package zedit2;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.regex.Pattern;

import static zedit2.Util.keyMatches;
import static zedit2.Util.pair;

public class CodeEditor implements KeyListener, MouseMotionListener {
    private Stat stat;
    private JDialog form;
    private JTextComponent editor;
    private ActionListener listener;
    private String title;
    private boolean readOnly;
    private WorldEditor worldEditor;
    private CodeDocument cd;
    private Image icon;
    private String lastText;

    private KeyStroke k_AltT, k_Escape, k_CtrlD, k_CtrlF, k_CtrlH, k_AltM, k_CtrlZ, k_CtrlY, k_F3, k_transposeDown, k_transposeUp;
    private JDialog findDialog;
    private boolean selFromRep;
    private boolean recordUndo;

    private ArrayList<Undo> currentUndo = new ArrayList<>();
    private ArrayList<ArrayList<Undo>> undoList = new ArrayList<>();
    private int undoPos = 0;

    private boolean musicMode = false;


    private void keySetup() {
        k_AltM = Util.getKeyStroke(worldEditor.getGlobalEditor(), "Alt-M");
        k_AltT = Util.getKeyStroke(worldEditor.getGlobalEditor(), "Alt-T");
        k_CtrlD = Util.getKeyStroke(worldEditor.getGlobalEditor(), "Ctrl-D");
        k_CtrlF = Util.getKeyStroke(worldEditor.getGlobalEditor(), "Ctrl-F");
        k_CtrlH = Util.getKeyStroke(worldEditor.getGlobalEditor(), "Ctrl-H");
        k_CtrlY = Util.getKeyStroke(worldEditor.getGlobalEditor(), "Ctrl-Y");
        k_CtrlZ = Util.getKeyStroke(worldEditor.getGlobalEditor(), "Ctrl-Z");
        k_transposeDown = Util.getKeyStroke(worldEditor.getGlobalEditor(), "Ctrl--");
        k_transposeUp = Util.getKeyStroke(worldEditor.getGlobalEditor(), "Ctrl-=");
        k_Escape = Util.getKeyStroke(worldEditor.getGlobalEditor(), "Escape");
        k_F3 = Util.getKeyStroke(worldEditor.getGlobalEditor(), "F3");
    }

    @Override
    public void mouseDragged(MouseEvent e) { }

    @Override
    public void mouseMoved(MouseEvent e) {
        var pt = e.getPoint();
        var pos = editor.viewToModel2D(pt);
        String warning = cd.getWarning(pos);
        editor.setToolTipText(warning);
    }

    private static class Playing {
        private static ArrayList<Integer> lineStarts;
        private static Audio audio;
    }

    public CodeEditor(Image icon, Stat stat, WorldEditor worldEditor, ActionListener listener, boolean readOnly, String title) {
        this.stat = stat;
        this.listener = listener;
        this.title = title;
        this.readOnly = readOnly;
        this.worldEditor = worldEditor;
        this.icon = icon;
        keySetup();
        gui();
    }

    public void gui() {
        form = new JDialog();
        form.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        form.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        form.setIconImage(icon);
        var codeEditor = this;
        form.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (listener != null) {
                    stopAudio();
                    var act = new ActionEvent(codeEditor, ActionEvent.ACTION_PERFORMED, readOnly ? "readOnly" : "update");
                    listener.actionPerformed(act);
                }
                if (findDialog != null) {
                    findDialog.dispose();
                    findDialog = null;
                }
            }
        });
        cd = new CodeDocument(worldEditor);
        cd.putProperty(DefaultEditorKit.EndOfLineStringProperty, "\n");
        editor = new JTextPane(cd) {
            // https://stackoverflow.com/a/23149584
            // Override getScrollableTracksViewportWidth
            // to preserve the full width of the text
            @Override
            public boolean getScrollableTracksViewportWidth() {
                Component parent = getParent();
                var ui = getUI();

                return parent == null || (ui.getPreferredSize(this).width <= parent.getSize().width);
            }
            // http://www.jguru.com/faq/view.jsp?EID=253404
            @Override
            public void setBounds(int x, int y,int width, int height)
            {
                Dimension size = this.getPreferredSize();
                super.setBounds(x, y, Math.max(size.width, width), height);
            }
        };
        editor.setSelectionColor(propCol("EDITOR_SELECTION_COL", "FF0000"));
        editor.setSelectedTextColor(propCol("EDITOR_SELECTED_TEXT_COL", "000000"));
        editor.setCaretColor(propCol("EDITOR_CARET_COL", "00FF00"));
        editor.addMouseMotionListener(this);

        //editor = new JTextArea(25, 60);
        //editor.setDocument(new SqlDocument());
        /*
        editor.setFont(CP437.getFont());

        editor.setForeground(new Color(0xFFFFFF));


         */
        editor.addKeyListener(this);
        var initialText = CP437.toUnicode(stat.getCode(), false);
        editor.setText(initialText);
        lastText = initialText;

        cd.reHighlight();
        editor.setCaretPosition(0);
        recordUndo = true;
        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (recordUndo) addUndo(true, e.getLength(), e.getOffset());
                upd(true, e.getLength(), e.getOffset());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (recordUndo) addUndo(false, e.getLength(), e.getOffset());
                upd(false, e.getLength(), e.getOffset());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }

            private void upd(boolean insert, int len, int offset) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (recordUndo) finishUndo();
                        codeEditor.fullUpdate(insert, len, offset);
                    }
                });
            }
        });
        editor.addCaretListener(new CaretListener() {
            @Override
            public void caretUpdate(CaretEvent e) {
                upd();
            }
        });
        if (readOnly) {
            editor.setEditable(false);
            editor.getCaret().setVisible(true);
            editor.getCaret().setSelectionVisible(true);
            editor.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        }
        editor.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JScrollPane scroll = new JScrollPane(editor);
        int w = worldEditor.getGlobalEditor().getInt("CODEEDITOR_WIDTH");
        int h = worldEditor.getGlobalEditor().getInt("CODEEDITOR_HEIGHT");

        createMenu();
        upd();
        scroll.setPreferredSize(new Dimension(w, h));
        form.getContentPane().add(scroll);
        form.pack();

        form.setLocationRelativeTo(worldEditor.getFrameForRelativePositioning());
        form.setVisible(true);

    }

    private void addUndo(boolean insert, int length, int offset) {
        String text = editor.getText();
        int caret = editor.getCaretPosition();
        if (insert) {
            boolean added = false;
            String addedPortion = text.substring(offset, offset + length);
            if (!currentUndo.isEmpty()) {
                var currentUndoLast = currentUndo.get(currentUndo.size()-1);
                if (currentUndoLast.insert && currentUndoLast.pos + currentUndoLast.text.length() == offset) {
                    currentUndoLast.text += addedPortion;
                    added = true;
                }
            }
            //System.out.printf("Added [%s] at %d\n", addedPortion, offset);
            if (!added) currentUndo.add(new Undo(true, offset, addedPortion, caret));
        } else {
            String deletedPortion = lastText.substring(offset, offset + length);
            boolean added = false;
            if (!currentUndo.isEmpty()) {
                var currentUndoLast = currentUndo.get(currentUndo.size()-1);
                if (!currentUndoLast.insert && offset + length == currentUndoLast.pos) {
                    currentUndoLast.text = deletedPortion + currentUndoLast.text;
                    currentUndoLast.pos = offset;
                    added = true;
                }
            }
            //System.out.printf("Removed [%s] at %d\n", deletedPortion, offset);
            if (!added) currentUndo.add(new Undo(false, offset, deletedPortion, caret));
        }
        lastText = text;
        //System.out.printf("addUndo(%s, %d, %d) - len=%d hash=%d\n", insert, length, offset, editor.getText().length(), editor.getText().hashCode());
    }

    private void finishUndo() {
        if (!currentUndo.isEmpty()) {
            boolean added = false;
            // Can we glue this change onto the last?
            if (undoPos > 0 && currentUndo.size() == 1) {
                var undos = undoList.get(undoPos - 1);
                var undo = undos.get(undos.size() - 1);
                var current = currentUndo.get(0);
                if (undo.insert && current.insert && undo.text.length() < 50) {
                    if (undo.pos + undo.text.length() == current.pos) {
                        undo.text += current.text;
                        added = true;
                    }
                } else if (!undo.insert && !current.insert && undo.text.length() < 50) {
                    if (current.pos + current.text.length() == undo.pos) {
                        undo.text = current.text + undo.text;
                        undo.pos = current.pos;
                        added = true;
                    }
                }
            }
            if (undoPos < undoList.size()) {
                undoList = new ArrayList<>(undoList.subList(0, undoPos));
            }
            if (undoPos != undoList.size()) {
                throw new RuntimeException("undoPos != undoList.size()");
            }
            if (added) {
                currentUndo.clear();
                return;
            }
            undoList.add(currentUndo);
            currentUndo = new ArrayList<>();
            undoPos++;
        }
        //String text = editor.getText();
        //System.out.printf("finishUndo (hash: %d)\n", text.hashCode());
    }

    private Color propCol(String property, String def) {
        return new Color(Integer.parseInt(worldEditor.getGlobalEditor().getString(property, def), 16));
    }

    private void fullUpdate(boolean insert, int len, int offset) {
        //System.out.printf("fullUpdate(%s, %d, %d)\n", insert ? "insert" : "delete", len, offset);
        cd.reHighlight(insert, len, offset);
        upd();
    }

    private void upd() {
        String name;
        if (musicMode) {
            name = "Music editor";
        } else {
            name = "Code editor";
        }
        form.setTitle(String.format("%s :: %s :: %d/%d", name, title, editor.getCaretPosition(), editor.getText().length()));
        if (musicMode) {
            editor.setBackground(propCol("EDITOR_MUSIC_BG", "000000"));
        } else if (readOnly) {
            editor.setBackground(propCol("EDITOR_BIND_BG", "000000"));
        } else {
            editor.setBackground(propCol("EDITOR_BG", "000000"));
        }
    }

    public byte[] getCode() {
        String text = editor.getText();
        if (worldEditor.getGlobalEditor().getBoolean("AUTO_INSERT_NEWLINE")) {
            if (text.length() != 0 && text.charAt(text.length() - 1) != '\n') {
                text += '\n';
            }
        }
        var bytes = CP437.toBytes(text, false);
        return bytes;
    }

    @Override
    public void keyTyped(KeyEvent e) {
        if (musicMode) {
            musicKey(e);
            /*
            try {
                //editor.getDocument().insertString(editor.getCaretPosition(), "" + e.getKeyChar(), null);
            } catch (BadLocationException ex) {
            }

             */
        }
    }

    private void musicKey(KeyEvent e) {
        e.consume();
    }

    private String getCurrentLine() {
        int caret = editor.getCaretPosition();
        int begin = beginningOfLine(caret);
        int end = nextLine(caret);
        return editor.getText().substring(begin, end);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (keyMatches(e, k_AltT)) {
            testAudio();
        } else if (keyMatches(e, k_Escape)) {
            if (Playing.audio != null) {
                stopAudio();
            } else {
                close();
            }
        } else if (keyMatches(e, k_CtrlF)) find(false);
        else if (keyMatches(e, k_CtrlH)) find(true);
        //else if (keyMatches(e, k_AltM)) toggleMusicMode();
        else if (keyMatches(e, k_CtrlZ)) undo();
        else if (keyMatches(e, k_CtrlY)) redo();
        else if (keyMatches(e, k_CtrlD)) duplicateLine();
        else if (keyMatches(e, k_transposeDown)) transposeDown();
        else if (keyMatches(e, k_transposeUp)) transposeUp();
        else if (keyMatches(e, k_F3)) Util.charInsert(worldEditor, editor, form, form);
        else if (musicMode && e.getKeyCode() == KeyEvent.VK_ENTER) {
            e.consume(); // We will handle this in the keyTyped event handler
        }
        else return;
        e.consume();
    }

    private void redo() {
        if (undoPos >= undoList.size()) return; // Nothing to redo
        var undoItem = undoList.get(undoPos);
        int caret = -1;
        for (Undo undoStep : undoItem) {
            try {
                doOp(undoStep.insert, undoStep.pos, undoStep.text);
            } catch (BadLocationException e) {
                undoError();
                return;
            }
            caret = undoStep.caret;
        }
        lastText = editor.getText();

        if (caret != -1 && caret < lastText.length()) editor.setCaretPosition(caret);
        undoPos++;
    }

    private void undoError() {
        undoPos = 0;
        undoList.clear();
        lastText = editor.getText();
        JOptionPane.showMessageDialog(form, "Error performing undo/redo", "Undo/redo error", JOptionPane.ERROR_MESSAGE);
    }

    private void undo() {
        if (undoPos == 0) return; // Nothing to undo
        undoPos--;
        var undoItem = undoList.get(undoPos);
        int caret = -1;
        for (int i = undoItem.size() - 1; i >= 0; i--) {
            var undoStep = undoItem.get(i);
            try {
                doOp(!undoStep.insert, undoStep.pos, undoStep.text);
            } catch (BadLocationException e) {
                undoError();
                return;
            }
            caret = undoStep.caret;
        }
        lastText = editor.getText();
        if (caret != -1 && caret < lastText.length()) editor.setCaretPosition(caret);
    }

    private void doOp(boolean insert, int pos, String text) throws BadLocationException {
        recordUndo = false;
        if (insert) {
            editor.getDocument().insertString(pos, text, null);
        } else {
            editor.getDocument().remove(pos, text.length());
        }
        recordUndo = true;
    }

    private void close() {
        if (musicMode) {
            toggleMusicMode();
            return;
        }
        stopAudio();
        form.dispose();
    }

    private void testAudio() {
        if (Playing.audio != null) {
            stopAudio();
            return;
        }
        int saved_selectStart = editor.getSelectionStart();
        int saved_selectEnd = editor.getSelectionEnd();
        int selectionLength = saved_selectEnd - saved_selectStart;

        boolean breakOnNonMusic;
        int playMin, playMax;
        if (selectionLength == 0) {
            breakOnNonMusic = true;
            playMin = 0;
            playMax = Integer.MAX_VALUE;
        } else {
            breakOnNonMusic = false;
            playMin = saved_selectStart;
            playMax = saved_selectEnd;
        }

        int cursor = beginningOfLine(saved_selectStart);
        var as = getMusicLines(cursor, playMin, playMax, breakOnNonMusic);
        Playing.lineStarts = new ArrayList<>();
        for (var musicLine : as) {
            Playing.lineStarts.add(musicLine.linestart);
        }

        if (as.isEmpty()) return;

        Playing.audio = Audio.playSequence(as, new AudioCallback(){
            @Override
            public void upTo(int line, int pos, int len) {
                if (line == -1) {
                    Playing.audio = null;
                    //System.err.printf("editor.setCaretPosition(%d)\n", editor.getCaretPosition());
                    editor.setCaretPosition(saved_selectStart);
                    editor.moveCaretPosition(saved_selectEnd);
                    return;
                }
                //System.out.printf("upTo(%d, %d, %d)\n", line, pos, len);
                //cd.musicAt(Playing.lineStarts.get(line) + pos, len);
                int start = Playing.lineStarts.get(line) + pos;
                //System.err.printf("editor.select(%d, %d)\n", start, start + len);
                editor.setCaretPosition(start);
                editor.moveCaretPosition(start + len);
            }
        });
    }

    private ArrayList<MusicLine> getMusicLines(int cursor, int playMin, int playMax, boolean breakOnNonMusic) {
        var as = new ArrayList<MusicLine>();
        while (cursor != -1) {
            var s = getLine(cursor);
            int beginningOfLine = cursor;
            int endOfLine = beginningOfLine + s.length();
            int musical = isMusicalLine(s);
            if (musical == 1) {
                MusicLine m;
                int mstart = 0;
                int mend = s.length();
                if (playMin > beginningOfLine && playMin <= endOfLine) {
                    mstart = playMin - beginningOfLine;
                }
                if (playMax > beginningOfLine && playMax <= endOfLine) {
                    mend = playMax - beginningOfLine;
                }

                as.add(new MusicLine(s, mstart, mend, cursor));
            } else {
                if (breakOnNonMusic && musical == -1) break;
            }

            cursor = nextLine(cursor);
            if (cursor >= playMax) break;
        }

        return as;
    }

    private void stopAudio() {
        if (Playing.audio == null) return;
        Playing.audio.stop();
        Playing.audio = null;
    }

    private int isMusicalLine(String s) {
        s = s.replace("?i", "");
        s = s.replace("/i", "");
        s = s.toUpperCase().trim();
        if (s.isEmpty()) return 0;
        if (s.startsWith(":")) return 0;
        if (s.startsWith("#PLAY")) return 1;
        return -1;
    }

    private int nextLine(int pos) {
        String s = editor.getText();
        for (;;) {
            if (pos == s.length()) return -1;
            if (s.charAt(pos) == '\n') break;
            pos++;
        }
        if (pos + 1 >= s.length()) return -1;
        return pos + 1;
    }

    private int beginningOfLine(int pos) {
        String s = editor.getText();
        for (;;) {
            if (pos == 0) break;
            if (s.charAt(pos - 1) == '\n') break;
            pos--;
        }
        return pos;
    }

    private String getLine(int pos) {
        String s = editor.getText();
        int endPos = pos;
        for (;;) {
            if (endPos == s.length()) break;
            if (s.charAt(endPos) == '\n') break;
            endPos++;
        }
        return s.substring(pos, endPos);
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

    private JButton addButtonTo(JPanel rightPane, String text) {
        var buttonPane = new JPanel(new BorderLayout());
        var button = new JButton(text);
        buttonPane.add(button, BorderLayout.NORTH);
        buttonPane.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 8));
        rightPane.add(buttonPane);
        return button;
    }

    private JTextField addTextfieldTo(JPanel centrePane, String s) {
        var textPane = new JPanel(new BorderLayout());
        var textField = new JTextField(32);
        var lbl = new JLabel(s);
        lbl.setPreferredSize(new Dimension(60, 0));
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));

        textPane.add(lbl, BorderLayout.WEST);
        textPane.add(textField, BorderLayout.CENTER);

        var fullpanel = new JPanel(new BorderLayout());
        fullpanel.add(textPane, BorderLayout.NORTH);
        fullpanel.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 0));
        centrePane.add(fullpanel);

        return textField;
    }

    private void addCheckboxTo(JPanel centrePane, String s, String cfg) {
        var ge = worldEditor.getGlobalEditor();

        var chk = new JCheckBox(s, ge.getBoolean(cfg, false));
        chk.addItemListener(e -> ge.setBoolean(cfg, chk.isSelected()));
        chk.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        var fullpanel = new JPanel(new BorderLayout());
        fullpanel.add(chk, BorderLayout.NORTH);
        fullpanel.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 0));
        centrePane.add(fullpanel);
    }


    private void find(boolean replace) {
        if (findDialog != null) {
            System.out.println("Closing");
            findDialog.dispose();
        }
        selFromRep = false;
        var ge = worldEditor.getGlobalEditor();
        findDialog = new JDialog(form);
        Util.addEscClose(findDialog, findDialog.getRootPane());
        findDialog.setResizable(false);
        findDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        findDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                findDialog = null;
            }
        });
        findDialog.setTitle(replace ? "Replace Text" : "Find Text");
        findDialog.getContentPane().setLayout(new BorderLayout());
        findDialog.setIconImage(worldEditor.getCanvas().extractCharImage('?', 0x70, 1, 1, false, "$"));
        JButton findNextBtn = null, cancelBtn = null, replaceBtn = null, replaceAllBtn = null;

        JPanel centrePane = new JPanel(new GridLayout(0, 1));
        centrePane.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel rightPane = new JPanel(new GridLayout(0, 1));
        rightPane.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        findDialog.getContentPane().add(centrePane, BorderLayout.CENTER);
        findDialog.getContentPane().add(rightPane, BorderLayout.EAST);

        findNextBtn = addButtonTo(rightPane, "Find next");
        findDialog.getRootPane().setDefaultButton(findNextBtn);
        JTextField findTextField = addTextfieldTo(centrePane, "Find what:");
        findNextBtn.addActionListener(e -> findNext(findTextField.getText()));
        if (replace) {
            replaceBtn = addButtonTo(rightPane, "Replace");
            replaceAllBtn = addButtonTo(rightPane, "Replace All");
            JTextField replaceTextField = addTextfieldTo(centrePane, "Replace:");
            replaceBtn.addActionListener(e -> replace(findTextField.getText(), replaceTextField.getText(), false, selFromRep));
            replaceAllBtn.addActionListener(e -> replace(findTextField.getText(), replaceTextField.getText(), true, selFromRep));
        }
        cancelBtn = addButtonTo(rightPane, "Cancel");
        if (!replace) {
            rightPane.add(new JPanel());
        }
        addCheckboxTo(centrePane, "Match case", "FIND_MATCH_CASE");
        addCheckboxTo(centrePane, "Regexp", "FIND_REGEX");

        cancelBtn.addActionListener(e -> findDialog.dispose());

        findDialog.pack();
        findDialog.setLocationRelativeTo(form);
        findDialog.setVisible(true);
        findTextField.requestFocusInWindow();
    }

    private void replace(String text, String replaceWith, boolean all, boolean selectionFromReplace) {
        if (text.isEmpty()) return;
        var document = editor.getText();
        var caret = editor.getSelectionStart();
        if (selectionFromReplace) {
            caret = (caret + 1) % document.length();
        }
        var startCaret = caret;
        boolean wrapped = false;
        int replaces = 0;
        int lastStart = -1, lastEnd = -1;
        for (;;) {
            var res = generalSearch(document, caret, text);
            if (res == null) {
                if (replaces == 0) {
                    JOptionPane.showMessageDialog(findDialog, "The specified text was not found.", "Replace", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                break;
            } else {
                int start = res.get(0);
                int end = res.get(1);
                if (start < caret && !wrapped) {
                    wrapped = true;
                } else if (start >= startCaret && wrapped) {
                    break;
                }

                lastStart = start;
                lastEnd = start + replaceWith.length();
                caret = (start + 1) % document.length();

                document = document.substring(0, start) + replaceWith + document.substring(end);
                replaces++;
            }
            if (!all) break;
        }

        editor.setText(document);
        editor.setCaretPosition(lastStart);
        editor.moveCaretPosition(lastEnd);
        selFromRep = true;

        if (all) {
            JOptionPane.showMessageDialog(findDialog,
                    String.format("%d occurrence%s of the specified text have been replaced.", replaces, replaces == 1 ? "" : "s"),
                    "Replace", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void findNext(String text) {
        var document = editor.getText();
        var caret = editor.getCaretPosition();
        var res = generalSearch(document, caret, text);
        if (res == null) {
            JOptionPane.showMessageDialog(findDialog, "The specified text was not found.", "Find next", JOptionPane.WARNING_MESSAGE);
        } else {
            editor.setCaretPosition(res.get(0));
            editor.moveCaretPosition(res.get(1));
            selFromRep = false;
        }
    }

    private ArrayList<Integer> generalSearch(String document, int caret, String text) {
        boolean regex = worldEditor.getGlobalEditor().getBoolean("FIND_REGEX", false);
        boolean caseSensitive = worldEditor.getGlobalEditor().getBoolean("FIND_MATCH_CASE", false);

        if (!regex) {
            if (!caseSensitive) {
                document = document.toLowerCase();
                text = text.toLowerCase();
            }
            int r = document.indexOf(text, caret);
            if (r != -1) return pair(r, r + text.length());
            if (caret > 0) {
                r = document.indexOf(text, 0);
                if (r != -1) return pair(r, r + text.length());
            }
            return null;
        } else {
            int flags = Pattern.MULTILINE | Pattern.UNIX_LINES;
            if (!caseSensitive) {
                flags |= Pattern.CASE_INSENSITIVE;
            }
            var pattern = Pattern.compile(text, flags);
            var matcher = pattern.matcher(document);
            if (matcher.find(caret)) {
                return pair(matcher.start(), matcher.end());
            }
            if (matcher.find(0)) {
                return pair(matcher.start(), matcher.end());
            }
            return null;
        }
    }

    private void createMenu() {
        ArrayList<Menu> menus = new ArrayList<>();
        Menu m;
        m = new Menu("Menu");
        m.add("Test music", "Alt-T", e -> testAudio());
        m.add("Transpose music down", "Ctrl--", e -> transposeDown());
        m.add("Transpose music up", "Ctrl-=", e -> transposeUp());
        m.add();
        m.add("Duplicate line", "Ctrl-D", e -> duplicateLine());
        m.add("Insert character", "F3", e -> Util.charInsert(worldEditor, editor, form, form));
        m.add();
        m.add("Find", "Ctrl-F", e -> find(false));
        m.add("Replace", "Ctrl-H", e -> find(true));
        m.add();
        m.add("Undo", "Ctrl-Z", e -> undo());
        m.add("Redo", "Ctrl-Y", e -> redo());
        m.add();
        //m.add("Toggle Music Mode", "Alt-M", e -> toggleMusicMode());
        //m.add();
        m.add("Close", "Escape", e -> close());
        menus.add(m);

        JMenuBar menuBar = new JMenuBar();
        for (var menu : menus) {
            JMenu jmenu = new JMenu(menu.getTitle());
            for (var menuEntry : menu) {
                menuEntry.addToJMenu(worldEditor.getGlobalEditor(), jmenu);
            }
            menuBar.add(jmenu);
        }
        form.setJMenuBar(menuBar);
    }

    private void transposeUp() {
        try {
            transpose(1);
        } catch (BadLocationException ignored) { }
    }

    private void transposeDown() {
        try {
            transpose(-1);
        } catch (BadLocationException ignored) {
        }
    }

    private void transpose(int by) throws BadLocationException {
        int saved_selectStart = editor.getSelectionStart();
        int saved_selectEnd = editor.getSelectionEnd();

        int cursor = beginningOfLine(saved_selectStart);
        ArrayList<MusicLine> as;

        if (saved_selectStart == saved_selectEnd) {
            int startOfLine = beginningOfLine(saved_selectStart);
            int endOfLine  = saved_selectStart + getLine(saved_selectStart).length();
            as = getMusicLines(cursor, startOfLine, endOfLine, false);
        } else {
            as = getMusicLines(cursor, saved_selectStart, saved_selectEnd, false);
        }

        if (as.isEmpty()) return;
        int codeOffset = 0;
        var musicNoteLines = new ArrayList<ArrayList<MusicNote>>();
        boolean transposeFailed = false;
        for (var musicLine : as) {
            var musicNotes = MusicNote.fromPlay(musicLine.seq);
            if (musicNotes.isEmpty()) continue;

            // Need to transpose notes between musicLine.start and musicLine.end

            for (int i = 0; i < musicNotes.size(); i++) {
                if (musicNotes.get(i).indicate_pos >= musicLine.start &&
                        musicNotes.get(i).indicate_pos < musicLine.end) {
                    if (!musicNotes.get(i).transpose(by)) {
                        transposeFailed = true;
                    }
                }
            }
            // after transposing all the notes, we need to fix up the octaves
            for (int i = 0; i < musicNotes.size(); i++) {
                i = MusicNote.fixOctavesFor(i, musicNotes);
            }
            musicNoteLines.add(musicNotes);
        }
        if (transposeFailed) return;

        for (int line = 0; line < as.size(); line++) {
            var musicLine = as.get(line);
            var musicNotes = musicNoteLines.get(line);
            // Now, we need to replace the original #play line with this new one
            String preamble = musicLine.seq.substring(0, musicNotes.get(0).indicate_pos);
            StringBuilder newSeq = new StringBuilder(preamble);
            for (int i = 0; i < musicNotes.size(); i++) {
                newSeq.append(musicNotes.get(i).original);
            }

            editor.getDocument().remove(musicLine.linestart + codeOffset, musicLine.seq.length());
            editor.getDocument().insertString(musicLine.linestart + codeOffset, newSeq.toString(), null);
            int offsetChange = newSeq.toString().length() - musicLine.seq.length();
            codeOffset += offsetChange;
            if (saved_selectStart != saved_selectEnd) {
                saved_selectEnd += offsetChange;
            }
        }

        editor.setCaretPosition(saved_selectStart);
        editor.moveCaretPosition(saved_selectEnd);
    }

    private void toggleMusicMode() {
        musicMode = !musicMode;
        upd();
    }

    private void duplicateLine() {
        int caret = editor.getCaretPosition();
        int begin = beginningOfLine(caret);
        int end = nextLine(caret);
        var text = editor.getText();
        if (end == -1) end = text.length();
        try {
            var dupedLine = text.substring(begin, end);
            if (dupedLine.isEmpty()) return;
            if (dupedLine.charAt(dupedLine.length() - 1) != '\n') {
                dupedLine = dupedLine + "\n";
            }
            editor.getDocument().insertString(begin, dupedLine, null);
        } catch (BadLocationException e) {
            JOptionPane.showMessageDialog(form, "Error duplicating line", "Duplicate line", JOptionPane.ERROR_MESSAGE);
        }
    }
}
