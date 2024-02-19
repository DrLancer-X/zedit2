package zedit2;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

import static zedit2.Util.pair;

public class WorldEditor implements KeyActionReceiver, KeyListener, WindowFocusListener, PopupMenuListener, MenuListener {
    private GlobalEditor globalEditor;
    private File path = null;
    private WorldData worldData = null;
    int currentBoardIdx = 0;
    private Board currentBoard = null;
    private ArrayList<Board> boards = new ArrayList<>();
    private HashMap<Integer, ArrayList<Board>> undoList = new HashMap<>();
    private HashMap<Integer, Integer> undoPositions = new HashMap<>();
    public static final int BLINK_DELAY = 267;

    private double zoom;
    private int cursorX, cursorY;
    private boolean centreView;
    private int blockStartX = -1, blockStartY = -1;
    private int moveBlockW, moveBlockH;
    private int moveBlockX, moveBlockY;
    private int boardW, boardH;
    private HashMap<Integer, Atlas> atlases = new HashMap<>();
    private Atlas currentAtlas;
    private int gridW, gridH;
    private int[][] grid;
    private int width, height;
    private boolean drawing, textEntry;
    private int textEntryX;
    private boolean fancyFillDialog = false;

    private HashSet<ArrayList<Integer>> voidsDrawn = new HashSet<>();
    private boolean redraw;
    private int redraw_x1, redraw_x2, redraw_y1, redraw_y2;
    private int redraw_width, redraw_height;
    private HashSet<File> deleteOnClose = new HashSet<>();

    private DosCanvas canvas;
    private JPanel bufferPane;
    private JPanel bufferPaneContents;
    private JTextArea infoBox;
    private JFrame frame;
    private JScrollPane canvasScrollPane;
    private JMenuBar menuBar;
    private EditingModePane editingModePane;

    private static final int PUT_DEFAULT = 1;
    private static final int PUT_PUSH_DOWN = 2;
    private static final int PUT_REPLACE_BOTH = 3;

    public static final int SHOW_NOTHING = 0;
    public static final int SHOW_STATS = 1;
    public static final int SHOW_OBJECTS = 2;
    public static final int SHOW_INVISIBLES = 3;
    public static final int SHOW_EMPTIES = 4;
    public static final int SHOW_EMPTEXTS = 5;
    public static final int SHOW_FAKES = 6;
    private int currentlyShowing = SHOW_NOTHING;

    private ArrayList<BlinkingImageIcon> blinkingImageIcons = new ArrayList<>();

    private int mouseScreenX = 0, mouseScreenY = 0;
    private int mousePosX = 0, mousePosY = 0;
    private int mouseX, mouseY;
    private int oldMousePosX = -1, oldMousePosY = -1;
    private HashMap<Integer, JMenu> fmenus = new HashMap<>();
    private JMenu recentFilesMenu;

    private ArrayList<Thread> testThreads = new ArrayList<>();

    private BufferManager currentBufferManager = null;
    private int mouseState = 0;
    private boolean undoDirty = false;

    private boolean popupOpen = false;
    private boolean validGracePeriod = false;

    public WorldEditor(GlobalEditor globalEditor, boolean szzt) throws IOException, WorldCorruptedException {
        this(globalEditor, null, szzt ? SZZTWorldData.createWorld() : ZZTWorldData.createWorld());
    }

    public WorldEditor(GlobalEditor globalEditor, File path) throws IOException, WorldCorruptedException {
        this(globalEditor, path, WorldData.loadWorld(path));
    }

    public WorldEditor(GlobalEditor globalEditor, File path, WorldData worldData) throws IOException, WorldCorruptedException {
        this.globalEditor = globalEditor;
        this.globalEditor.editorOpened();
        this.zoom = globalEditor.getDouble("ZOOM");
        createGUI();
        loadWorld(path, worldData);
        afterUpdate();
    }

    private void openWorld(File path) throws IOException, WorldCorruptedException
    {
        if (oneWorldAtATime()) {
            if (promptOnClose()) {
                loadWorld(path, WorldData.loadWorld(path));
                invalidateCache();
                afterModification();
            }
        } else {
            new WorldEditor(globalEditor, path);
        }
    }

    private void newWorld(boolean szzt) throws IOException {
        try {
            if (oneWorldAtATime()) {
                if (promptOnClose()) {
                    var newWorld = szzt ? SZZTWorldData.createWorld() : ZZTWorldData.createWorld();
                    loadWorld(null, newWorld);
                    invalidateCache();
                    afterModification();
                }
            } else {
                new WorldEditor(globalEditor, szzt);
            }
        } catch (WorldCorruptedException e) {
            throw new RuntimeException("This should not happen.");
        }
    }


    public WorldData getWorldData() {
        return worldData;
    }
    public ArrayList<Board> getBoards() {
        return boards;
    }
    public DosCanvas getCanvas() {
        return canvas;
    }

    private void loadWorld(File path, WorldData worldData) throws WorldCorruptedException {
        if (path != null) {
            globalEditor.setDefaultDirectory(path);
        }
        this.path = path;
        globalEditor.addToRecent(path);
        this.worldData = worldData;
        boards.clear();
        atlases.clear();
        try {
            canvas.setCP(null, null);
        } catch (IOException ignored) { }

        for (int i = 0; i <= worldData.getNumBoards(); i++) {
            boards.add(worldData.getBoard(i));
        }

        var cb = worldData.getCurrentBoard();
        boardW = boards.get(0).getWidth();
        boardH = boards.get(0).getHeight();
        cursorX = boards.get(cb).getStat(0).getX() - 1;
        cursorY = boards.get(cb).getStat(0).getY() - 1;
        centreView = true;

        updateMenu();
        changeBoard(cb);
    }
    public void changeToIndividualBoard(int newBoardIdx) {
        setCurrentBoard(newBoardIdx);
        atlases.remove(newBoardIdx);
        currentAtlas = null;
        gridW = 1;
        gridH = 1;
        cursorX %= boardW;
        cursorY %= boardH;
        canvas.setCursor(cursorX, cursorY);
        width = boardW * gridW;
        height = boardH * gridH;
        grid = new int[1][1];
        grid[0][0] = newBoardIdx;
        invalidateCache();
        afterModification();
        canvas.revalidate();
    }
    private void setCurrentBoard(int newBoardIdx) {
        currentBoardIdx = newBoardIdx;
        if (currentBoardIdx == -1) {
            currentBoard = null;
        } else {
            currentBoard = boards.get(currentBoardIdx);
        }
    }
    public void changeBoard(int newBoardIdx) {
        // Search the atlas for this board
        var atlas = atlases.get(newBoardIdx);
        if (atlas != null) {
            var gridPos = searchInAtlas(atlas, newBoardIdx);
            int x = gridPos.get(0);
            int y = gridPos.get(1);
            cursorX = (cursorX % boardW) + x * boardW;
            cursorY = (cursorY % boardH) + y * boardH;
            canvas.setCursor(cursorX, cursorY);
            setCurrentBoard(newBoardIdx);
            if (atlas != currentAtlas) {
                gridW = atlas.getW();
                gridH = atlas.getH();
                grid = atlas.getGrid();
                width = boardW * gridW;
                height = boardH * gridH;

                currentAtlas = atlas;
                invalidateCache();
                afterModification();
                canvas.revalidate();
            }

            var rect = new Rectangle(canvas.getCharW(x * boardW),
                    canvas.getCharH(y * boardH),
                    canvas.getCharW(boardW),
                    canvas.getCharH(boardH));
            canvas.scrollRectToVisible(rect);

        } else {
            // No atlas, switch to individual board
            changeToIndividualBoard(newBoardIdx);
        }
    }

    private void resetUndoList() {
        undoList.clear();
        undoPositions.clear();
        for (var row : grid) {
            for (int boardIdx : row) {
                if (boardIdx != -1) {
                    var boardUndoList = new ArrayList<Board>();
                    boardUndoList.add(boards.get(boardIdx).clone());
                    undoList.put(boardIdx, boardUndoList);
                    undoPositions.put(boardIdx, 0);
                }
            }
        }
    }

    private void operationUndo() {
        undo(false);
    }

    private void operationRedo() {
        undo(true);
    }

    private void undo(boolean redo) {
        // Find most recent timestamp value in the undos
        long mostRecentTimestamp = redo ? Long.MAX_VALUE : Long.MIN_VALUE;

        ArrayList<Integer> boardsToUndo = new ArrayList<>();
        for (var row : grid) {
            for (int boardIdx : row) {
                if (boardIdx != -1) {
                    ArrayList<Board> undoBoards = undoList.get(boardIdx);
                    if (undoBoards == null) continue;
                    int undoPos = undoPositions.get(boardIdx);
                    int newUndoPos = undoPos + (redo ? 1 : -1);
                    if (newUndoPos < 0) continue;
                    if (newUndoPos >= undoBoards.size()) continue;
                    long undoTimestamp = undoBoards.get(newUndoPos).getDirtyTimestamp();
                    if ((!redo && undoTimestamp > mostRecentTimestamp) ||
                            (redo && undoTimestamp < mostRecentTimestamp)) {
                        mostRecentTimestamp = undoTimestamp;
                        boardsToUndo.clear();
                    }
                    if (undoTimestamp == mostRecentTimestamp) {
                        boardsToUndo.add(boardIdx);
                    }
                }
            }
        }

        if (boardsToUndo.isEmpty()) {
            String operationName = redo ? "Redo" : "Undo";
            editingModePane.display(Color.RED, 1500, "Can't " + operationName);
        } else {
            for (int boardIdx : boardsToUndo) {
                int undoPos = undoPositions.get(boardIdx) + (redo ? 1 : -1);
                undoPositions.put(boardIdx, undoPos);
                Board undoBoard = undoList.get(boardIdx).get(undoPos);
                boards.set(boardIdx, undoBoard.clone());
            }
            invalidateCache();
            afterModification();
        }
    }

    private void addUndo()
    {
        undoDirty = false;

        for (var row : grid) {
            for (int boardIdx : row) {
                if (boardIdx != -1) {
                    ArrayList<Board> undoBoards = undoList.get(boardIdx);
                    int undoPos = -1;
                    boolean addTo = true;
                    if (undoBoards == null) {
                        undoBoards = new ArrayList<Board>();
                        undoList.put(boardIdx, undoBoards);
                    } else {
                        undoPos = undoPositions.get(boardIdx);

                        Board undoBoard = undoBoards.get(undoPos);
                        if (boards.get(boardIdx).timestampEquals(undoBoard)) {
                            addTo = false;
                        }
                        //System.out.println("Current board timestamp: " + boards.get(boardIdx).getDirtyTimestamp());
                        //System.out.println("Undo board timestamp: " + undoBoard.getDirtyTimestamp());
                        //System.out.println("addTo: " + addTo);
                    }

                    if (addTo) {
                        // Seems this board was modified.
                        // First, cut off everything after the current undo position
                        while (undoBoards.size() > undoPos + 1) {
                            undoBoards.remove(undoPos + 1);
                        }
                        // Now add this board to the undo list
                        undoBoards.add(boards.get(boardIdx).clone());
                        // Too many?
                        int undoBufferSize = globalEditor.getInt("UNDO_BUFFER_SIZE", 100);
                        if (undoBoards.size() > undoBufferSize) {
                            undoBoards.remove(0);
                        }
                        // Update the undo position
                        undoPositions.put(boardIdx, undoBoards.size() - 1);
                        //System.out.println("New undo list length for board " + boardIdx + ": " + undoBoards.size());
                    }
                }
            }
        }
    }

    private ArrayList<Integer> searchInAtlas(Atlas atlas, int idx) {
        for (int y = 0; y < atlas.getH(); y++) {
            for (int x = 0; x < atlas.getW(); x++) {
                if (atlas.getGrid()[y][x] == idx) {
                    return pair(x, y);
                }
            }
        }
        return null;
    }

    private void invalidateCache() {
        voidsDrawn.clear();
        redraw_width = 0;
        redraw_height = 0;
    }

    private void addRedraw(int x1, int y1, int x2, int y2)
    {
        // Expand the range by 1 to handle lines
        x1--;
        y1--;
        x2++;
        y2++;

        if (!redraw) {
            redraw_x1 = x1;
            redraw_y1 = y1;
            redraw_x2 = x2;
            redraw_y2 = y2;
            redraw = true;
        } else {
            redraw_x1 = Math.min(redraw_x1, x1);
            redraw_y1 = Math.min(redraw_y1, y1);
            redraw_x2 = Math.max(redraw_x2, x2);
            redraw_y2 = Math.max(redraw_y2, y2);
        }
    }

    private void drawBoard() {
        if (width != redraw_width || height != redraw_height) {
            canvas.setDimensions(width, height);
            redraw_width = width;
            redraw_height = height;
            redraw_x1 = 0;
            redraw_y1 = 0;
            redraw_x2 = width - 1;
            redraw_y2 = height - 1;
            redraw = true;
        }
        canvas.setZoom(worldData.isSuperZZT() ? zoom * 2 : zoom, zoom);
        canvas.setAtlas(currentAtlas, boardW, boardH, globalEditor.getBoolean("ATLAS_GRID", true));
        if (redraw) {
            for (int y = 0; y < gridH; y++) {
                for (int x = 0; x < gridW; x++) {
                    int boardIdx = grid[y][x];
                    if (boardIdx != -1) {
                        int x1 = Math.max(x * boardW, redraw_x1);
                        int x2 = Math.min(x * boardW + (boardW - 1), redraw_x2);
                        int y1 = Math.max(y * boardH, redraw_y1);
                        int y2 = Math.min(y * boardH + (boardH - 1), redraw_y2);
                        if (x2 >= x1 && y2 >= y1) {
                            boards.get(boardIdx).drawToCanvas(canvas, x * boardW, y * boardH,
                                    x1 - x * boardW, y1 - y * boardH, x2 - x * boardW, y2 - y * boardH,
                                    currentlyShowing);
                        }
                    } else {
                        var voidCoord = pair(x, y);
                        if (!voidsDrawn.contains(voidCoord)) {
                            canvas.drawVoid(x * boardW, y * boardH, boardW, boardH);
                            voidsDrawn.add(voidCoord);
                        }

                    }
                }
            }
        }
        canvas.repaint();
        redraw = false;
    }

    private void createGUI() throws IOException {
        frame = new JFrame();
        disableAlt();

        /**/ //frame.setUndecorated(true);

        frame.addKeyListener(this);
        frame.addWindowFocusListener(this);

        //var ico = ImageIO.read(new File("zediticon.png"));
        var ico = ImageIO.read(new ByteArrayInputStream(Data.ZEDITICON_PNG));
        frame.setIconImage(ico);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                globalEditor.editorClosed();
            }
            public void windowClosing(WindowEvent e) {
                tryClose();
            }
        });

        menuBar = new JMenuBar();
        createMenu();
        frame.setJMenuBar(menuBar);
        { // Remove F10
            var im = menuBar.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            for (var k : im.allKeys()) {
                if (k.getKeyCode() == KeyEvent.VK_F10) {
                    im.remove(k);
                }
            }
        }
        canvas = new DosCanvas(this, zoom);
        canvas.setBlinkMode(globalEditor.getBoolean("BLINKING", true));
        addKeybinds(frame.getLayeredPane());

        //drawBoard();
        canvasScrollPane = new JScrollPane(canvas);
        canvasScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        canvasScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        JPanel controlPane = new JPanel(new BorderLayout());

        infoBox = new JTextArea(3, 20);
        infoBox.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        infoBox.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        //infoBox.setFont(CP437.getFont());
        infoBox.setEditable(false);
        infoBox.setFocusable(false);

        bufferPane = new JPanel(new BorderLayout());

        editingModePane = new EditingModePane();
        editingModePane.setOpaque(false);

        //infoBox.setPreferredSize(new Dimension(80, 40));
        var controlScrollPane = new JScrollPane(bufferPane);
        controlScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        controlPane.add(infoBox, BorderLayout.NORTH);
        controlPane.add(controlScrollPane, BorderLayout.CENTER);
        controlPane.add(editingModePane, BorderLayout.SOUTH);


        var splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, controlPane, canvasScrollPane);


        var timer = new java.util.Timer(true);
        timer.schedule(new TimerTask() {
            private boolean blinkState = false;
            @Override
            public void run() {
                //if (!globalEditor.getBoolean("BLINKING", true)) return;
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (!popupOpen) frame.requestFocusInWindow();
                        blinkState = !blinkState;
                        canvas.setBlink(blinkState);
                        for (var icon : blinkingImageIcons) {
                            icon.blink(blinkState);
                        }
                        bufferPane.repaint();
                    }
                });
            }
        }, BLINK_DELAY, BLINK_DELAY);

        frame.getContentPane().add(splitPane);
        frame.pack();
        frame.setLocationRelativeTo(null);
        /**/ //frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setVisible(true);
    }

    private void closeEditor() {
        frame.dispose();
    }

    private void tryClose() {
        if (promptOnClose()) {
            closeEditor();
        }
    }

    private boolean isDirty() {
        if (worldData.isDirty()) return true;
        for (Board board : boards) {
            if (board.isDirty()) {
                return true;
            }
        }
        return false;
    }

    private boolean promptOnClose() {
        if (isDirty()) {
            int result = JOptionPane.showConfirmDialog(frame, "Save changes before closing?",
                    "Close world", JOptionPane.YES_NO_CANCEL_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                return menuSaveAs();
            }
            if (result != JOptionPane.NO_OPTION) {
                return false;
            }
        } else {
            if (globalEditor.getBoolean("PROMPT_ON_CLOSE", true) == false) return true;
            int result = JOptionPane.showConfirmDialog(frame, "Close world?",
                    "Close world", JOptionPane.YES_NO_OPTION);
            return result == JOptionPane.YES_OPTION;
        }
        return true;
    }

    private boolean saveTo(File path) {
        var worldCopy = worldData.clone();
        if (saveGame(path, worldCopy)) {
            for (var board : boards) {
                board.clearDirty();
            }
            worldCopy.setDirty(false);
            worldData = worldCopy;
            return true;
        }
        return false;
    }

    private boolean saveGame(File path, WorldData worldCopy) {
        var warning = new CompatWarning(worldData.isSuperZZT());

        // Update world name, if necessary
        String oldWorldName = new String(worldCopy.getName());
        String oldFileName = this.path == null ? "" : Util.getExtensionless(this.path.toString());
        if (oldWorldName.length() == 0 || oldWorldName.equalsIgnoreCase(oldFileName)) {
            String newWorldName =  Util.getExtensionless(path.toString()).toUpperCase();
            worldCopy.setName(newWorldName.getBytes());
        }

        if (boards.size() > 101 && !worldData.isSuperZZT()) {
            warning.warn(1, "World has >101 boards, which may cause problems in vanilla ZZT.");
        } else if (boards.size() > 33 && worldData.isSuperZZT()) {
            warning.warn(1, "World has >33 boards, which may cause problems in vanilla Super ZZT.");
        }

        for (int boardIdx = 0; boardIdx < boards.size(); boardIdx++) {
            var board = boards.get(boardIdx);
            warning.setPrefix(String.format("Board %d (%s) ",  boardIdx, CP437.toUnicode(board.getName())));
            if (board.isDirty()) {
                worldCopy.setBoard(warning, boardIdx, board);
            }
        }
        worldCopy.terminateWorld(boards.size());
        warning.setPrefix("");
        if (worldCopy.getSize() > 450*1024) {
            warning.warn(1, "World is over 450kb, which may cause memory problems in ZZT.");
        }

        if (warning.getWarningLevel() == 2) {
            JOptionPane.showMessageDialog(frame, warning.getMessages(2),
                    "Compatibility error", JOptionPane.ERROR_MESSAGE);
            return false;
        } else if (warning.getWarningLevel() == 1) {
            int result = JOptionPane.showConfirmDialog(frame, warning.getMessages(1),
                    "Compatibility warning",  JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return false;
        }

        try {
            worldCopy.write(path);
        } catch (IOException o) {
            JOptionPane.showMessageDialog(frame, "Failed to write to file",
                    "File error", JOptionPane.ERROR_MESSAGE);
        }

        return true;
    }

    private JFileChooser getFileChooser(String[] exts, String desc) {
        var fileChooser = new JFileChooser();
        var filter = new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) return true;
                String name = f.getName();
                int extPos = name.lastIndexOf('.');
                if (extPos != -1) {
                    String ext = name.substring(extPos + 1);
                    for (var validExt : exts) {
                        if (ext.equalsIgnoreCase(validExt)) return true;
                    }
                }
                return false;
            }

            @Override
            public String getDescription() {
                return desc;
            }
        };
        fileChooser.setFileFilter(filter);
        fileChooser.setCurrentDirectory(globalEditor.getDefaultDirectory());
        //fileChooser.setCurrentDirectory(new File("C:\\Users\\" + System.getProperty("user.name") + "\\Dropbox\\YHWH\\zzt\\zzt\\"));
        return fileChooser;
    }

    private boolean menuSave() {
        if (path != null) {
            boolean r = saveTo(path);
            if (r) editingModePane.display(Color.ORANGE, 1500, "Saved World");
            afterUpdate();
            return r;
        }
        else return menuSaveAs();
    }

    private boolean menuSaveAs() {
        var fileChooser = getFileChooser(new String[]{"zzt", "szt", "sav"}, "ZZT/Super ZZT world and save files");
        if (path != null) fileChooser.setSelectedFile(path);

        int result = fileChooser.showSaveDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (saveTo(file)) {
                updateDefaultDir(file);
                path = file;
                globalEditor.setDefaultDirectory(path);
                globalEditor.addToRecent(path);
                updateMenu();
                afterUpdate();
                editingModePane.display(Color.ORANGE, 1500, "Saved World");
                return true;
            }
        }
        return false;
    }

    private void menuOpenWorld() {
        var fileChooser = getFileChooser(new String[]{"zzt", "szt", "sav"}, "ZZT/Super ZZT world and save files");

        int result = fileChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                updateDefaultDir(file);
                openWorld(file);
            } catch (IOException | WorldCorruptedException e) {
                JOptionPane.showMessageDialog(frame, e, "Error loading world", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void menuImportWorld() {
        var fileChooser = getFileChooser(new String[]{"zzt", "szt", "sav"}, "ZZT/Super ZZT world and save files");

        int result = fileChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                updateDefaultDir(file);
                var worldData = WorldData.loadWorld(file);
                if (worldData.isSuperZZT() != this.worldData.isSuperZZT()) {
                    throw new RuntimeException("Error: ZZT / Super ZZT mismatch");
                }
                for (int i = 0; i <= worldData.getNumBoards(); i++) {
                    var board = worldData.getBoard(i);
                    board.setDirty();
                    boards.add(board);
                }
            } catch (IOException | RuntimeException | WorldCorruptedException e) {
                JOptionPane.showMessageDialog(frame, e, "Error importing world", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void menuLoadCharset() {
        var fileChooser = getFileChooser(new String[]{"chr", "com"}, "Character set");
        int result = fileChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                canvas.setCharset(file);
                updateDefaultDir(file);
            } catch (IOException | RuntimeException e) {
                JOptionPane.showMessageDialog(frame, e, "Error loading char set", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    private void menuLoadPalette() {
        var fileChooser = getFileChooser(new String[]{"pal"}, "Palette");
        int result = fileChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                canvas.setPalette(file);
                updateDefaultDir(file);
            } catch (IOException | RuntimeException e) {
                JOptionPane.showMessageDialog(frame, e, "Error loading palette", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    private void menuDefaultCharsetPalette() {
        try {
            canvas.setCharset(null);
            canvas.setPalette(null);
        } catch (IOException ignored) {}
    }

    private void updateDefaultDir(File file) {
        if (file != null) {
            globalEditor.setDefaultDirectory(file);
        }
    }

    private void menuBoardList() {
        new BoardManager(this, boards);
    }

    private void menuNewWorld(boolean szzt) {
        try {
            newWorld(szzt);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, e, "Error creating new world", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void menuExportBoard() {
        if (currentBoard == null) return;
        var fileChooser = getFileChooser(new String[]{"brd"}, "ZZT/Super ZZT .BRD files");
        int result = fileChooser.showSaveDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                updateDefaultDir(file);
                currentBoard.saveTo(file);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(frame, e, "Error exporting board", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void menuImportBoard() {
        var fileChooser = getFileChooser(new String[]{"brd"}, "ZZT/Super ZZT .BRD files");
        int result = fileChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                currentBoard.loadFrom(file);
                updateDefaultDir(file);
                changeBoard(currentBoardIdx);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(frame, e, "Error loading board", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    private void menuImportBoards() {
        var fileChooser = getFileChooser(new String[]{"brd"}, "ZZT/Super ZZT .BRD files (Files that begin with numbers will be loaded to that index)");
        fileChooser.setMultiSelectionEnabled(true);
        int result = fileChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] files = fileChooser.getSelectedFiles();

            ArrayList<File> boardsToLoad = new ArrayList<>();
            for (Board board : boards) {
                boardsToLoad.add(null);
            }

            for (File file : files) {
                int boardNum = -1;
                try {
                    boardNum = Integer.parseInt(file.getName().split("[^0-9]")[0]);
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException ignore) {
                }
                if (boardNum < 0 || boardNum > 255) {
                    // Load this into a null slot that's >= the current board count
                    boolean placed = false;
                    for (int idx = boards.size(); idx < boardsToLoad.size(); idx++) {
                        if (boardsToLoad.get(idx) == null) {
                            boardsToLoad.set(idx, file);
                            placed = true;
                            break;
                        }
                    }
                    if (!placed && boardsToLoad.size() < 256)
                        boardsToLoad.add(file);
                } else {
                    while (boardNum >= boardsToLoad.size()) {
                        if (boardsToLoad.size() < 255) {
                            boardsToLoad.add(null);
                        }
                    }
                    boardsToLoad.set(boardNum, file);
                }
            }

            for (int idx = 0; idx < boardsToLoad.size(); idx++) {
                File file = boardsToLoad.get(idx);
                if (idx >= boards.size()) {
                    // Insert blank board here
                    boards.add(blankBoard("-untitled"));
                }
                if (file != null) {
                    try {
                        boards.get(idx).loadFrom(file);
                        if (idx == currentBoardIdx) {
                            changeBoard(currentBoardIdx);
                        }
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(frame, e, "Error loading board", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }
    }

    private void operationBoardExits() {
        Settings.boardExits(frame, currentBoard, boards, currentBoardIdx);
    }

    private boolean handleCheckbox(String key, Boolean setting) {
        if (setting == null) {
            if (globalEditor.isKey(key)) {
                return globalEditor.getBoolean(key);
            } else {
                return false;
            }
        } else {
            globalEditor.setBoolean(key, setting);
            return setting;
        }
    }

    private boolean oneWorldAtATime() {
        final String key = "ONE_WORLD_AT_A_TIME";
        return globalEditor.isKey(key) && globalEditor.getBoolean(key);
    }

    public void createMenu() {
        menuBar.removeAll();
        // | File     | World    |          |          |          |          |          |
        // | New ZZT  | World Set|          |          |          |          |          |
        // | New Super| Board Set|          |          |          |          |          |
        // | Open worl|          |          |          |          |          |          |
        // | Save     |          |          |          |          |          |          |
        // | Save As  |          |          |          |          |          |          |
        ArrayList<Menu> menus = new ArrayList<>();
        {
            Menu m;
            m = new Menu("File");
            m.add("New ZZT world", null, e -> menuNewWorld(false));
            m.add("New Super ZZT world", null, e -> menuNewWorld(true));
            m.add("Open world", "L", e -> menuOpenWorld());
            recentFilesMenu = new JMenu("Recent files");
            m.add(recentFilesMenu, "");

            m.add();
            m.add("One world at a time", e -> handleCheckbox("ONE_WORLD_AT_A_TIME", ((JCheckBoxMenuItem) e.getSource()).isSelected()), handleCheckbox("ONE_WORLD_AT_A_TIME", null));
            m.add();
            m.add("Save", "Ctrl-S", e -> menuSave());
            m.add("Save as", "S", e -> menuSaveAs());
            m.add();
            m.add("Zedit2 settings...", null, e -> {
                new Settings(this);
            });
            m.add();
            m.add("Close world", "Escape", e -> {
                if (promptOnClose()) closeEditor();
            });
            menus.add(m);

            m = new Menu("World");
            m.add("World settings...", "G", e -> Settings.world(frame, boards, worldData, canvas));
            m.add();
            m.add("Import world", null, e -> menuImportWorld());
            m.add();
            m.add("Load charset", null, e -> menuLoadCharset());
            m.add("Load palette", null, e -> menuLoadPalette());
            m.add("Default charset and palette", null, e -> menuDefaultCharsetPalette());
            m.add();
            m.add("Blinking", e -> {
                handleCheckbox("BLINKING", ((JCheckBoxMenuItem) e.getSource()).isSelected());
                afterBlinkToggle();
            }, handleCheckbox("BLINKING", null));
            m.add();
            m.add("Test world", "Alt-T", e -> operationTestWorld());
            m.add();
            m.add("Atlas", "Ctrl-A", e -> atlas());
            menus.add(m);

            m = new Menu("Board");
            m.add("Board settings...", "I", e -> Settings.board(frame, currentBoard, worldData));
            m.add("Board exits", "X", e -> operationBoardExits());
            m.add();
            m.add("Switch board", "B", e -> operationChangeBoard());
            m.add("Add board", "A", e -> operationAddBoard());
            m.add("Add boards (*x* grid)", null, e -> operationAddBoardGrid());
            m.add("Board list", "Shift-B", e -> menuBoardList());
            m.add();
            m.add("Import board", "Alt-I", e -> menuImportBoard());
            m.add("Import boards", null, e -> menuImportBoards());
            m.add("Export board", "Alt-X", e -> menuExportBoard());
            m.add();
            m.add("Remove from atlas", "Ctrl-R", e -> atlasRemoveBoard());
            m.add();
            m.add("Stats list", "Alt-S", e -> operationStatList());
            menus.add(m);

            m = new Menu("Edit");
            m.add("Undo", "Ctrl-Z", e -> operationUndo());
            m.add("Redo", "Ctrl-Y", e -> operationRedo());
            m.add();
            m.add("Select colour", "C", e -> operationColour());
            m.add("Modify buffer tile", "P", e -> operationModifyBuffer(false));
            m.add("Modify buffer tile (advanced)", "Ctrl-P", e -> operationModifyBuffer(true));
            m.add("Modify tile under cursor", "Alt-M", e -> operationGrabAndModify(false, false));
            m.add("Modify tile under cursor (advanced)", "Ctrl-Alt-M", e -> operationGrabAndModify(false, true));
            m.add("Exchange buffer fg/bg colours", "Ctrl-X", e -> operationBufferSwapColour());
            m.add();
            m.add("Start block operation", "Alt-B", e -> operationBlockStart());
            m.add();
            m.add("Enter text", "F2", e -> operationToggleText());
            m.add("Toggle drawing", "Tab", e -> operationToggleDrawing());
            m.add("Flood fill", "F", e -> operationFloodfill(cursorX, cursorY, false));
            m.add("Gradient fill", "Alt-F", e -> operationFloodfill(cursorX, cursorY, true));
            m.add();
            m.add("Convert image", null, e -> operationLoadImage());
            m.add("Convert image from clipboard", "Ctrl-V", e -> operationPasteImage());
            m.add();
            m.add("Erase player from board", "Ctrl-E", e -> operationErasePlayer());
            m.add();
            m.add("Buffer manager", "Ctrl-B", e -> operationOpenBufferManager());
            m.add();
            for (int f = 3; f <= 10; f++) {
                var fMenuName = getFMenuName(f);
                if (fMenuName != null && !fMenuName.equals("")) {
                    var elementMenu = new JMenu(fMenuName);
                    fmenus.put(f, elementMenu);
                    m.add(elementMenu, "F" + f);
                }
            }
            menus.add(m);

            m = new Menu("View");
            m.add("Zoom in", "Ctrl-=", e -> operationZoomIn(false));
            m.add("Zoom out", "Ctrl--", e -> operationZoomOut(false));
            m.add("Reset zoom", null, e -> operationResetZoom(false));
            m.add();
            m.add("Show grid in atlas", e ->
                    {
                        handleCheckbox("ATLAS_GRID", ((JCheckBoxMenuItem) e.getSource()).isSelected());
                        afterModification();
                    },
                    handleCheckbox("ATLAS_GRID", null));
            m.add();
            m.add("Show stats", "Shift-F1", e -> operationShowTileTypes(SHOW_STATS));
            m.add("Show objects", "Shift-F2", e -> operationShowTileTypes(SHOW_OBJECTS));
            m.add("Show invisibles", "Shift-F3", e -> operationShowTileTypes(SHOW_INVISIBLES));
            m.add("Show empties", "Shift-F4", e -> operationShowTileTypes(SHOW_EMPTIES));
            m.add("Show fakes", "Shift-F5", e -> operationShowTileTypes(SHOW_FAKES));
            m.add("Show empties as text", "Shift-F6", e -> operationShowTileTypes(SHOW_EMPTEXTS));
            m.add("Show nothing", null, e -> operationShowTileTypes(SHOW_NOTHING));
            m.add();
            m.add("Take screenshot", "F12", e -> takeScreenshot());
            m.add("Take screenshot to clipboard", "Alt-F12", e -> takeScreenshotToClipboard());
            menus.add(m);

            m = new Menu("Help");
            m.add("Help", "F1", e -> menuHelp());
            m.add("About", null, e -> menuAbout());
            menus.add(m);
        }

        /*
        ActionListener listener = e -> {
            var menuItem = (JMenuItem)e.getSource();
            menuSelection(menuItem.getText());
        };
        ChangeListener checkboxListener = e -> {
            var menuItem = (JCheckBoxMenuItem)e.getSource();
            menuCheckbox(menuItem.getText(), menuItem.isSelected());
        };
         */

        for (var m : menus) {
            var menu = new JMenu(m.getTitle());
            menu.addMenuListener(this);
            for (var mEntry : m) {
                mEntry.addToJMenu(globalEditor, menu);
            }

            menuBar.add(menu);
        }
        menuBar.revalidate();
        /*
                var mi = menuList[i];
                if (mi.equals("|")) {
                    menu.addSeparator();
                } else if (mi.equals("__ELEMENT_CATEGORIES__")) {
                    // F3 to F10
                    for (int f = 3; f <= 10; f++) {
                        var fMenuName = getFMenuName(f);
                        if (fMenuName != null) {
                            var elementMenu = new JMenu(fMenuName);
                            fmenus.put(f, elementMenu);
                            menu.add(elementMenu);
                        }
                    }

                } else if (mi.startsWith("[ ]")) {
                    var menuItemName = mi.substring(3);
                    var menuItem = new JCheckBoxMenuItem(menuItemName);
                    menuItem.setSelected(menuCheckbox(menuItemName, null));
                    menuItem.addChangeListener(checkboxListener);
                    menu.add(menuItem);
                    //mi.substring(3);
                } else {
                    var menuItem = new JMenuItem(menuList[i]);
                    menuItem.addActionListener(listener);
                    menu.add(menuItem);
                }
            }
            menuBar.add(menu);
        }
         */
    }

    private void afterBlinkToggle() {
        canvas.setBlinkMode(globalEditor.getBoolean("BLINKING", true));
        invalidateCache();
    }

    private void menuHelp() {
        new Help(this);
    }

    private void menuAbout() {
        new About(this);
    }

    public void updateMenu()
    {
        for (var f : fmenus.keySet()) {
            var fMenuItems = getFMenuItems(f);
            fmenus.get(f).removeAll();
            for (var fMenuItem : fMenuItems) {
                fmenus.get(f).add(fMenuItem);
            }
        }

        recentFilesMenu.removeAll();
        int recentMax = globalEditor.getInt("RECENT_MAX", 10);
        for (int i = 9; i >= 0; i--) {
            var recentFileName = globalEditor.getString(String.format("RECENT_%d", i));
            if (recentFileName != null) {
                var recentFile = new File(Util.evalConfigDir(recentFileName));
                var menuEntry = recentFile.getName();
                var menuItem = new JMenuItem(menuEntry);
                menuItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            openWorld(recentFile);
                            updateDefaultDir(recentFile);
                        } catch (IOException | WorldCorruptedException ex) {
                            globalEditor.removeRecentFile(recentFileName);
                            recentFilesMenu.remove(menuItem);
                            if (recentFilesMenu.getMenuComponentCount() == 0) recentFilesMenu.setEnabled(false);
                            JOptionPane.showMessageDialog(frame, ex, "Error loading world", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                });
                recentFilesMenu.add(menuItem);
            }
        }
        recentFilesMenu.setEnabled(recentFilesMenu.getMenuComponentCount() != 0);
    }

    private JMenuItem[] getFMenuItems(int f) {
        boolean szzt = worldData.isSuperZZT();
        ArrayList<JMenuItem> menuItems = new ArrayList<>();
        for (int i = 0;; i++) {
            var prop = String.format("F%d_MENU_%d", f, i);
            var element = globalEditor.getString(prop);
            if (element == null || element.equals("")) {
                break;
            } else {
                String elementName, displayName;
                boolean starred = false;
                int parenPos = element.indexOf('(');
                if (parenPos == -1) {
                    int exclPos = element.indexOf('!');
                    if (exclPos != -1) {
                        elementName = element.substring(0, exclPos);
                        displayName = element.substring(exclPos + 1);
                    } else {
                        elementName = element;
                        displayName = elementName;
                    }
                } else {
                    elementName = element.substring(0, parenPos);
                    int endParenPos = element.lastIndexOf(')');
                    if (endParenPos == -1) throw new RuntimeException("Malformed element: " + element);
                    if (endParenPos == element.length() - 1) {
                        displayName = elementName;
                    } else {
                        int starPos = element.lastIndexOf('*');
                        if (starPos == -1 || starPos < endParenPos) {
                            displayName = element.substring(endParenPos + 1);
                        } else {
                            starred = true;
                            displayName = element.substring(endParenPos + 1, starPos);
                        }
                    }
                }
                final boolean editOnPlace = !starred;
                if (ZType.getId(szzt, elementName) == -1) continue;
                Tile tile = getTileFromElement(element, 0xF0);
                int chr = ZType.getChar(szzt, tile);
                //byte[] glyph = new byte[1];
                //glyph[0] = (byte) chr;
                //var menuItem = new JMenuItem(CP437.toUnicode(glyph) + " " + displayName);
                var menuItem = new JMenuItem(displayName);
                //menuItem.setFont(CP437.getFont());
                menuItem.addActionListener(e -> setBufferToElement(element, editOnPlace));

                int col = ZType.getColour(szzt, tile);
                var img = canvas.extractCharImageWH(chr, col, szzt ? 2 : 1, 1, false, "____$____", 3, 3);
                int side = 20;
                var img2 = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
                var g = img2.getGraphics();
                g.drawImage(img, (side - img.getWidth()) / 2, (side - img.getHeight()) / 2, null);
                ImageIcon icon = new ImageIcon(img2);
                menuItem.setIcon(icon);
                menuItems.add(menuItem);
            }
        }
        return menuItems.toArray(new JMenuItem[0]);
    }

    private Tile getTileFromElement(String element, int col) {
        boolean vanilla = false;
        int parenPos = element.indexOf('(');
        if (parenPos == -1) {
            parenPos = element.length();
            vanilla = true;
        }
        var elementName = element.substring(0, parenPos);
        int exclPos = elementName.indexOf('!');
        if (exclPos != -1) {
            elementName = element.substring(0, exclPos);
        }
        var elementId = ZType.getId(worldData.isSuperZZT(), elementName);
        if (elementId == -1) {
            throw new RuntimeException(String.format("\"%s\" is not a valid %s element", elementName, worldData.isSuperZZT() ? "SuperZZT" : "ZZT"));
        }
        Tile tile = new Tile(elementId, 0);
        if (!ZType.isText(worldData.isSuperZZT(), tile.getId())) {
            paintTile(tile, col);
        }
        if (!vanilla) {
            int lastParenPos = element.lastIndexOf(')');
            if (lastParenPos == -1) throw new RuntimeException("Malformed element: " + element);
            String elementStatInfo = element.substring(parenPos + 1, lastParenPos);
            // | , =, ", \n are escaped. Convert the escaped forms to Unicode PUA U+E00{0,1,2,3,4} respectively so they don't interfere with splitting
            elementStatInfo = elementStatInfo.replace("\\|", "\uE000");
            elementStatInfo = elementStatInfo.replace("\\,", "\uE001");
            elementStatInfo = elementStatInfo.replace("\\=", "\uE002");
            elementStatInfo = elementStatInfo.replace("\\\"", "\uE003");
            elementStatInfo = elementStatInfo.replace("\\n", "\uE004");

            // Split into stats
            var statList = elementStatInfo.split(Pattern.quote("|"));
            var stats = new ArrayList<Stat>();
            for (var statString : statList) {
                Stat stat = new Stat(worldData.isSuperZZT());
                // Split into params
                var paramList = statString.split(",");
                for (var paramString : paramList) {
                    if (paramString.isEmpty()) continue;
                    // Split into key=value
                    var kvPair = paramString.split("=");
                    if (kvPair.length != 2) throw new RuntimeException("Invalid key=value pair in " + paramString);
                    var param = kvPair[0].toUpperCase();
                    var value = kvPair[1];
                    switch (param) {
                        case "STATID":
                            stat.setStatId(Integer.parseInt(value));
                            break;
                        case "CYCLE":
                            stat.setCycle(Integer.parseInt(value));
                            break;
                        case "P1":
                            stat.setP1(Integer.parseInt(value));
                            break;
                        case "P2":
                            stat.setP2(Integer.parseInt(value));
                            break;
                        case "P3":
                            stat.setP3(Integer.parseInt(value));
                            break;
                        case "UID":
                            stat.setUid(Integer.parseInt(value));
                            break;
                        case "UCO":
                            stat.setUco(Integer.parseInt(value));
                            break;
                        case "IP":
                            stat.setIp(Integer.parseInt(value));
                            break;
                        case "STEPX":
                            stat.setStepX(Integer.parseInt(value));
                            break;
                        case "STEPY":
                            stat.setStepY(Integer.parseInt(value));
                            break;
                        case "POINTER":
                            stat.setPointer(Integer.parseInt(value));
                            break;
                        case "AUTOBIND":
                            stat.setAutobind(Boolean.parseBoolean(value));
                            break;
                        case "SPECIFYID":
                            stat.setSpecifyId(Boolean.parseBoolean(value));
                            break;
                        case "ISPLAYER":
                            stat.setIsPlayer(Boolean.parseBoolean(value));
                            break;
                        case "CODE":
                            String code = value;
                            code = code.replace("\uE000", "|");
                            code = code.replace("\uE001", ",");
                            code = code.replace("\uE002", "=");
                            code = code.replace("\uE003", "\"");
                            code = code.replace("\uE004", "\n");
                            stat.setCode(CP437.toBytes(code, false));
                            break;
                        default:
                            throw new RuntimeException("Stat property not supported: " + param);
                    }
                }
                stats.add(stat);
            }
            tile.setStats(stats);
        }
        return tile;
    }

    private Tile getBufferTile() {
        return globalEditor.getBufferTile(worldData.isSuperZZT());
    }

    private boolean isValidTile(Tile tile) {
        if (validGracePeriod) return true;
        String configKey = "DONT_WARN_" + tile.getId() + (worldData.isSuperZZT() ? "_SZZT" : "");
        if (globalEditor.getBoolean(configKey, false)) return true;
        if (ZType.isCrashy(worldData.isSuperZZT(), tile)) {
            int r = JOptionPane.showOptionDialog(frame,
                    "This element type (" + ZType.getName(worldData.isSuperZZT(), tile.getId()) + ") is known to cause stability problems in ZZT.\nIt will most likely cause ZZT to crash if touched by the player, and may cause problems even if not touched.",
                    "Unstable type selected",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    new String[]{"Okay", "Use it anyway", "Use it anyway (don't warn me again)"},
                    null);
            if (r == JOptionPane.CLOSED_OPTION) return false;
            if (r == 0) return false;
            if (r == 2) globalEditor.setBoolean(configKey, true);
            validGracePeriod = true;
        }
        return true;
    }

    private void setBufferTile(Tile tile) {
        if (!isValidTile(tile)) return;
        globalEditor.setBufferTile(tile, worldData.isSuperZZT());
    }

    private void setBufferToElement(String element, boolean editOnPlace) {
        Tile tile = getTileFromElement(element, getTileColour(getBufferTile()));
        if (!isValidTile(tile)) return;

        if (editOnPlace) {
            openTileEditorExempt(tile, currentBoard, -1, -1, this::elementPlaceAtCursor, false);
        } else {
            elementPlaceAtCursor(tile);
        }
    }

    private void elementPlaceAtCursor(Tile tile) {
        if (!isValidTile(tile)) return;
        setBufferTile(tile);
        var board = getBoardAt(cursorX, cursorY);

        if (board != null) {
            putTileAt(cursorX, cursorY, tile, PUT_DEFAULT);
            afterModification();
        } else {
            afterUpdate();
        }
    }

    private void paintTile(Tile tile, int col) {
        var backupTile = tile.clone();
        if (isText(tile)) {
            tile.setId(ZType.getTextId(worldData.isSuperZZT(), col));
        } else {
            tile.setCol(col);
        }
        if (!isValidTile(tile)) {
            tile.setId(backupTile.getId());
            tile.setCol(backupTile.getCol());
        }
    }

    private boolean isText(Tile bufferTile) {
        return ZType.isText(worldData.isSuperZZT(), bufferTile.getId());
    }

    private int getTileColour(Tile bufferTile) {
        int col = bufferTile.getCol();
        int id = bufferTile.getId();
        boolean szzt = worldData.isSuperZZT();
        int tcol = ZType.getTextColour(szzt, id);
        if (tcol != -1) {
            col = tcol;
        }
        return col;
    }

    private String getFMenuName(int f) {
        var firstItem = globalEditor.getString(String.format("F%d_MENU_0", f), "");
        if (firstItem.isEmpty()) return "";
        return globalEditor.getString(String.format("F%d_MENU", f), "");
    }

    private void addKeybinds(JComponent component)
    {
        frame.setFocusTraversalKeysEnabled(false);
        component.getActionMap().clear();
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).clear();

        var ge = globalEditor;
        Util.addKeybind(ge, this, component, "Escape");
        Util.addKeybind(ge, this, component, "Up");
        Util.addKeybind(ge, this, component, "Down");
        Util.addKeybind(ge, this, component, "Left");
        Util.addKeybind(ge, this, component, "Right");
        Util.addKeybind(ge, this, component, "Alt-Up");
        Util.addKeybind(ge, this, component, "Alt-Down");
        Util.addKeybind(ge, this, component, "Alt-Left");
        Util.addKeybind(ge, this, component, "Alt-Right");
        Util.addKeybind(ge, this, component, "Shift-Up");
        Util.addKeybind(ge, this, component, "Shift-Down");
        Util.addKeybind(ge, this, component, "Shift-Left");
        Util.addKeybind(ge, this, component, "Shift-Right");
        Util.addKeybind(ge, this, component, "Ctrl-Shift-Up");
        Util.addKeybind(ge, this, component, "Ctrl-Shift-Down");
        Util.addKeybind(ge, this, component, "Ctrl-Shift-Left");
        Util.addKeybind(ge, this, component, "Ctrl-Shift-Right");
        Util.addKeybind(ge, this, component, "Tab");
        Util.addKeybind(ge, this, component, "Home");
        Util.addKeybind(ge, this, component, "End");
        Util.addKeybind(ge, this, component, "Insert");
        Util.addKeybind(ge, this, component, "Space");
        Util.addKeybind(ge, this, component, "Delete");
        Util.addKeybind(ge, this, component, "Enter");
        Util.addKeybind(ge, this, component, "Ctrl-Enter");
        Util.addKeybind(ge, this, component, "Ctrl-=");
        Util.addKeybind(ge, this, component, "Ctrl--");
        Util.addKeybind(ge, this, component, "A");
        Util.addKeybind(ge, this, component, "B");
        Util.addKeybind(ge, this, component, "C");
        Util.addKeybind(ge, this, component, "D");
        Util.addKeybind(ge, this, component, "F");
        Util.addKeybind(ge, this, component, "G");
        Util.addKeybind(ge, this, component, "I");
        Util.addKeybind(ge, this, component, "L");
        Util.addKeybind(ge, this, component, "P");
        Util.addKeybind(ge, this, component, "S");
        Util.addKeybind(ge, this, component, "X");
        Util.addKeybind(ge, this, component, "Ctrl-A");
        Util.addKeybind(ge, this, component, "Ctrl-B");
        Util.addKeybind(ge, this, component, "Ctrl-E");
        Util.addKeybind(ge, this, component, "Ctrl-P");
        Util.addKeybind(ge, this, component, "Ctrl-R");
        Util.addKeybind(ge, this, component, "Ctrl-S");
        Util.addKeybind(ge, this, component, "Ctrl-V");
        Util.addKeybind(ge, this, component, "Ctrl-X");
        Util.addKeybind(ge, this, component, "Ctrl-Y");
        Util.addKeybind(ge, this, component, "Ctrl-Z");
        Util.addKeybind(ge, this, component, "Alt-B");
        Util.addKeybind(ge, this, component, "Alt-F");
        Util.addKeybind(ge, this, component, "Alt-I");
        Util.addKeybind(ge, this, component, "Alt-M");
        Util.addKeybind(ge, this, component, "Alt-S");
        Util.addKeybind(ge, this, component, "Alt-T");
        Util.addKeybind(ge, this, component, "Alt-X");
        Util.addKeybind(ge, this, component, "Shift-B");
        Util.addKeybind(ge, this, component, "Ctrl-Alt-M");
        Util.addKeybind(ge, this, component, "F1");
        Util.addKeybind(ge, this, component, "F2");
        Util.addKeybind(ge, this, component, "F3");
        Util.addKeybind(ge, this, component, "F4");
        Util.addKeybind(ge, this, component, "F5");
        Util.addKeybind(ge, this, component, "F6");
        Util.addKeybind(ge, this, component, "F7");
        Util.addKeybind(ge, this, component, "F8");
        Util.addKeybind(ge, this, component, "F9");
        Util.addKeybind(ge, this, component, "F10");
        Util.addKeybind(ge, this, component, "F12");
        Util.addKeybind(ge, this, component, "Shift-F1");
        Util.addKeybind(ge, this, component, "Shift-F2");
        Util.addKeybind(ge, this, component, "Shift-F3");
        Util.addKeybind(ge, this, component, "Shift-F4");
        Util.addKeybind(ge, this, component, "Shift-F5");
        Util.addKeybind(ge, this, component, "Shift-F6");
        Util.addKeybind(ge, this, component, "Alt-F12");
        Util.addKeybind(ge, this, component, "0");
        Util.addKeybind(ge, this, component, "1");
        Util.addKeybind(ge, this, component, "2");
        Util.addKeybind(ge, this, component, "3");
        Util.addKeybind(ge, this, component, "4");
        Util.addKeybind(ge, this, component, "5");
        Util.addKeybind(ge, this, component, "6");
        Util.addKeybind(ge, this, component, "7");
        Util.addKeybind(ge, this, component, "8");
        Util.addKeybind(ge, this, component, "9");
        Util.addKeybind(ge, this, component, "Ctrl-0");
        Util.addKeybind(ge, this, component, "Ctrl-1");
        Util.addKeybind(ge, this, component, "Ctrl-2");
        Util.addKeybind(ge, this, component, "Ctrl-3");
        Util.addKeybind(ge, this, component, "Ctrl-4");
        Util.addKeybind(ge, this, component, "Ctrl-5");
        Util.addKeybind(ge, this, component, "Ctrl-6");
        Util.addKeybind(ge, this, component, "Ctrl-7");
        Util.addKeybind(ge, this, component, "Ctrl-8");
        Util.addKeybind(ge, this, component, "Ctrl-9");
    }

    @Override
    public void keyAction(String actionName, ActionEvent e) {
        // These actions activate whether textEntry is set or not
        switch (actionName) {
            case "Escape": operationEscape(); break;
            case "Up": operationCursorMove(0, -1, true); break;
            case "Down": operationCursorMove(0, 1, true); break;
            case "Left": operationCursorMove(-1, 0, true); break;
            case "Right": operationCursorMove(1, 0, true); break;
            case "Alt-Up": operationCursorMove(0, -10, true); break;
            case "Alt-Down": operationCursorMove(0, 10, true); break;
            case "Alt-Left": operationCursorMove(-10, 0, true); break;
            case "Alt-Right": operationCursorMove(10, 0, true); break;
            case "Shift-Up": operationExitJump(0); break;
            case "Shift-Down": operationExitJump(1); break;
            case "Shift-Left": operationExitJump(2); break;
            case "Shift-Right": operationExitJump(3); break;
            case "Ctrl-Shift-Up": operationExitCreate(0); break;
            case "Ctrl-Shift-Down": operationExitCreate(1); break;
            case "Ctrl-Shift-Left": operationExitCreate(2); break;
            case "Ctrl-Shift-Right": operationExitCreate(3); break;
            case "Tab": operationToggleDrawing(); break;
            case "Home": operationCursorMove(-999999999, -999999999, false); break;
            case "End": operationCursorMove(999999999, 999999999, false); break;
            case "Insert": operationBufferGrab(); break;
            case "Ctrl-=": operationZoomIn(false); break;
            case "Ctrl--": operationZoomOut(false); break;
            case "Ctrl-X": operationBufferSwapColour(); break;
            case "Ctrl-Y": operationRedo(); break;
            case "Ctrl-Z": operationUndo(); break;
            case "F1": menuHelp(); break;
            case "F2": operationToggleText(); break;
            case "F3": operationF(3); break;
            case "F4": operationF(4); break;
            case "F5": operationF(5); break;
            case "F6": operationF(6); break;
            case "F7": operationF(7); break;
            case "F8": operationF(8); break;
            case "F9": operationF(9); break;
            case "F10": operationF(10); break;
            case "F12": takeScreenshot(); break;
            case "Shift-F1": operationShowTileTypes(SHOW_STATS); break;
            case "Shift-F2": operationShowTileTypes(SHOW_OBJECTS); break;
            case "Shift-F3": operationShowTileTypes(SHOW_INVISIBLES); break;
            case "Shift-F4": operationShowTileTypes(SHOW_EMPTIES); break;
            case "Shift-F5": operationShowTileTypes(SHOW_FAKES); break;
            case "Shift-F6": operationShowTileTypes(SHOW_EMPTEXTS); break;
            case "Alt-F12": takeScreenshotToClipboard(); break;
            default: break;
        }
        if (!textEntry) {
            switch (actionName) {
                case "Space": operationBufferPut(); break;
                case "Delete": operationDelete(); break;
                case "Enter": operationGrabAndModify(true, false); break;
                case "Ctrl-Enter": operationGrabAndModify(true, true); break;
                case "A": operationAddBoard(); break;
                case "B": operationChangeBoard(); break;
                case "C": operationColour(); break;
                case "D": operationDeleteBoard(); break;
                case "F": operationFloodfill(cursorX, cursorY, false); break;
                case "G": Settings.world(frame, boards, worldData, canvas); break;
                case "I": Settings.board(frame, currentBoard, worldData); break;
                case "L": menuOpenWorld(); break;
                case "P": operationModifyBuffer(false); break;
                case "S": menuSaveAs(); break;
                case "X": operationBoardExits(); break;
                case "Ctrl-A": atlas(); break;
                case "Ctrl-B": operationOpenBufferManager(); break;
                case "Ctrl-E": operationErasePlayer(); break;
                case "Ctrl-P": operationModifyBuffer(true); break;
                case "Ctrl-R": atlasRemoveBoard(); break;
                case "Ctrl-S": menuSave(); break;
                case "Ctrl-V": operationPasteImage(); break;
                case "Alt-B": operationBlockStart(); break;
                case "Alt-F": operationFloodfill(cursorX, cursorY, true); break;
                case "Alt-I": menuImportBoard(); break;
                case "Alt-M": operationGrabAndModify(false, false); break;
                case "Alt-S": operationStatList(); break;
                case "Alt-T": operationTestWorld(); break;
                case "Alt-X": menuExportBoard(); break;
                case "Shift-B": menuBoardList(); break;
                case "Ctrl-Alt-M": operationGrabAndModify(false, true); break;
                case "0": operationGetFromBuffer(0); break;
                case "1": operationGetFromBuffer(1); break;
                case "2": operationGetFromBuffer(2); break;
                case "3": operationGetFromBuffer(3); break;
                case "4": operationGetFromBuffer(4); break;
                case "5": operationGetFromBuffer(5); break;
                case "6": operationGetFromBuffer(6); break;
                case "7": operationGetFromBuffer(7); break;
                case "8": operationGetFromBuffer(8); break;
                case "9": operationGetFromBuffer(9); break;
                case "Ctrl-0": operationSaveToBuffer(0); break;
                case "Ctrl-1": operationSaveToBuffer(1); break;
                case "Ctrl-2": operationSaveToBuffer(2); break;
                case "Ctrl-3": operationSaveToBuffer(3); break;
                case "Ctrl-4": operationSaveToBuffer(4); break;
                case "Ctrl-5": operationSaveToBuffer(5); break;
                case "Ctrl-6": operationSaveToBuffer(6); break;
                case "Ctrl-7": operationSaveToBuffer(7); break;
                case "Ctrl-8": operationSaveToBuffer(8); break;
                case "Ctrl-9": operationSaveToBuffer(9); break;
                default: break;
            }
        }
    }

    private void operationShowTileTypes(int showMode) {
        if (currentlyShowing == showMode) {
            currentlyShowing = SHOW_NOTHING;
        } else {
            currentlyShowing = showMode;
        }
        afterChangeShowing();
    }

    private void takeScreenshot() {
        var boardBuffer = canvas.getBoardBuffer(worldData.isSuperZZT());
        var now = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH-mm-ss");
        var screenshotNameTemplate = globalEditor.getString("SCREENSHOT_NAME", "Screenshot {date} {time}.png");
        var screenshotFilename = screenshotNameTemplate.replace("{date}", dateFormatter.format(now));
        screenshotFilename = screenshotFilename.replace("{time}", timeFormatter.format(now));
        screenshotFilename = screenshotFilename.replace("{worldname}", CP437.toUnicode(worldData.getName()));
        var currentBoardName = currentBoard == null ? "(no board)" : CP437.toUnicode(currentBoard.getName());
        screenshotFilename = screenshotFilename.replace("{boardname}", currentBoardName);
        screenshotFilename = screenshotFilename.replace("{boardnum}", String.valueOf(currentBoardIdx));

        //var screenshotFilename = String.format("Screenshot %s.png", dtf.format(now));

        var screenshotDir = Util.evalConfigDir(globalEditor.getString("SCREENSHOT_DIR", ""));

        File file;
        if (screenshotDir.isEmpty()) {
            var writer = globalEditor.getWriterInLocalDir(screenshotFilename, true);
            if (writer == null) {
                JOptionPane.showMessageDialog(frame, "Could not find a directory to save the screenshot to", "Failed to save screenshot", JOptionPane.ERROR_MESSAGE);
                return;
            }
            file = writer.getFile();
        } else {
            file = Path.of(screenshotDir, screenshotFilename).toFile();
        }
        try {
            ImageIO.write(boardBuffer, "png", file);
            //System.out.println("Saved screenshot to " + writer.getFile().toString());
            editingModePane.display(Color.YELLOW, 1500, "Saved Screenshot");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, e, "Failed to save screenshot", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void takeScreenshotToClipboard() {
        var boardBuffer = canvas.getBoardBuffer(worldData.isSuperZZT());
        var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        var transferable = new Transferable() {
            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[]{DataFlavor.imageFlavor};
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return flavor.equals(DataFlavor.imageFlavor);
            }

            @Override
            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
                if (flavor.equals(DataFlavor.imageFlavor)) return boardBuffer;
                throw new UnsupportedFlavorException(flavor);
            }
        };
        clipboard.setContents(transferable, (clipbrd, contents) -> { });
        editingModePane.display(Color.YELLOW, 1500, "Copied Screenshot");
    }

    private void operationPasteImage() {
        var image = getClipboardImage();
        if (image == null) return;

        new ConvertImage(this, image);
    }

    private void operationLoadImage() {
        var fileChooser = getFileChooser(new String[]{"png", "jpg", "jpeg", "gif", "bmp"}, "Bitmap image file");
        int result = fileChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                var image = ImageIO.read(file);
                if (image == null) throw new RuntimeException("Unrecognised file format");
                new ConvertImage(this, image);
            } catch (IOException | RuntimeException e) {
                JOptionPane.showMessageDialog(frame, e, "Error loading image", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private Image getClipboardImage() {
        var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        var contents = clipboard.getContents(null);
        try {
            return (Image) contents.getTransferData(DataFlavor.imageFlavor);
        } catch (UnsupportedFlavorException | IOException e) {
            return null;
        }
    }

    private void operationOpenBufferManager() {
        if (currentBufferManager != null) {
            currentBufferManager.toFront();
            frame.requestFocus();
        } else {
            currentBufferManager = new BufferManager(this);
        }
    }

    public String prefix() {
        return worldData.isSuperZZT() ? "SZZT_" : "ZZT_";
    }

    public void operationSaveToBuffer(int bufferNum) {
        String data;
        if (blockStartX != -1) {
            // Block selected.
            blockCopy(false);
        }
        data = globalEditor.encodeBuffer();
        setBlockBuffer(0, 0, null, false);
        String key = String.format(prefix()+"BUF_%d", bufferNum);
        globalEditor.setString(key, data);
        globalEditor.setInt(prefix()+"BUF_MAX", Math.max(bufferNum,
                globalEditor.getInt(prefix()+"BUF_MAX", 0)));
        globalEditor.setBufferPos(bufferNum, currentBufferManager);
        if (currentBufferManager != null) {
            currentBufferManager.updateBuffer(bufferNum);
        }
        afterUpdate();
        editingModePane.display(Color.MAGENTA, 1500, "Saved to buffer #" + bufferNum);
    }

    public void operationGetFromBuffer(int bufferNum) {
        String key = String.format(prefix()+"BUF_%d", bufferNum);
        if (globalEditor.isKey(key)) {
            globalEditor.decodeBuffer(globalEditor.getString(key));
            globalEditor.setBufferPos(bufferNum, currentBufferManager);
            if (currentBufferManager != null) {
                currentBufferManager.updateBuffer(bufferNum);
            }
            afterUpdate();
            editingModePane.display(Color.PINK, 750, "Loaded from buffer #" + bufferNum);
        }
    }


    private void operationZoomOut(boolean mouse) {
        changeZoomLevel(-1, mouse);
    }

    private void operationZoomIn(boolean mouse) {
        changeZoomLevel(1, mouse);
    }
    private void operationResetZoom(boolean mouse) {
        changeZoomLevel(0, mouse);
    }

    private void changeZoomLevel(int zoomChange, boolean mouse) {
        double zoomFactor = globalEditor.getDouble("ZOOM_FACTOR", Math.sqrt(2));
        double minZoom = globalEditor.getDouble("MIN_ZOOM", 0.0625);
        double maxZoom = globalEditor.getDouble("MAX_ZOOM", 8.0);
        double newZoom = zoom;

        if (zoomChange == 1) {
            newZoom *= zoomFactor;
        } else if (zoomChange == -1) {
            newZoom /= zoomFactor;
        } else if (zoomChange == 0) {
            newZoom = 1.0;
        }

        // Find nearest zoom level
        double iterZoom = 1.0;
        double iterZoomFactor = zoomFactor;
        if (newZoom < iterZoom) iterZoomFactor = 1.0 / iterZoomFactor;

        for (;;) {
            double iterZoomNew = iterZoom * iterZoomFactor;
            if (Math.abs(iterZoom - newZoom) < Math.abs(iterZoomNew - newZoom)) {
                zoom = iterZoom;
                break;
            }
            iterZoom = iterZoomNew;
        }
        newZoom = Util.clamp(newZoom, minZoom, maxZoom);
        double zoomDiff = newZoom - zoom;
        if (Math.abs(zoomDiff) > 0.0001) {
            zoom = newZoom;
        }

        int centreOnX, centreOnY;
        if (mouse) {
            centreOnX = mouseX;
            centreOnY = mouseY;
        } else {
            centreOnX = cursorX;
            centreOnY = cursorY;
        }

        invalidateCache();
        afterModification();
        canvas.revalidate();
        if (frame.getExtendedState() == Frame.NORMAL) {
            frame.pack();
        }
        centreOn(centreOnX, centreOnY);
        canvas.recheckMouse();
    }

    private void centreOn(int x, int y) {
        int xPos = canvas.getCharW(x);
        int yPos = canvas.getCharH(y);
        int xSize = canvas.getCharW(1);
        int ySize = canvas.getCharH(1);
        int xAdd = canvas.getVisibleRect().width / 2;
        int yAdd = canvas.getVisibleRect().height / 2;

        xSize += xAdd * 2;
        ySize += yAdd * 2;
        xPos -= xAdd;
        yPos -= yAdd;

        canvas.scrollRectToVisible(new Rectangle(xPos, yPos, xSize, ySize));
    }

    private void operationFloodfill(int x, int y, boolean fancy) {
        var originalTile = getTileAt(x, y, false);
        if (originalTile == null) return;

        byte[][] filled = new byte[height][width];
        var tileStats = originalTile.getStats();
        boolean isStatted = false;
        if (tileStats != null && !tileStats.isEmpty()) isStatted = true;

        floodFill(x, y, originalTile.getId(), originalTile.getCol(), isStatted, filled);

        if (!fancy) {

            var dirty = new HashSet<Board>();
            for (int fy = 0; fy < height; fy++) {
                for (int fx = 0; fx < width; fx++) {
                    if (filled[fy][fx] == 1) {
                        var board = putTileDeferred(fx, fy, getBufferTile(), PUT_DEFAULT);
                        if (board != null) dirty.add(board);
                    }
                }
            }
            for (var board : dirty) {
                board.finaliseStats();
            }
            afterModification();
        } else {
            fancyFill(filled);
        }
    }

    private void fancyFill(byte[][] filled) {
        var boardListing = new HashSet<Integer>();
        for (int fy = 0; fy < height; fy++)
            for (int fx = 0; fx < width; fx++)
                if (filled[fy][fx] == 1)
                    boardListing.add(grid[fy / boardH][fx / boardW]);
        var savedBoards = new HashMap<Integer, Board>();
        for (var boardIdx : boardListing)
            savedBoards.put(boardIdx, boards.get(boardIdx).clone());
        fancyFillDialog = true;
        var listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                FancyFill fill = (FancyFill) e.getSource();
                if (e.getActionCommand().equals("updateFill")) {

                    var tileXs = fill.getXs();
                    var tileYs = fill.getYs();
                    var tiles = fill.getTiles();
                    HashSet<Board> boardsHit = new HashSet<>();
                    for (int i = 0; i < tileXs.length; i++) {
                        boardsHit.add(putTileDeferred(tileXs[i], tileYs[i], tiles[i], PUT_REPLACE_BOTH));
                    }
                    for (var board : boardsHit) {
                        board.finaliseStats();
                    }
                    afterModification();
                } else if (e.getActionCommand().equals("undo")) {
                    fancyFillDialog = false;
                    for (var boardIdx : boardListing) {
                        savedBoards.get(boardIdx).cloneInto(boards.get(boardIdx));
                    }
                    addRedraw(1, 1, width - 2, height - 2);
                    afterModification();
                } else if (e.getActionCommand().equals("done")) {
                    fancyFillDialog = false;
                    afterUpdate();
                }
            }
        };
        new FancyFill(this, listener, filled);
    }

    private void floodFill(int startX, int startY, int id, int col, boolean statted, byte[][] filled)
    {
        final int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        var stack = new ArrayDeque<Integer>();
        stack.add(startX);
        stack.add(startY);
        filled[startY][startX] = 1;

        while (!stack.isEmpty()) {
            int x = stack.pop();
            int y = stack.pop();

            for (var dir : dirs) {
                int nx = x + dir[0];
                int ny = y + dir[1];
                if (nx >= 0 && ny >= 0 && nx < width && ny < height) {
                    if (filled[ny][nx] == 0) {
                        var board = getBoardAt(nx, ny);
                        if (board != null) {
                            if (board.getTileId(nx % boardW, ny % boardH) == id) {
                                if (id == ZType.EMPTY || board.getTileCol(nx % boardW, ny % boardH) == col) {
                                    if (!board.getStatsAt(nx % boardW, ny % boardH).isEmpty() == statted) {
                                        filled[ny][nx] = 1;
                                        stack.add(nx);
                                        stack.add(ny);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void operationToggleDrawing() {
        if (operationCancel()) return;
        setDrawing(true);
        putTileAt(cursorX, cursorY, getBufferTile(), PUT_DEFAULT);
        afterModification();
    }

    private void operationToggleText() {
        if (operationCancel()) return;
        setTextEntry(true);
        textEntryX = cursorX;
        afterUpdate();
    }

    private void setTextEntry(boolean state) {
        textEntry = state;
        canvas.setTextEntry(state);
    }

    private void setDrawing(boolean state) {
        drawing = state;
        canvas.setDrawing(state);
    }

    private void setBlockStart(int x, int y) {
        blockStartX = x;
        blockStartY = y;
        canvas.setSelectionBlock(blockStartX, blockStartY);
    }

    private void setBlockBuffer(int w, int h, Tile[] ar, boolean r) {
        globalEditor.setBlockBuffer(w, h, ar, r, worldData.isSuperZZT());
        canvas.repaint();
    }

    private void operationEscape() {
        if (operationCancel()) return;
        tryClose();
    }

    private boolean operationCancel() {
        if (drawing) {
            setDrawing(false);
            afterUpdate();
            return true;
        }
        if (textEntry) {
            setTextEntry(false);
            afterUpdate();
            return true;
        }
        if (globalEditor.isBlockBuffer()) {
            setBlockBuffer(0, 0, null, false);
            afterUpdate();
            return true;
        }
        if (blockStartX != -1) {
            setBlockStart(-1, -1);
            afterUpdate();
            return true;
        }
        if (moveBlockW != 0) {
            setMoveBlock(0, 0);
            afterUpdate();
            return true;
        }

        if (currentlyShowing != SHOW_NOTHING) {
            currentlyShowing = SHOW_NOTHING;
            afterChangeShowing();
            return true;
        }
        return false;
    }



    private void operationBlockStart() {
        operationCancel();
        setBlockStart(cursorX, cursorY);

        afterUpdate();
    }

    private void operationBlockEnd() {
        final int saved_blockStartX = blockStartX;
        final int saved_blockStartY = blockStartY;
        final int saved_cursorX = cursorX;
        final int saved_cursorY = cursorY;
        var popupMenu = new JPopupMenu("Choose block command");
        popupMenu.addPopupMenuListener(this);
        String[] menuItems = {"Copy block", "Copy block (repeated)", "Move block", "Clear block", "Flip block",
                "Mirror block", "Paint block"};
        ActionListener listener = e -> {
            if (blockStartX != saved_blockStartX ||
                    blockStartY != saved_blockStartY ||
                    cursorX != saved_cursorX ||
                    cursorY != saved_cursorY) return;

            var menuItem = ((JMenuItem)e.getSource()).getText();
            switch (menuItem) {
                case "Copy block": blockCopy(false); break;
                case "Copy block (repeated)": blockCopy(true); break;
                case "Move block": blockMove(); break;
                case "Clear block": blockClear(); break;
                case "Flip block": blockFlip(false); break;
                case "Mirror block": blockFlip(true); break;
                case "Paint block": blockPaint(); break;
                default: break;
            }
        };

        for (var item : menuItems) {
            var menuItem = new JMenuItem(item);
            menuItem.addActionListener(listener);
            popupMenu.add(menuItem);
        }
        popupMenu.show(frame, (frame.getWidth() - popupMenu.getPreferredSize().width) / 2,
                (frame.getHeight() - popupMenu.getPreferredSize().height) / 2);

        // From https://stackoverflow.com/a/7754567
        SwingUtilities.invokeLater(() -> popupMenu.dispatchEvent(
                new KeyEvent(popupMenu, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_DOWN, '\0')
        ));
    }

    private int getBlockX1() { return Math.min(cursorX, blockStartX); }
    private int getBlockY1() { return Math.min(cursorY, blockStartY); }
    private int getBlockX2() { return Math.max(cursorX, blockStartX); }
    private int getBlockY2() { return Math.max(cursorY, blockStartY); }
    private void afterBlockOperation(boolean modified) {
        setBlockStart(-1, -1);
        if (modified) afterModification();
        else afterUpdate();
    }

    private void blockClear() {
        Tile tile = new Tile(0, 0);
        addRedraw(getBlockX1(), getBlockY1(), getBlockX2(), getBlockY2());
        for (int y = getBlockY1(); y <= getBlockY2(); y++) {
            for (int x = getBlockX1(); x <= getBlockX2(); x++) {
                putTileAt(x, y, tile, PUT_REPLACE_BOTH);
            }
        }

        afterBlockOperation(true);
    }

    private void blockPaint() {
        int paintCol = getTileColour(getBufferTile());
        addRedraw(getBlockX1(), getBlockY1(), getBlockX2(), getBlockY2());
        for (int y = getBlockY1(); y <= getBlockY2(); y++) {
            for (int x = getBlockX1(); x <= getBlockX2(); x++) {
                var tile = getTileAt(x, y, false);
                paintTile(tile, paintCol);
                putTileAt(x, y, tile, PUT_REPLACE_BOTH);
            }
        }
        afterBlockOperation(true);
    }

    private void blockCopy(boolean repeated) {
        int w = getBlockX2() + 1 - getBlockX1();
        int h = getBlockY2() + 1 - getBlockY1();
        var blockBuffer = new Tile[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int xPos = x + getBlockX1();
                int yPos = y + getBlockY1();
                int idx = y * w + x;
                blockBuffer[idx] = getTileAt(xPos, yPos, true);
            }
        }
        setBlockBuffer(w, h, blockBuffer, repeated);
        afterBlockOperation(false);
    }

    private void setMoveBlock(int w, int h) {
        moveBlockW = w;
        moveBlockH = h;
        canvas.setPlacingBlock(w, h);
    }

    private void blockMove() {
        moveBlockX = getBlockX1();
        moveBlockY = getBlockY1();
        int w = getBlockX2() + 1 - getBlockX1();
        int h = getBlockY2() + 1 - getBlockY1();
        setMoveBlock(w, h);
        afterBlockOperation(false);
    }

    private void blockFinishMove() {
        // Move from moveBlockX, moveBlockY, moveBlockW, moveBlockH, cursorX, cursorY

        var blockMap = new LinkedHashMap<ArrayList<Board>, LinkedHashMap<ArrayList<Integer>, ArrayList<Integer>>>();
        addRedraw(cursorX, cursorY, cursorX + moveBlockW - 1, cursorY + moveBlockH - 1);
        addRedraw(moveBlockX, moveBlockY, moveBlockX + moveBlockW - 1, moveBlockY + moveBlockH - 1);
        for (int vy = 0; vy < moveBlockH; vy++) {
            for (int vx = 0; vx < moveBlockW; vx++) {
                // The move order depends on the relationship between the two blocks, to avoid double moving
                int x = moveBlockX >= cursorX ? vx : moveBlockW - 1 - vx;
                int y = moveBlockY >= cursorY ? vy : moveBlockH - 1 - vy;
                int xFrom = x + moveBlockX;
                int yFrom = y + moveBlockY;
                int xTo = x + cursorX;
                int yTo = y + cursorY;
                if (xFrom < width && yFrom < height) {
                    if (xTo < width && yTo < height) {
                        var boardKey = new ArrayList<Board>(2);
                        boardKey.add(getBoardAt(xFrom, yFrom));
                        boardKey.add(getBoardAt(xTo, yTo));
                        var from = pair(xFrom, yFrom);
                        var to = pair(xTo, yTo);

                        if (!blockMap.containsKey(boardKey)) {
                            blockMap.put(boardKey, new LinkedHashMap<>());
                        }
                        blockMap.get(boardKey).put(from, to);
                    }
                }
            }
        }

        blockTileMove(blockMap, false);

        setMoveBlock(0, 0);
        afterBlockOperation(true);
    }

    private void blockFlip(boolean horizontal) {
        int hx = (getBlockX1() + getBlockX2()) / 2;
        int hy = (getBlockY1() + getBlockY2()) / 2;
        var blockMap = new LinkedHashMap<ArrayList<Board>, LinkedHashMap<ArrayList<Integer>, ArrayList<Integer>>>();
        addRedraw(getBlockX1(), getBlockY1(), getBlockX2(), getBlockY2());
        for (int y = getBlockY1(); y <= getBlockY2(); y++) {
            for (int x = getBlockX1(); x <= getBlockX2(); x++) {
                if ((horizontal && x <= hx) || (!horizontal && y <= hy)) {
                    int xTo = horizontal ? getBlockX2() - (x - getBlockX1()) : x;
                    int yTo = !horizontal ? getBlockY2() - (y - getBlockY1()) : y;

                    var boardKey = new ArrayList<Board>(2);
                    boardKey.add(getBoardAt(x, y));
                    boardKey.add(getBoardAt(xTo, yTo));
                    var from = pair(x, y);
                    var to = pair(xTo, yTo);

                    if (!blockMap.containsKey(boardKey)) {
                        blockMap.put(boardKey, new LinkedHashMap<>());
                    }
                    blockMap.get(boardKey).put(from, to);
                }
            }
        }

        blockTileMove(blockMap, true);
        afterBlockOperation(true);
    }

    private void blockTileMove(LinkedHashMap<ArrayList<Board>, LinkedHashMap<ArrayList<Integer>, ArrayList<Integer>>> blockMap, boolean swap)
    {
        Tile blankTile = new Tile(0, 0);
        for (var boardKey : blockMap.keySet()) {
            var tileMoves = blockMap.get(boardKey);
            var fromBoard = boardKey.get(0);
            var toBoard = boardKey.get(1);
            if (fromBoard == null || toBoard == null) continue;

            var firstFrom = tileMoves.keySet().iterator().next();
            int fromBoardXOffset = firstFrom.get(0) / boardW * boardW;
            int fromBoardYOffset = firstFrom.get(1) / boardH * boardH;

            if (fromBoard == toBoard) {
                if (!swap) {
                    // If stat0 is being overwritten and isn't being moved itself, don't move it
                    int stat0x = toBoard.getStat(0).getX() - 1 + fromBoardXOffset;
                    int stat0y = toBoard.getStat(0).getY() - 1 + fromBoardYOffset;
                    if (!tileMoves.containsKey(pair(stat0x, stat0y))) {
                        for (var from : tileMoves.keySet()) {
                            var to = tileMoves.get(from);
                            if (to.get(0) == stat0x && to.get(1) == stat0y) {
                                tileMoves.remove(from);
                                break;
                            }
                        }
                    }
                }

                HashMap<ArrayList<Integer>, ArrayList<Integer>> reverseTileMoves = new HashMap<>();
                for (var from : tileMoves.keySet()) {
                    var to = tileMoves.get(from);
                    reverseTileMoves.put(to, from);
                }

                // Same board
                var deleteStats = new ArrayList<Integer>();
                for (int i = 0; i < toBoard.getStatCount(); i++) {
                    var stat = toBoard.getStat(i);
                    var from = pair(stat.getX() - 1 + fromBoardXOffset, stat.getY() - 1 + fromBoardYOffset);
                    var to = tileMoves.get(from);
                    if (to != null) {
                        //System.out.printf("Stat %d moving from %d,%d to %d,%d\n", i, stat.getX(), stat.getY(), to.get(0) % boardW + 1, to.get(1) % boardH + 1);
                        stat.setX(to.get(0) % boardW + 1);
                        stat.setY(to.get(1) % boardH + 1);
                        continue;
                    }
                    to = from;
                    from = reverseTileMoves.get(to);
                    if (from != null) {
                        if (swap) {
                            //System.out.printf("Stat %d moving from %d,%d to %d,%d !\n", i, stat.getX(), stat.getY(), from.get(0) % boardW + 1, from.get(1) % boardH + 1);
                            stat.setX(from.get(0) % boardW + 1);
                            stat.setY(from.get(1) % boardH + 1);
                        } else {
                            if (i != 0) deleteStats.add(i);
                        }
                    }
                }

                toBoard.directDeleteStats(deleteStats);

                var changingTiles = new HashMap<ArrayList<Integer>, ArrayList<Integer>>();
                for (var from : tileMoves.keySet()) {
                    var to = tileMoves.get(from);
                    var id = toBoard.getTileId(from.get(0) % boardW, from.get(1) % boardH);
                    var col = toBoard.getTileCol(from.get(0) % boardW, from.get(1) % boardH);
                    changingTiles.put(to, pair(id, col));
                    if (swap) {
                        var tid = toBoard.getTileId(to.get(0) % boardW, to.get(1) % boardH);
                        var tcol = toBoard.getTileCol(to.get(0) % boardW, to.get(1) % boardH);
                        changingTiles.put(from, pair(tid, tcol));
                    } else {
                        toBoard.setTileRaw(from.get(0) % boardW, from.get(1) % boardH, blankTile.getId(), blankTile.getCol());
                    }
                }
                for (var to : changingTiles.keySet()) {
                    var tile = changingTiles.get(to);
                    toBoard.setTileRaw(to.get(0) % boardW, to.get(1) % boardH, tile.get(0), tile.get(1));
                }
                toBoard.finaliseStats();

                // Copy to buffer

            } else {
                // Different board

                for (var from : tileMoves.keySet()) {
                    var to = tileMoves.get(from);
                    var tile = getTileAt(from.get(0), from.get(1), true);
                    if (swap) {
                        var otherTile = getTileAt(to.get(0), to.get(1), true);
                        putTileAt(from.get(0), from.get(1), otherTile, PUT_REPLACE_BOTH);
                    } else {
                        putTileAt(from.get(0), from.get(1), blankTile, PUT_REPLACE_BOTH);
                    }
                    putTileAt(to.get(0), to.get(1), tile, PUT_REPLACE_BOTH);
                }
            }
        }
    }

    private void blockPaste() {
        int w = globalEditor.getBlockBufferW();
        int h = globalEditor.getBlockBufferH();
        var blockBuffer = globalEditor.getBlockBuffer(worldData.isSuperZZT());
        addRedraw(cursorX, cursorY, cursorX + w - 1, cursorY + h - 1);

        // Find the player
        int px = -1, py = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int xPos = x + cursorX;
                int yPos = y + cursorY;
                if (xPos < width && yPos < height) {
                    int idx = y * w + x;
                    Tile t = blockBuffer[idx];
                    var st = t.getStats();
                    if (!st.isEmpty()) {
                        if (st.get(0).isPlayer()) {
                            // Player has been found. Record the player's location
                            // then place
                            px = x;
                            py = y;
                            putTileAt(xPos, yPos, blockBuffer[idx], PUT_REPLACE_BOTH);
                        }
                    }
                }
            }
        }

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (x == px && y == py) {
                    continue; // We have already placed the player
                }
                int xPos = x + cursorX;
                int yPos = y + cursorY;
                if (xPos < width && yPos < height) {
                    int idx = y * w + x;
                    putTileAt(xPos, yPos, blockBuffer[idx], PUT_REPLACE_BOTH);
                }
            }
        }

        if (!globalEditor.getBlockBufferRepeated()) {
            setBlockBuffer(0, 0, null, false);
        }
        afterBlockOperation(true);
    }

    private void operationF(int f) {
        JPopupMenu popup = new JPopupMenu();
        var fMenuName = getFMenuName(f);
        var items = 0;
        if (fMenuName != null) {
            var fMenuItems = getFMenuItems(f);
            for (var fMenuItem : fMenuItems) {
                popup.add(fMenuItem);
                items++;
            }
        }
        popup.addPopupMenuListener(this);
        if (items > 0) {
            popup.show(frame, (frame.getWidth() - popup.getPreferredSize().width) / 2,
                    (frame.getHeight() - popup.getPreferredSize().height) / 2);
        }
    }

    private void operationColour() {
        int col = getTileColour(getBufferTile());
        ColourSelector.createColourSelector(this, col, frame, e -> {
            int newCol = Integer.parseInt(e.getActionCommand());
            var tile = getBufferTile();
            paintTile(tile, newCol);
            setBufferTile(tile);
            afterUpdate();
        }, isText(getBufferTile()) ? ColourSelector.TEXTCOLOUR : ColourSelector.COLOUR);
    }

    void testCharsetPalette(File dir, String basename, ArrayList<File> unlinkList, ArrayList<String> argList) throws IOException
    {
        var palette = canvas.getPalette();
        if (palette != Data.DEFAULT_PALETTE) {
            File palFile = Paths.get(dir.getPath(), basename + ".PAL").toFile();
            Files.write(palFile.toPath(), palette);
            unlinkList.add(palFile);
            argList.add("-l");
            argList.add("palette:pal:" + basename + ".PAL");
        }
        var charset = canvas.getCharset();
        if (charset != Data.DEFAULT_CHARSET) {
            File chrFile = Paths.get(dir.getPath(), basename + ".CHR").toFile();
            Files.write(chrFile.toPath(), charset);
            unlinkList.add(chrFile);
            argList.add("-l");
            argList.add("charset:chr:" + basename + ".CHR");
        }
    }

    private void operationTestWorld() {
        int changeBoardTo;
        if (globalEditor.getBoolean("TEST_SWITCH_BOARD", false)) {
            changeBoardTo = currentBoardIdx;
        } else {
            changeBoardTo = worldData.getCurrentBoard();
        }
        if (changeBoardTo == -1) return;

        try {
            ArrayList<File> unlinkList = new ArrayList<>();

            String zzt = worldData.isSuperZZT() ? "SZZT" : "ZZT";
            String ext = worldData.isSuperZZT() ? ".SZT" : ".ZZT";
            String hiext = worldData.isSuperZZT() ? ".HGS" : ".HI";
            String testPath = Util.evalConfigDir(globalEditor.getString(zzt+"_TEST_PATH", ""));
            if (testPath.isBlank()) {
                var errmsg = String.format("You need to configure a %s test directory in the settings before you can test worlds.",
                        worldData.isSuperZZT() ? "Super ZZT" : "ZZT");
                JOptionPane.showMessageDialog(frame, errmsg, "Error testing world", JOptionPane.ERROR_MESSAGE);
                return;
            }
            File dir = new File(testPath);
            File zeta = Paths.get(dir.getPath(), globalEditor.getString(zzt+"_TEST_COMMAND")).toFile();
            String basename = "";
            File testFile = null;
            File testFileHi = null;
            for (int nameSuffix = 0; nameSuffix < 99; nameSuffix++) {
                basename = globalEditor.getString(zzt + "_TEST_FILENAME");
                if (nameSuffix > 1) {
                    String suffixString = String.valueOf(nameSuffix);
                    int maxLen = 8 - suffixString.length();
                    if (basename.length() > maxLen) {
                        basename = basename.substring(0, maxLen);
                    }
                    basename += suffixString;
                }
                if (basename.length() > 8) basename = basename.substring(0, 8);
                testFile = Paths.get(dir.getPath(), basename + ext).toFile();
                testFileHi = Paths.get(dir.getPath(), basename + hiext).toFile();
                if (!testFile.exists()) {
                    break;
                }
                testFile = null;
                testFileHi = null;
            }
            if (testFile == null) {
                throw new IOException("Error creating test file");
            }
            unlinkList.add(testFile);
            unlinkList.add(testFileHi);
            ArrayList<String> argList = new ArrayList<>();
            argList.add(zeta.getPath());

            if (globalEditor.getBoolean(zzt+"_TEST_USE_CHARPAL", false)) {
                testCharsetPalette(dir, basename, unlinkList, argList);
            }
            if (globalEditor.getBoolean(zzt+"_TEST_USE_BLINK", false)) {
                if (!globalEditor.getBoolean("BLINKING", true)) {
                    argList.add("-b");
                }
            }
            var params = globalEditor.getString(zzt+"_TEST_PARAMS").split(" ");
            argList.addAll(Arrays.asList(params));

            argList.add(basename+ext);

            boolean inject_P = globalEditor.getBoolean(zzt+"_TEST_INJECT_P", false);
            int delay_P = globalEditor.getInt(zzt+"_TEST_INJECT_P_DELAY", 0);
            boolean inject_Enter = false;
            int delay_Enter = 0;
            if (worldData.isSuperZZT()) {
                inject_Enter = globalEditor.getBoolean(zzt+"_TEST_INJECT_ENTER", false);
                delay_Enter = globalEditor.getInt(zzt+"_TEST_INJECT_ENTER_DELAY", 0);
            }
            launchTest(argList, dir, testFile, unlinkList, changeBoardTo, inject_P, delay_P, inject_Enter, delay_Enter);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, e, "Error testing world", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void launchTest(ArrayList<String> argList, File dir, File testFile, ArrayList<File> unlinkList, int testBoard,
                            boolean inject_P, int delay_P, boolean inject_Enter, int delay_Enter) {
        /*
        synchronized (deleteOnClose) {
            for (var unlinkFile : unlinkList) {
                deleteOnClose.add(unlinkFile);
            }
        }
        */
        Thread testThread = new Thread(() -> {
            try {
                var worldCopy = worldData.clone();
                worldCopy.setCurrentBoard(testBoard);
                ProcessBuilder pb = new ProcessBuilder(argList);

                if (saveGame(testFile, worldCopy)) {
                    pb.directory(dir);
                    Process p = pb.start();
                    if (inject_P || inject_Enter) {
                        Robot r = new Robot();
                        if (inject_P) {
                            r.delay(delay_P);
                            r.keyPress(KeyEvent.VK_P);
                            r.keyRelease(KeyEvent.VK_P);
                        }
                        if (inject_Enter) {
                            r.delay(delay_Enter);
                            r.keyPress(KeyEvent.VK_ENTER);
                            r.keyRelease(KeyEvent.VK_ENTER);
                        }
                    }
                    p.waitFor();
                } else {
                    throw new IOException("Error creating test file");
                }


                for (var unlinkFile : unlinkList) {
                    if (unlinkFile.exists()) {
                        unlinkFile.delete();
                        /*
                        synchronized (deleteOnClose) {
                            deleteOnClose.remove(unlinkFile);
                        }
                        */
                    }
                }
            } catch (IOException | AWTException e) {
                JOptionPane.showMessageDialog(frame, e);
            } catch (InterruptedException ignored) {
            }
        });
        testThreads.add(testThread);
        testThread.start();
    }

    private void operationExitJump(int exit) {
        int destBoard = currentBoard.getExit(exit);
        if (destBoard != 0) changeBoard(destBoard);
    }

    private void operationExitCreate(int exit) {
        final int[] exitRecip = {1, 0, 3, 2};
        final int[] xOff = {0, 0, -boardW, boardW};
        final int[] yOff = {-boardH, boardH, 0, 0};

        int oldBoardIdx = currentBoardIdx;
        int destBoard = currentBoard.getExit(exit);
        if (destBoard != 0) {
            changeBoard(destBoard);
        } else {
            int savedCursorX = cursorX;
            int savedCursorY = cursorY;
            cursorX += xOff[exit];
            cursorY += yOff[exit];
            if (cursorX < 0 || cursorY < 0 || cursorX >= width || cursorY >= height) {
                cursorX = savedCursorX;
                cursorY = savedCursorY;
            }
            int newBoardIdx = operationAddBoard();
            if (newBoardIdx != -1) {
                boards.get(oldBoardIdx).setExit(exit, newBoardIdx);
                boards.get(newBoardIdx).setExit(exitRecip[exit], oldBoardIdx);
                canvas.setCursor(cursorX, cursorY);
            } else {
                cursorX = savedCursorX;
                cursorY = savedCursorY;
            }
            afterUpdate();
        }
    }

    public int operationAddBoard() {
        var response = JOptionPane.showInputDialog(frame, "Name for new board:");
        if (response != null) {
            Board newBoard = blankBoard(response);
            int newBoardIdx = boards.size();
            boards.add(newBoard);

            boolean addedToAtlas = false;

            if (currentAtlas != null) {
                int gridX = cursorX / boardW;
                int gridY = cursorY / boardH;
                if (grid[gridY][gridX] == -1) {
                    addedToAtlas = true;
                    grid[gridY][gridX] = newBoardIdx;
                    atlases.put(newBoardIdx, currentAtlas);

                    final int[][] dirs = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
                    final int[] dirReverse = {1, 0, 3, 2};

                    for (int exit = 0; exit < 4; exit++) {
                        int bx = gridX + dirs[exit][0];
                        int by = gridY + dirs[exit][1];
                        if (bx >= 0 && by >= 0 && bx < gridW && by < gridH) {
                            int boardAtIdx = grid[by][bx];
                            if (boardAtIdx != -1) {
                                int revExit = dirReverse[exit];
                                var boardAt = boards.get(boardAtIdx);
                                if (boardAt.getExit(revExit) == 0) {
                                    boardAt.setExit(revExit, newBoardIdx);
                                    newBoard.setExit(exit, boardAtIdx);
                                }
                            }
                        }
                    }

                }
            }
            if (!addedToAtlas) {
                changeBoard(boards.size() - 1);
            } else {
                invalidateCache();
                afterModification();
            }
            return newBoardIdx;
        }
        return -1;
    }

    private Board blankBoard(String name) {
        if (worldData.isSuperZZT()) {
            return new SZZTBoard(name);
        } else {
            return new ZZTBoard(name);
        }
    }

    private void operationAddBoardGrid() {
        var dlg = new JDialog();
        //Util.addEscClose(settings, settings.getRootPane());
        //Util.addKeyClose(settings, settings.getRootPane(), KeyEvent.VK_ENTER, 0);
        dlg.setResizable(false);
        dlg.setTitle("Add Boards (*x* grid)");
        dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dlg.setModalityType(JDialog.ModalityType.APPLICATION_MODAL);
        dlg.getContentPane().setLayout(new BorderLayout());
        var cp = new JPanel(new GridLayout(0, 1));
        cp.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        dlg.getContentPane().add(cp, BorderLayout.CENTER);

        var widthSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        var heightSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        var nameField = new JTextField("Board {x},{y}");
        nameField.setFont(CP437.getFont());
        nameField.setToolTipText("Board name template. {x} and {y} are replaced with the grid location of the board (1-based).");
        var hidePlayerChk = new JCheckBox("Erase player from boards", false);
        hidePlayerChk.setToolTipText("Erase the player from each board. This will place the player's stat in a corner of the board's border, keeping it off the board. You should place the player later if you want to actually use this board.");
        var openAtlasChk = new JCheckBox("Open board grid in Atlas view.", true);
        openAtlasChk.setToolTipText("After creating the board grid, load it in Atlas view.");
        var createButton = new JButton("Create boards");
        var cancelButton = new JButton("Cancel");

        var widthPanel = new JPanel(new BorderLayout());
        var heightPanel = new JPanel(new BorderLayout());
        var namePanel = new JPanel(new BorderLayout());
        var btnsPanel = new JPanel(new BorderLayout());
        widthPanel.add(new JLabel("Grid width: "), BorderLayout.WEST);
        heightPanel.add(new JLabel("Grid height: "), BorderLayout.WEST);
        namePanel.add(new JLabel("Name template: "), BorderLayout.WEST);
        widthPanel.add(widthSpinner, BorderLayout.EAST);
        heightPanel.add(heightSpinner, BorderLayout.EAST);
        namePanel.add(nameField, BorderLayout.EAST);
        btnsPanel.add(createButton, BorderLayout.WEST);
        btnsPanel.add(cancelButton, BorderLayout.EAST);
        cp.add(widthPanel);
        cp.add(heightPanel);
        cp.add(namePanel);
        cp.add(hidePlayerChk);
        cp.add(openAtlasChk);
        cp.add(btnsPanel);

        cancelButton.addActionListener(e -> {
            dlg.dispose();
        });
        createButton.addActionListener(e -> {
            int width = (int) widthSpinner.getValue();
            int height = (int) heightSpinner.getValue();
            String nameTemplate = nameField.getText();

            int startIdx = boards.size();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    String name = nameTemplate.replace("{x}", Integer.toString(x + 1));
                    name = name.replace("{y}", Integer.toString(y + 1));

                    Board newBoard = blankBoard(name);

                    int currentIdx = y * width + x + startIdx;

                    // Create connections
                    // North
                    if (y > 0) newBoard.setExit(0, currentIdx - width);
                    // South
                    if (y < height - 1) newBoard.setExit(1, currentIdx + width);
                    // West
                    if (x > 0) newBoard.setExit(2, currentIdx - 1);
                    // East
                    if (x < width - 1) newBoard.setExit(3, currentIdx + 1);

                    if (hidePlayerChk.isSelected()) {
                        erasePlayer(newBoard);
                    }

                    boards.add(newBoard);
                }
            }

            if (openAtlasChk.isSelected()) {
                changeBoard(startIdx);
                atlas();
            }
            dlg.dispose();
        });

        dlg.pack();
        dlg.setLocationRelativeTo(frame);
        dlg.setVisible(true);
    }

    private void operationErasePlayer() {
        int x = getBoardXOffset() + currentBoard.getStat(0).getX() - 1;
        int y = getBoardYOffset() + currentBoard.getStat(0).getY() - 1;

        erasePlayer(currentBoard);
        addRedraw(x, y, x, y);

        afterModification();
    }

    private void erasePlayer(Board board) {
        int cornerX = board.getWidth() + 1;
        int cornerY = 0;

        var stat0 = board.getStat(0);

        // If the player is already in the corner, do nothing
        if (stat0.getX() == cornerX && stat0.getY() == cornerY)
            return;

        var oldStat0x = stat0.getX() - 1;
        var oldStat0y = stat0.getY() - 1;

        // Replace player with under tile
        Tile under = new Tile(stat0.getUid(), stat0.getUco());
        stat0.setX(cornerX);
        stat0.setY(cornerY);

        board.setTile(oldStat0x, oldStat0y, under);

        /*
        // Fix uid/uco
        stat0.setUid(placingTile.getStats().get(0).getUid());
        stat0.setUco(placingTile.getStats().get(0).getUco());
        // Place the tile here, but without stat0
        placingTile.getStats().remove(0);
        addRedraw(x, y, x, y);
        board.setTileDirect(x % boardW, y % boardH, placingTile);
        // Then move stat0 to the cursor
        stat0.setX(x % boardW + 1);
        stat0.setY(y % boardH + 1);
        */
    }

    private void operationDeleteBoard() {
        new BoardManager(this, boards, currentBoardIdx);
    }

    private void operationChangeBoard() {
        new BoardSelector(this, boards, e -> {
            int newBoardIdx = Integer.parseInt(e.getActionCommand());
            changeBoard(newBoardIdx);
        });
    }

    private void operationCursorMove(int offX, int offY, boolean draw) {
        int newCursorX = Util.clamp(cursorX + offX, 0, width - 1);
        int newCursorY = Util.clamp(cursorY + offY, 0, height - 1);

        if (newCursorX != cursorX || newCursorY != cursorY) {
            if (!draw) {
                cursorX = newCursorX;
                cursorY = newCursorY;
            } else {
                int deltaX = offX == 0 ? 0 : offX / Math.abs(offX);
                int deltaY = offY == 0 ? 0 : offY / Math.abs(offY);
                var dirty = new HashSet<Board>();
                while (cursorX != newCursorX || cursorY != newCursorY) {
                    cursorX += deltaX;
                    cursorY += deltaY;

                    if (drawing) {
                        var board = putTileDeferred(cursorX, cursorY, getBufferTile(), PUT_DEFAULT);
                        if (board != null) dirty.add(board);
                    }
                }
                for (var board : dirty) {
                    board.finaliseStats();
                }
            }
            canvas.setCursor(cursorX, cursorY);

            if (drawing) {
                afterModification();
            } else {
                afterUpdate();
            }
        }
    }

    private void operationBufferGrab() {
        setBufferTile(getTileAt(cursorX, cursorY, true));
        afterUpdate();
    }

    private void operationBufferPut() {
        if (getTileAt(cursorX, cursorY, false).equals(getBufferTile())) {
            operationDelete();
            return;
        }
        putTileAt(cursorX, cursorY, getBufferTile(), PUT_DEFAULT);
        afterModification();
    }

    private void operationBufferSwapColour() {
        var tile = getBufferTile().clone();
        int oldCol = tile.getCol();
        int newCol = ((oldCol & 0x0F) << 4) | ((oldCol & 0xF0) >> 4);
        tile.setCol(newCol);
        setBufferTile(tile);
        afterUpdate();
    }

    private void mouseDraw() {
        var dirty = new HashSet<Board>();
        if (oldMousePosX == -1) {
            mousePlot(mouseX, mouseY, dirty);
        } else {
            int cx = -1, cy = -1;
            int dx = mousePosX - oldMousePosX;
            int dy = mousePosY - oldMousePosY;
            int dist = Math.max(Math.abs(dx), Math.abs(dy));
            if (dist == 0) return;
            var plotSet = new HashSet<ArrayList<Integer>>();
            //int cw = canvas.getCharW(), ch = canvas.getCharH();
            for (int i = 0; i <= dist; i++) {
                int x = dx * i / dist + oldMousePosX;
                int y = dy * i / dist + oldMousePosY;
                int ncx = canvas.toCharX(x);
                int ncy = canvas.toCharY(y);
                if (ncx != cx || ncy != cy) {
                    cx = ncx;
                    cy = ncy;
                    plotSet.add(pair(cx, cy));
                }
            }
            for (var plot : plotSet) {
                mousePlot(plot.get(0), plot.get(1), dirty);
            }
        }
        for (var board : dirty) {
            board.finaliseStats();
        }
        afterModification();
        canvas.setCursor(cursorX, cursorY);
    }

    private boolean mouseMove() {
        int x = mouseX;
        int y = mouseY;
        if (x >= 0 && y >= 0 && x < width && y < height) {
            cursorX = x;
            cursorY = y;
            canvas.setCursor(cursorX, cursorY);
            afterUpdate();
            return true;
        }
        return false;
    }

    private void mouseGrab() {
        if (mouseMove()) {
            setBufferTile(getTileAt(cursorX, cursorY, true));
            afterUpdate();
        }
    }

    private int mouseCharX(int x) {
        return canvas.toCharX(x);
    }
    private int mouseCharY(int y) {
        return canvas.toCharY(y);
    }

    private void mousePlot(int x, int y, HashSet<Board> dirty) {
        if (x >= 0 && y >= 0 && x < width && y < height) {
            cursorX = x;
            cursorY = y;
            canvas.setCursor(cursorX, cursorY);
            var board = putTileDeferred(cursorX, cursorY, getBufferTile(), PUT_DEFAULT);
            if (board != null) dirty.add(board);
        }
    }

    private void operationDelete() {
        Tile tile = getTileAt(cursorX, cursorY, false);
        Tile underTile = new Tile(0, 0);
        var tileStats = tile.getStats();
        if (tileStats.size() > 0) {
            int uid = tileStats.get(0).getUid();
            int uco = tileStats.get(0).getUco();
            underTile.setId(uid);
            underTile.setCol(uco);
        }
        putTileAt(cursorX, cursorY, underTile, PUT_DEFAULT);
        afterModification();
    }

    private void operationGrabAndModify(boolean grab, boolean advanced) {
        if (grab) { // Enter also finishes a copy block operation
            if (blockStartX != -1) {
                operationBlockEnd();
                return;
            }
            if (globalEditor.isBlockBuffer()) {
                blockPaste();
                return;
            }
            if (moveBlockW != 0) {
                blockFinishMove();
                return;
            }
        }
        var tile = getTileAt(cursorX, cursorY, false);
        var board = getBoardAt(cursorX, cursorY);
        int x = cursorX % boardW;
        int y = cursorY % boardH;
        if (tile != null && board != null) {
            openTileEditor(tile, board, x, y, resultTile -> {
                // Put this tile down, subject to the following:
                // - Any stat IDs on this tile that matches a stat ID on the destination tile go in in-place
                // - If there are stats on the destination tile that weren't replaced, delete them
                // - If there are stats on this tile that didn't go in, add them to the end

                setStats(board, cursorX / boardW * boardW, cursorY / boardH * boardH, x, y, resultTile.getStats());
                addRedraw(cursorX, cursorY, cursorX, cursorY);
                board.setTileRaw(x, y, resultTile.getId(), resultTile.getCol());
                if (grab) setBufferTile(getTileAt(cursorX, cursorY, true));

                afterModification();
            }, advanced);
        }
    }

    private void operationModifyBuffer(boolean advanced) {
        openTileEditor(getBufferTile(), currentBoard, -1, -1, resultTile -> {
            setBufferTile(resultTile);
            afterUpdate();
        }, advanced);
    }

    private void operationStatList() {
        Board board = getBoardAt(cursorX, cursorY);
        int boardX = cursorX / boardW * boardW;
        int boardY = cursorY / boardH * boardH;
        if (board == null) return;
        new StatSelector(this, board, e -> {
            int val = StatSelector.getStatIdx(e.getActionCommand());
            int option = StatSelector.getOption(e.getActionCommand());
            var stat = board.getStat(val);
            int x = stat.getX() - 1;
            int y = stat.getY() - 1;

            openTileEditor(board.getStatsAt(x, y), board, x, y, resultTile -> {
                setStats(board, boardX, boardY, x, y, resultTile.getStats());
                if (resultTile.getId() != -1) {
                    addRedraw(x + boardX, y + boardY, x + boardX, y + boardY);
                    board.setTileRaw(x, y, resultTile.getId(), resultTile.getCol());
                }
                ((StatSelector)(e.getSource())).dataChanged();
                afterModification();
            }, option == 1, val);
        }, new String[]{"Modify", "Modify (advanced)"});
    }

    private void openTileEditor(Tile tile, Board board, int x, int y, TileEditorCallback callback, boolean advanced)
    {
        new TileEditor(this, board, tile, null, callback, x, y, advanced, -1, false);
    }
    private void openTileEditorExempt(Tile tile, Board board, int x, int y, TileEditorCallback callback, boolean advanced)
    {
        new TileEditor(this, board, tile, null, callback, x, y, advanced, -1, true);
    }

    private void openTileEditor(java.util.List<Stat> stats, Board board, int x, int y, TileEditorCallback callback, boolean advanced, int selected)
    {
        new TileEditor(this, board, null, stats, callback, x, y, advanced, selected, false);
    }

    private void setStats(Board board, int bx, int by, int x, int y, java.util.List<Stat> stats) {
        // If any stats move, invalidate the entire board
        boolean invalidateAll = false;

        var destStats = board.getStatsAt(x, y);
        ArrayList<Integer> statsToDelete = new ArrayList<>();
        boolean[] statsAdded = new boolean[stats.size()];
        boolean mustFinalise = false;

        for (var destStat : destStats) {
            Stat replacementStat = null;
            boolean replacementStatMoved = false;
            for (int i = 0; i < stats.size(); i++) {
                var stat = stats.get(i);
                if (stat.getStatId() == destStat.getStatId() && stat.getStatId() != -1) {
                    if (stat.getX() != destStat.getX() || stat.getY() != destStat.getY()) {
                        replacementStatMoved = true;
                    }
                    replacementStat = stat;
                    statsAdded[i] = true;
                    break;
                }
            }
            if (replacementStat != null) {
                if (board.directReplaceStat(destStat.getStatId(), replacementStat)) {
                    if (replacementStatMoved) invalidateAll = true;
                    mustFinalise = true;
                }
            } else {
                statsToDelete.add(destStat.getStatId());
            }
        }

        if (board.directDeleteStats(statsToDelete)) {
            mustFinalise = true;
        }

        for (int i = 0; i < stats.size(); i++) {
            if (!statsAdded[i]) {
                var stat = stats.get(i);
                if (stat.getX() != x + 1 || stat.getY() != y + 1) invalidateAll = true;
                board.directAddStat(stat);
                mustFinalise = true;
            }
        }



        if (mustFinalise) {
            board.finaliseStats();
        }
        if (invalidateAll) {
            addRedraw(bx + 1, by + 1, bx + boardW - 2, by + boardH - 2);
        }
    }

    private Board getBoardAt(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            throw new IndexOutOfBoundsException("Attempted to getBoardAt() coordinate off map");
        }
        int gridX = x / boardW;
        int gridY = y / boardH;
        int boardIdx = grid[gridY][gridX];
        if (boardIdx == -1) return null;
        return boards.get(boardIdx);
    }

    /**
     *
     * @param putMode PUT_DEFAULT or PUT_PUSH_DOWN or PUT_REPLACE_BOTH
     */
    private void putTileAt(int x, int y, Tile tile, int putMode) {
        var board = putTileDeferred(x, y, tile, putMode);
        if (board != null) board.finaliseStats();
    }

    /**
     *
     * @param putMode PUT_DEFAULT or PUT_PUSH_DOWN or PUT_REPLACE_BOTH
     */
    private Board putTileDeferred(int x, int y, Tile tile, int putMode) {
        var board = getBoardAt(x, y);
        var currentTile = getTileAt(x, y, false);
        if (board != null && currentTile != null && tile != null) {
            Tile placingTile = tile.clone();
            // First, we will not allow putTileAt to erase stat 0
            var currentTileStats = currentTile.getStats();
            if (currentTileStats.size() > 0) {
                if (currentTileStats.get(0).getStatId() == 0) {
                    return board;
                }
            }

            if (putMode == PUT_DEFAULT || putMode == PUT_PUSH_DOWN) {
                var tileStats = placingTile.getStats();
                // Only check if we have exactly 1 stat
                if (tileStats.size() == 1) {
                    var tileStat = tileStats.get(0);

                    if (putMode == PUT_DEFAULT) {
                        // If the tile currently there is floor, we will push it down
                        if (ZType.isFloor(worldData.isSuperZZT(), currentTile)) {
                            putMode = PUT_PUSH_DOWN;
                        } else {
                            // Not a floor, so does it have one stat? If so, still push down (we will use its uid/uco)
                            if (currentTileStats.size() == 1) {
                                putMode = PUT_PUSH_DOWN;
                                // replace currentTile with what was under it
                                currentTile = new Tile(currentTileStats.get(0).getUid(), currentTileStats.get(0).getUco());
                            }
                        }
                    }
                    if (putMode == PUT_PUSH_DOWN) {
                        if (placingTile.getCol() < 16) {
                            placingTile.setCol((currentTile.getCol() & 0x70) | placingTile.getCol());
                        }
                        tileStat.setUid(currentTile.getId());
                        tileStat.setUco(currentTile.getCol());
                    }
                }

            }
            // Are we placing stat 0?
            if (placingTile.getStats().size() > 0) {
                if (placingTile.getStats().get(0).getStatId() == 0 ||
                        placingTile.getStats().get(0).isPlayer()) {
                    // Find the stat 0 on this board
                    var stat0 = board.getStat(0);
                    var oldStat0x = stat0.getX() - 1;
                    var oldStat0y = stat0.getY() - 1;
                    // If stat 0 isn't on the board, nevermind!
                    if (oldStat0x >= 0 && oldStat0x < boardW && oldStat0y >= 0 && oldStat0y < boardH) {
                        // See what other stats are there
                        var oldStat0TileStats = board.getStatsAt(oldStat0x, oldStat0y);
                        if (oldStat0TileStats.size() == 1) {
                            // Once we move stat0 there will be no other stats, so erase this
                            board.setTileRaw(oldStat0x, oldStat0y, stat0.getUid(), stat0.getUco());
                            int bx = x / boardW * boardW;
                            int by = y / boardH * boardH;
                            addRedraw(oldStat0x + bx, oldStat0y + by, oldStat0x + bx, oldStat0y + by);
                        } // Otherwise there are stats left, so leave the tile alone
                    }
                    // Fix uid/uco
                    stat0.setUid(placingTile.getStats().get(0).getUid());
                    stat0.setUco(placingTile.getStats().get(0).getUco());
                    // Place the tile here, but without stat0
                    placingTile.getStats().remove(0);
                    addRedraw(x, y, x, y);
                    board.setTileDirect(x % boardW, y % boardH, placingTile);
                    // Then move stat0 to the cursor
                    stat0.setX(x % boardW + 1);
                    stat0.setY(y % boardH + 1);

                    return board;
                }
            }
            addRedraw(x, y, x, y);
            board.setTileDirect(x % boardW, y % boardH, placingTile);
        }
        return board;
    }

    private void updateCurrentBoard() {
        cursorX = Util.clamp(cursorX, 0, width - 1);
        cursorY = Util.clamp(cursorY, 0, height - 1);
        int gridX = cursorX / boardW;
        int gridY = cursorY / boardH;
        setCurrentBoard(grid[gridY][gridX]);
        String worldName = path == null ? "new world" : path.getName();
        String boardInfo;
        if (currentBoard != null) {
            boardInfo = boardInfo = "Board #" + currentBoardIdx + " :: " + CP437.toUnicode(currentBoard.getName());
        } else {
            boardInfo = "(no board)";
        }
        frame.setTitle("zedit2 [" + worldName + "] :: " + boardInfo + (isDirty() ? "*" : ""));
    }
    private Tile getTileAt(int x, int y, boolean copy) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            throw new IndexOutOfBoundsException("Attempted to read coordinate off map");
        }
        int boardX = x % boardW;
        int boardY = y % boardH;
        var board = getBoardAt(x, y);
        if (board == null) {
            return null;
        } else {
            return board.getTile(boardX, boardY, copy);
        }
    }
    private void swapTile(int xFrom, int yFrom, int xTo, int yTo)
    {
        // Check if these are on the same board
        var fromBoard = getBoardAt(xFrom, yFrom);
        var board = getBoardAt(xTo, yTo);
        if (fromBoard == board) {
            // Move stats
            int bxFrom = xFrom % boardW;
            int byFrom = yFrom % boardH;
            int bxTo = xTo & boardW;
            int byTo = yTo & boardH;
            var fromTileCol = board.getTileCol(bxFrom, byFrom);
            var fromTileId = board.getTileId(bxFrom, byFrom);
            var toTileCol = board.getTileCol(bxFrom, byFrom);
            var toTileId = board.getTileId(bxFrom, byFrom);
            board.setTileRaw(bxTo, byTo, fromTileId, fromTileCol);
            board.setTileRaw(bxFrom, byFrom, toTileId, toTileCol);
            for (int i = 0; i < board.getStatCount(); i++) {
                var stat = board.getStat(i);
                if (stat.getX() == bxTo + 1 && stat.getY() == byTo + 1) {
                    stat.setX(bxFrom + 1);
                    stat.setY(byFrom + 1);
                } else if (stat.getX() == bxFrom + 1 && stat.getY() == byFrom + 1) {
                    stat.setX(bxTo + 1);
                    stat.setY(byTo + 1);
                }
            }
        } else {
            var fromTile = getTileAt(xFrom, yFrom, true);
            var toTile = getTileAt(xTo, yTo, true);
            putTileAt(xTo, yTo, fromTile, PUT_REPLACE_BOTH);
            putTileAt(xFrom, yFrom, toTile, PUT_REPLACE_BOTH);
        }
    }

    private void afterModification() {
        drawBoard();
        undoDirty = true;
        afterUpdate();
    }

    private void scrollToCursor() {
        int w, h, x, y;
        if (centreView) {
            canvas.revalidate();
            var visibleRect = canvas.getVisibleRect();
            w = Util.clamp(visibleRect.width, 0, canvas.getCharW(width));
            h = Util.clamp(visibleRect.height, 0, canvas.getCharH(height));
            int charW = canvas.getCharW(1);
            int charH = canvas.getCharH(1);
            x = Math.max(0, canvas.getCharW(cursorX) + (charW - w) / 2);
            y = Math.max(0, canvas.getCharH(cursorY) + (charH - h) / 2);
            centreView = false;
        } else {
            w = canvas.getCharW(1);
            h = canvas.getCharH(1);
            x = canvas.getCharW(cursorX);
            y = canvas.getCharH(cursorY);

            // Expand this slightly
            final int EXPAND_X = 4;
            final int EXPAND_Y = 4;
            x -= canvas.getCharW(EXPAND_X);
            y -= canvas.getCharH(EXPAND_Y);
            w += canvas.getCharW(EXPAND_X * 2);
            h += canvas.getCharH(EXPAND_Y * 2);
        }
        var rect = new Rectangle(x, y, w, h);
        canvas.scrollRectToVisible(rect);
    }

    private void afterChangeShowing() {
        invalidateCache();
        afterModification();
    }

    private void afterUpdate() {
        GlobalEditor.updateTimestamp();
        if (undoDirty && mouseState != 1 && !fancyFillDialog) {
            addUndo();
        }
        validGracePeriod = false;

        updateCurrentBoard();
        scrollToCursor();
        int boardX = cursorX % boardW;
        int boardY = cursorY % boardH;

        String s = "";
        if (currentBoard != null) {
            int boardNameFieldLen = 22;
            if (currentBoardIdx < 100) boardNameFieldLen++;
            if (currentBoardIdx < 10) boardNameFieldLen++;
            var boardName = CP437.toUnicode(currentBoard.getName());
            boardNameFieldLen = Math.min(boardNameFieldLen, boardName.length());
            String limitedName = boardName.substring(0, boardNameFieldLen);

            s = String.format("Stats: %3d/%d   X/Y: %d,%d\n" +
                            "%d: %s\nB.Mem: %5d  ",
                    currentBoard.getStatCount() - 1, !worldData.isSuperZZT() ? 150 : 128,
                    boardX + 1, boardY + 1,
                    currentBoardIdx, limitedName, currentBoard.getCurrentSize());
        }

        s += String.format("W.Mem: %6d", getWorldSize());

        infoBox.setText(s);

        updateEditingMode();

        bufferPane.removeAll();
        bufferPaneContents = bufferPane;
        blinkingImageIcons.clear();
        var cursorTile = getTileAt(cursorX, cursorY, false);
        addTileInfoDisplay("Cursor", cursorTile);
        addTileInfoDisplay("Buffer", getBufferTile());
        bufferPane.repaint();
    }

    private void updateEditingMode() {
        var enter = Util.keyStrokeString(Util.getKeyStroke(globalEditor, "Enter"));;
        if (textEntry) editingModePane.display(Color.YELLOW, "Type to place text");
        else if (blockStartX != -1) editingModePane.display(new Color[]{new Color(0, 127, 255), Color.CYAN}, enter + " on other corner");
        else if (drawing) editingModePane.display(Color.GREEN, "Drawing");
        else if (globalEditor.isBlockBuffer() || moveBlockW != 0) editingModePane.display(new Color[]{Color.ORANGE, Color.RED}, enter + " to place block");
        else if (currentlyShowing == SHOW_STATS) editingModePane.display(new Color[]{Color.YELLOW, Color.RED}, "Showing Stats");
        else if (currentlyShowing == SHOW_OBJECTS) editingModePane.display(new Color[]{Color.GREEN, Color.BLUE}, "Showing Objects");
        else if (currentlyShowing == SHOW_INVISIBLES) editingModePane.display(new Color[]{Color.CYAN, Color.MAGENTA}, "Showing Invisibles");
        else if (currentlyShowing == SHOW_EMPTIES) editingModePane.display(new Color[]{Color.LIGHT_GRAY, Color.GRAY}, "Showing Empties");
        else if (currentlyShowing == SHOW_FAKES) editingModePane.display(new Color[]{Color.LIGHT_GRAY, Color.GRAY}, "Showing Fakes");
        else if (currentlyShowing == SHOW_EMPTEXTS) editingModePane.display(new Color[]{Color.LIGHT_GRAY, Color.GRAY}, "Showing Empties as Text");
        else editingModePane.display(Color.BLUE, "Editing");
    }

    private JLabel createLabel(Tile cursorTile) {
        boolean szzt = worldData.isSuperZZT();
        int chr = ZType.getChar(szzt, cursorTile);
        int col = ZType.getColour(szzt, cursorTile);
        String name = ZType.getName(szzt, cursorTile.getId());
        char chBg = (char)((cursorTile.getCol()) | (32 << 8));
        char chFg = (char)((cursorTile.getCol()) | (254 << 8));
        String pattern = "@ " + chBg + chFg + chBg;
        var imgBlinkOff = canvas.extractCharImage(chr, col, 2, 2, false, pattern);
        var imgBlinkOn = canvas.extractCharImage(chr, col, 2, 2, true, pattern);
        var tileLabelIcon = new BlinkingImageIcon(imgBlinkOff, imgBlinkOn);

        var tileLabel = new JLabel();
        tileLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        tileLabel.setIcon(tileLabelIcon);
        tileLabel.setText(name);
        blinkingImageIcons.add(tileLabelIcon);

        return tileLabel;
    }

    private void addTileInfoDisplay(String title, Tile cursorTile) {
        if (cursorTile == null) return;
        var tileInfoPanel = new JPanel(new BorderLayout());
        var tileLabel = createLabel(cursorTile);

        //tileInfoPanel.setText("<html>Test</html>");
        //tileInfoPanel.add(label);
        tileInfoPanel.setBorder(BorderFactory.createTitledBorder(title));
        tileInfoPanel.add(tileLabel, BorderLayout.NORTH);
        if (!cursorTile.getStats().isEmpty()) {
            var tileInfoBox = new JLabel();
            var tileInfo = new StringBuilder();
            tileInfo.append("<html>");

            boolean firstStat = true;

            for (var stat : cursorTile.getStats()) {
                if (!firstStat) {
                    tileInfo.append("<hr></hr>");
                }
                firstStat = false;

                tileInfo.append("<table>");
                tileInfo.append("<tr>");
                int statId = stat.getStatId();

                tileInfo.append(String.format("<th align=\"left\">%s</th><td>%s</td>", "Stat ID:", statId != -1 ? statId : ""));
                tileInfo.append(String.format("<th align=\"left\">%s</th><td>%d</td>", "Cycle:", stat.getCycle()));
                tileInfo.append("</tr><tr>");
                tileInfo.append(String.format("<th align=\"left\">%s</th><td>%d</td>", "X-Step:", stat.getStepX()));
                tileInfo.append(String.format("<th align=\"left\">%s</th><td>%d</td>", "Y-Step:", stat.getStepY()));
                tileInfo.append("</tr><tr>");
                tileInfo.append(String.format("<th align=\"left\">%s</th><td>%d</td>", "Param 1:", stat.getP1()));
                tileInfo.append(String.format("<th align=\"left\">%s</th><td>%d</td>", "Follower:", stat.getFollower()));
                tileInfo.append("</tr><tr>");
                tileInfo.append(String.format("<th align=\"left\">%s</th><td>%d</td>", "Param 2:", stat.getP2()));
                tileInfo.append(String.format("<th align=\"left\">%s</th><td>%d</td>", "Leader:", stat.getLeader()));
                tileInfo.append("</tr><tr>");
                tileInfo.append(String.format("<th align=\"left\">%s</th><td>%d</td>", "Param 3:", stat.getP3()));
                tileInfo.append(String.format("<th align=\"left\">%s</th><td>%d</td>", "Instr. Ptr:", stat.getIp()));
                tileInfo.append("</tr><tr>");
                // Code
                int codeLen = stat.getCodeLength();
                if (codeLen != 0) {
                    if (codeLen >= 0) {
                        tileInfo.append(String.format("<th colspan=\"2\" align=\"left\">%s</th><td colspan=\"2\">%d</td>", "Code length:", codeLen));
                    } else {
                        int boundTo = -codeLen;
                        String appendMessage = "";
                        if (boundTo < currentBoard.getStatCount()) {
                            var boundToStat = currentBoard.getStat(boundTo);
                            appendMessage = " @ " + boundToStat.getX() + "," + boundToStat.getY();
                        }
                        tileInfo.append(String.format("<th align=\"left\">%s</th><td colspan=\"3\">%s</td>", "Bound to:", "#" + boundTo + appendMessage));
                    }
                    tileInfo.append("</tr><tr>");
                }
                tileInfo.append("<th align=\"left\">Under:</th>");
                String bgcol = canvas.htmlColour(stat.getUco() / 16);
                String fgcol = canvas.htmlColour(stat.getUco() % 16);
                tileInfo.append(String.format("<td align=\"left\"><span bgcolor=\"%s\" color=\"%s\">&nbsp;&nbsp;■&nbsp;&nbsp;</span></td>", bgcol, fgcol));

                tileInfo.append(String.format("<td align=\"left\" colspan=\"2\">%s</td>", ZType.getName(worldData.isSuperZZT(), stat.getUid())));
                tileInfo.append("</tr>");
                tileInfo.append("</table>");
            }

            tileInfo.append("</html>");
            tileInfoBox.setHorizontalAlignment(SwingConstants.LEFT);
            tileInfoBox.setText(tileInfo.toString());
            tileInfoPanel.add(tileInfoBox, BorderLayout.CENTER);
        }

        //int w = bufferPane.getWidth() - 16;
        //tileInfoPanel.setPreferredSize(new Dimension(w, tileInfoPanel.getPreferredSize().height));
        bufferPaneContents.add(tileInfoPanel, BorderLayout.NORTH);
        var childPanel = new JPanel(new BorderLayout());
        bufferPaneContents.add(childPanel, BorderLayout.CENTER);
        bufferPaneContents = childPanel;
    }

    private int getWorldSize() {
        int size = worldData.boardListOffset();
        for (var board : boards) {
            size += board.getCurrentSize();
        }
        return size;
    }

    public int getBoardIdx() {
        return currentBoardIdx;
    }

    public int getBoardXOffset() {
        return cursorX / boardW * boardW;
    }

    public int getBoardYOffset() {
        return cursorY / boardH * boardH;
    }

    public void replaceBoardList(ArrayList<Board> newBoardList) {
        atlases.clear();
        currentAtlas = null;
        boards = newBoardList;
    }

    public void setWorldData(WorldData newWorldData) {
        worldData = newWorldData;
    }

    public void mouseMotion(MouseEvent e, int heldDown) {
        mouseState = heldDown;
        mousePosX = e.getX();
        mousePosY = e.getY();
        mouseX = mouseCharX(mousePosX);
        mouseY = mouseCharY(mousePosY);

        // Translate into local space
        mouseScreenX = e.getXOnScreen() - frame.getLocationOnScreen().x;
        mouseScreenY = e.getYOnScreen() - frame.getLocationOnScreen().y;
        if (heldDown == 1) {
            mouseDraw();
            oldMousePosX = mousePosX;
            oldMousePosY = mousePosY;
        } else {
            if (heldDown == 2) mouseGrab();
            else if (heldDown == 3) mouseMove();
            oldMousePosX = -1;
            oldMousePosY = -1;
        }

        if (undoDirty && mouseState != 1 && !fancyFillDialog) {
            addUndo();
        }
    }

    public GlobalEditor getGlobalEditor() {
        return globalEditor;
    }

    private void removeAtlas() {
        int x = cursorX / boardW;
        int y = cursorY / boardH;
        int changeTo = grid[y][x];
        if (changeTo == -1) {
            for (var row : grid) {
                for (var brd : row) {
                    if (brd != -1) {
                        changeTo = brd;
                        break;
                    }
                }
                if (changeTo != -1) break;
            }
        }
        Atlas atlas = atlases.get(changeTo);
        var removeThese = new ArrayList<Integer>();
        for (var i : atlases.keySet()) {
            if (atlases.get(i) == atlas)
                removeThese.add(i);
        }
        for (var i : removeThese) {
            atlases.remove(i);
        }
        currentAtlas = null;
        changeBoard(changeTo);
    }

    private void atlas() {
        if (currentBoard == null) return;
        if (currentAtlas != null) {
            removeAtlas();
            return;
        }

        var boardsSeen = new HashSet<Board>();
        boardsSeen.add(currentBoard);
        var map = new HashMap<ArrayList<Integer>, Board>();
        map.put(pair(0, 0), currentBoard);
        var stack = new ArrayDeque<Object>();
        stack.add(0);
        stack.add(0);
        stack.add(currentBoard);
        for (;;) {
            // Board exits go NORTH, SOUTH, WEST, EAST
            final int[][] dir = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
            if (stack.isEmpty()) break;
            int x = (int) stack.pop();
            int y = (int) stack.pop();
            Board board = (Board) stack.pop();

            for (int exit = 0; exit < 4; exit++) {
                int dest = board.getExit(exit);
                if (dest > 0 && dest < boards.size()) {
                    var destBoard = boards.get(dest);
                    if (!boardsSeen.contains(destBoard)) {
                        int dx = x + dir[exit][0];
                        int dy = y + dir[exit][1];
                        if (!map.containsKey(pair(dx, dy))) {
                            map.put(pair(dx, dy), destBoard);
                            boardsSeen.add(destBoard);
                            stack.add(dx);
                            stack.add(dy);
                            stack.add(destBoard);
                        }
                    }
                }
            }
        }

        int minX = 0, minY = 0, maxX = 0, maxY = 0;
        for (var loc : map.keySet()) {
            int x = loc.get(0);
            int y = loc.get(1);
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            //System.out.printf("%d,%d: %s\n", loc.get(0), loc.get(1), CP437.toUnicode(map.get(loc).getName()));
        }
        gridW = maxX - minX + 1;
        gridH = maxY - minY + 1;
        grid = new int[gridH][gridW];
        var boardIdLookup = new HashMap<Board, Integer>();
        for (int i = 0; i < boards.size(); i++) {
            boardIdLookup.put(boards.get(i), i);
        }
        for (int y = 0; y < gridH; y++) {
            Arrays.fill(grid[y], -1);
        }
        var atlas = new Atlas(gridW, gridH, grid);
        currentAtlas = atlas;
        for (var loc : map.keySet()) {
            int x = loc.get(0) - minX;
            int y = loc.get(1) - minY;
            var board = map.get(loc);
            int boardIdx = boardIdLookup.get(board);
            grid[y][x] = boardIdx;
            atlases.put(boardIdx, atlas);
        }
        cursorX = (cursorX % boardW) + boardW * -minX;
        cursorY = (cursorY % boardH) + boardH * -minY;
        width = boardW * gridW;
        height = boardH * gridH;
        canvas.setCursor(cursorX, cursorY);
        invalidateCache();
        afterModification();
        canvas.revalidate();
        var rect = new Rectangle(canvas.getCharW(-minX * boardW),
                canvas.getCharH(-minY * boardH),
                canvas.getCharW(boardW),
                canvas.getCharH(boardH));
        canvas.scrollRectToVisible(rect);
        resetUndoList();
    }

    private void atlasRemoveBoard() {
        if (currentBoard == null) return;
        if (currentAtlas == null) return;
        atlases.remove(grid[cursorY / boardH][cursorX / boardW]);
        grid[cursorY / boardH][cursorX / boardW] = -1;
        boolean notEmpty = false;
        for (int y = 0; y < gridH; y++) {
            for (int x = 0; x < gridW; x++) {
                if (grid[y][x] != -1) notEmpty = true;
            }
        }
        if (!notEmpty) {
            removeAtlas();
            return;
        }
        invalidateCache();
        afterModification();
        canvas.revalidate();
    }

    @Override
    public void keyTyped(KeyEvent e) {
        if (textEntry) {
            char ch = e.getKeyChar();
            if (ch < 256) {
                if (ch == '\n') {
                    cursorX = textEntryX;
                    cursorY = Util.clamp(cursorY + 1, 0, height - 1);
                } else {
                    if (ch == 8) { // bksp
                        cursorX = Util.clamp(cursorX - 1, 0, width - 1);
                    }
                    int col = ch;
                    if (ch == 8 || ch == 127) { // bksp or del
                        col = ' ';
                    }

                    int id = ZType.getTextId(worldData.isSuperZZT(), getTileColour(getBufferTile()));
                    Tile textTile = new Tile(id, col);
                    putTileAt(cursorX, cursorY, textTile, PUT_DEFAULT);

                    if (ch != 8 && ch != 127) { // not bksp or del
                        cursorX = Util.clamp(cursorX + 1, 0, width - 1);
                    }

                    afterModification();
                }
                canvas.setCursor(cursorX, cursorY);
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) { }
    @Override
    public void keyReleased(KeyEvent e) { }

    private void disableAlt() {
        // From https://stackoverflow.com/a/3994002
        frame.addFocusListener(new FocusListener() {
            private final KeyEventDispatcher altDisabler = new KeyEventDispatcher() {
                @Override
                public boolean dispatchKeyEvent(KeyEvent e) {
                    return e.getKeyCode() == 18;
                }
            };

            @Override
            public void focusGained(FocusEvent e) {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(altDisabler);
            }

            @Override
            public void focusLost(FocusEvent e) {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(altDisabler);
            }
        });
    }

    public Component getFrameForRelativePositioning() {
        return canvasScrollPane;
    }
    public JFrame getFrame() {
        return frame;
    }

    public void wheelUp(MouseWheelEvent e) {
        operationZoomIn(true);
        e.consume();
    }

    public void wheelDown(MouseWheelEvent e) {
        operationZoomOut(true);
        e.consume();
    }

    public void removeBufferManager() {
        currentBufferManager = null;
    }

    @Override
    public void windowGainedFocus(WindowEvent e) {
        if (currentBufferManager != null)
            currentBufferManager.setAlwaysOnTop(true);
    }

    @Override
    public void windowLostFocus(WindowEvent e) {
        if (currentBufferManager != null)
            currentBufferManager.setAlwaysOnTop(false);
    }

    public void refreshKeymapping() {
        addKeybinds(frame.getLayeredPane());
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        popupOpen = true;
    }

    @Override
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        popupOpen = false;
        frame.requestFocusInWindow();
    }

    @Override
    public void popupMenuCanceled(PopupMenuEvent e) {
        popupOpen = false;
        frame.requestFocusInWindow();
    }

    @Override
    public void menuSelected(MenuEvent e) {
        popupOpen = true;
    }

    @Override
    public void menuDeselected(MenuEvent e) {
        popupOpen = false;
        frame.requestFocusInWindow();
    }

    @Override
    public void menuCanceled(MenuEvent e) {
        popupOpen = false;
        frame.requestFocusInWindow();
    }
}
