package zedit2;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import static zedit2.Util.pair;

public class FancyFill extends JDialog {
    private final Random rng = new Random();
    private final JLabel vertLabel;
    private final JLabel horizLabel;
    private final JLabel angleLabel;
    private final JLabel repeatLabel;
    private final JLabel xsLabel;
    private final JLabel ysLabel;
    private final JLabel diffLabel;
    private final JLabel tsengLabel;
    private final JLabel invertLabel;
    private final JSpinner horizInput;
    private final JSpinner vertInput;
    private final JSpinner angleInput;
    private final JSpinner repeatInput;
    private final JSpinner xsInput;
    private final JSpinner ysInput;
    private final JSpinner diffInput;
    private final JCheckBox invertChk;
    private final JSpinner tsengInput;
    private final JSpinner sliderStart;
    private final JSpinner sliderEnd;
    private final JComboBox<String> gradientCombo;
    private final JRadioButton btnLinear;
    private final JRadioButton btnBox;
    private final JRadioButton btnRadial;
    private final JRadioButton btnConic;
    private final JRadioButton btnSlime;
    private final byte[][] filled;
    private final ActionListener listener;
    private WorldEditor editor;

    private int[] xPositions;
    private int[] yPositions;
    private Tile[] tileFills;
    private Tile[] gradientTiles;
    private int minX, minY, maxX, maxY;

    private int fillMode;

    private static final int LINEAR = 0;
    private static final int BOX = 1;
    private static final int RADIAL = 2;
    private static final int CONIC = 3;
    private static final int SLIME = 4;
    private double linearMin;
    private double linearMax;
    private String closeCmd;
    private double tseng = 0.0;
    private double tsengMatr[][] = null;

    private boolean tsengAvail;

    public FancyFill(WorldEditor editor, ActionListener listener, byte[][] filled) {
        super(editor.getFrame(), "Gradient Fill");
        tsengCheck();

        //gradRev();
        Util.addEscClose(this, getRootPane());

        this.editor = editor;
        this.filled = filled;
        this.listener = listener;
        genList(filled);
        closeCmd = "undo";
        setModalityType(JDialog.ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        var cp = getContentPane();
        cp.setLayout(new BorderLayout());
        var gradientStyleBox = new JPanel(new GridLayout(1, 0));
        gradientStyleBox.setBorder(BorderFactory.createTitledBorder("Gradient style"));
        btnLinear = new JRadioButton("Linear");
        btnBox = new JRadioButton("Box");
        btnRadial = new JRadioButton("Radial");
        btnConic = new JRadioButton("Conical");
        btnSlime = new JRadioButton("Slime");
        ButtonGroup styleGroup = new ButtonGroup();
        styleGroup.add(btnLinear);
        styleGroup.add(btnBox);
        styleGroup.add(btnRadial);
        styleGroup.add(btnConic);
        styleGroup.add(btnSlime);
        gradientStyleBox.add(btnLinear);
        gradientStyleBox.add(btnBox);
        gradientStyleBox.add(btnRadial);
        gradientStyleBox.add(btnConic);
        gradientStyleBox.add(btnSlime);

        btnLinear.setSelected(true);
        cp.add(gradientStyleBox, BorderLayout.NORTH);

        var gradientBox = new JPanel(new BorderLayout());
        gradientBox.setBorder(BorderFactory.createTitledBorder("Gradient"));
        var comboBoxRenderer = new FancyFillRenderer(editor);
        gradientCombo = new JComboBox<String>();

        var ge = editor.getGlobalEditor();
        var preSelectedKey = getPreselectKey();
        int preSelectedIdx = -1;
        String preSelectedGrad = ge.getString(preSelectedKey);

        var grads = new ArrayList<String>();
        for (int i = 0;; i++) {
            var key = String.format(editor.prefix() + "GRAD_%d", i);
            if (ge.isKey(key)) {
                var grad = ge.getString(key);
                if (preSelectedGrad != null && preSelectedGrad.equals(grad)) {
                    preSelectedIdx = grads.size();
                }
                grads.add(grad);
            } else {
                break;
            }
        }
        int bufMax = ge.getInt(editor.prefix()+"BUF_MAX", 0);
        for (int i = 0; i <= bufMax; i++) {
            var encodedBuffer = ge.getString(String.format("%sBUF_%d", editor.prefix(), i));
            if (encodedBuffer == null) continue;
            var clip = Clip.decode(encodedBuffer);
            if (clip.getH() != 1) continue;
            if (clip.getW() == 1) continue;
            grads.add(encodedBuffer);
        }

        if (preSelectedIdx == -1) {
            if (preSelectedGrad != null) {
                gradientCombo.addItem(preSelectedGrad);
            }
            preSelectedIdx = 0;
        }
        for (var grad : grads) {
            gradientCombo.addItem(grad);
        }
        gradientCombo.setSelectedIndex(preSelectedIdx);

        gradientCombo.setRenderer(comboBoxRenderer);
        gradientBox.add(gradientCombo);

        gradientCombo.setToolTipText("Which gradient pattern to use. # x 1 blocks in your buffer can also be used.");

        var middleLeftPanel = new JPanel(new BorderLayout());
        middleLeftPanel.add(gradientBox, BorderLayout.NORTH);

        var gradOptionsLeft = new JPanel(new GridLayout(1, 2));
        sliderStart = new JSpinner(new SpinnerNumberModel(0.0, -10, 10, 0.01));
        sliderEnd = new JSpinner(new SpinnerNumberModel(1.0, -10, 10, 0.01));
        sliderStart.setPreferredSize(sliderStart.getMinimumSize());
        sliderEnd.setPreferredSize(sliderEnd.getMinimumSize());
        sliderStart.getModel().setValue(0.0);
        sliderEnd.getModel().setValue(1.0);
        sliderStart.setToolTipText("Gradient start point");
        sliderEnd.setToolTipText("Gradient finish point");
        var sliderStartBox = new JPanel(new BorderLayout());
        var sliderEndBox = new JPanel(new BorderLayout());
        sliderStartBox.add(sliderStart, BorderLayout.CENTER);
        sliderEndBox.add(sliderEnd, BorderLayout.CENTER);
        sliderStartBox.setBorder(BorderFactory.createTitledBorder("Start"));
        sliderEndBox.setBorder(BorderFactory.createTitledBorder("Finish"));
        gradOptionsLeft.add(sliderStartBox);
        gradOptionsLeft.add(sliderEndBox);

        sliderStart.setToolTipText("How far into the gradient to start (negative expands the space at the start)");
        sliderEnd.setToolTipText("How far into the gradient to finish (>1 expands the space at the end)");

        middleLeftPanel.add(gradOptionsLeft, BorderLayout.SOUTH);

        cp.add(middleLeftPanel, BorderLayout.WEST);

        var gradOptionsRight = new JPanel(new GridLayout(0, 4, 4, 4));
        horizLabel = new JLabel("Horizontal:");
        vertLabel = new JLabel("Vertical:");
        angleLabel = new JLabel("Angle:");
        repeatLabel = new JLabel("Repeats:");
        xsLabel = new JLabel("X-Weight:");
        ysLabel = new JLabel("Y-Weight:");
        diffLabel = new JLabel("Diffusion:");
        tsengLabel = new JLabel("Tseng:");
        invertLabel = new JLabel("Invert:");
        horizLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        vertLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        angleLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        repeatLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        xsLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        ysLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        diffLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        tsengLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        invertLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        horizInput = new JSpinner(new SpinnerNumberModel(0.5, 0.0, 1.0, 0.01));
        vertInput = new JSpinner(new SpinnerNumberModel(0.5, 0.0, 1.0, 0.01));
        angleInput = new JSpinner(new SpinnerNumberModel(0, 0, 360, 1));
        repeatInput = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
        xsInput = new JSpinner(new SpinnerNumberModel(0.8, 0.0, 10.0, 0.1));
        ysInput = new JSpinner(new SpinnerNumberModel(1.4, 0.0, 10.0, 0.1));
        diffInput = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 1.0, 0.002));
        invertChk = new JCheckBox();
        tsengInput = new JSpinner(new SpinnerNumberModel(0.1, 0.0, 1.0, 0.05));

        setTT(horizLabel, horizInput, "Horizontal epicentre of gradient fill");
        setTT(vertLabel, vertInput, "Vertical epicentre of gradient fill");
        setTT(angleLabel, angleInput, "Angle of linear fill, angle offset of conical fill");
        setTT(repeatLabel, repeatInput, "Number of times to repeat the gradient");
        setTT(xsLabel, xsInput, "Horizontal cost of fill operation");
        setTT(ysLabel, ysInput, "Vertical cost of fill operation");
        setTT(diffLabel, diffInput, "Amount of random variance to apply to the gradient");
        setTT(invertLabel, invertChk, "Whether to invert (reverse) the gradient pattern");
        setTT(tsengLabel, tsengInput, "Amount to Tseng-ify this gradient");

        getConfigOpts();

        gradOptionsRight.add(horizLabel);
        gradOptionsRight.add(horizInput);
        gradOptionsRight.add(xsLabel);
        gradOptionsRight.add(xsInput);
        gradOptionsRight.add(vertLabel);
        gradOptionsRight.add(vertInput);
        gradOptionsRight.add(ysLabel);
        gradOptionsRight.add(ysInput);
        gradOptionsRight.add(angleLabel);
        gradOptionsRight.add(angleInput);
        gradOptionsRight.add(diffLabel);
        gradOptionsRight.add(diffInput);
        gradOptionsRight.add(repeatLabel);
        gradOptionsRight.add(repeatInput);
        gradOptionsRight.add(invertLabel);
        gradOptionsRight.add(invertChk);
        if (tsengAvail) {
            gradOptionsRight.add(tsengLabel);
            gradOptionsRight.add(tsengInput);
        }

        cp.add(gradOptionsRight, BorderLayout.EAST);

        setIconImage(editor.getCanvas().extractCharImage(176, 0x6e, 1, 1, false, "$"));

        var bottomPane = new JPanel(new BorderLayout());
        var buttonPane = new JPanel(new GridLayout(1, 2, 4, 0));
        var okBtn = new JButton("OK");
        getRootPane().setDefaultButton(okBtn);
        var cancelBtn = new JButton("Cancel");
        okBtn.addActionListener(e -> {
            closeCmd = "done";
            setConfigOpts();
            dispose();
        });
        cancelBtn.addActionListener(e -> {
            closeCmd = "undo";
            dispose();
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                updateListener(closeCmd);
            }
        });
        buttonPane.add(okBtn);
        buttonPane.add(cancelBtn);
        buttonPane.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 4));
        bottomPane.add(buttonPane, BorderLayout.EAST);

        cp.add(bottomPane, BorderLayout.SOUTH);
        setResizable(false);

        pack();

        setLocationRelativeTo(editor.getFrameForRelativePositioning());

        ItemListener il = e -> updateContents();
        ChangeListener cl = e -> updateContents();
        btnLinear.addItemListener(il);
        btnBox.addItemListener(il);
        btnRadial.addItemListener(il);
        btnConic.addItemListener(il);
        btnSlime.addItemListener(il);
        invertChk.addItemListener(il);
        gradientCombo.addItemListener(il);
        sliderStart.addChangeListener(cl);
        sliderEnd.addChangeListener(cl);
        horizInput.addChangeListener(cl);
        vertInput.addChangeListener(cl);
        angleInput.addChangeListener(cl);
        repeatInput.addChangeListener(cl);
        xsInput.addChangeListener(cl);
        ysInput.addChangeListener(cl);
        ysInput.addChangeListener(cl);
        diffInput.addChangeListener(cl);
        tsengInput.addChangeListener(cl);

        updateContents();

        setVisible(true);
    }

    private void tsengCheck() {
        tsengAvail = GlobalEditor.getGlobalEditor().getBoolean("AGH_MORE_DOBERMANS", false);

        var calendar = Calendar.getInstance();
        if (calendar.get(Calendar.MONTH) != Calendar.APRIL) return;
        if (calendar.get(Calendar.DAY_OF_MONTH) != 1) return;
        // April fools!
        tsengAvail = true;
    }

    private String getPreselectKey() {
        return String.format("FILL_%s_SELECTED", editor.getWorldData().isSuperZZT() ? "SZZT" : "ZZT");
    }

    private void setTT(JComponent component1, JComponent component2, String ttText) {
        component1.setToolTipText(ttText);
        component2.setToolTipText(ttText);
    }

    private void setConfigOpts() {
        var ge = editor.getGlobalEditor();
        ge.setString("FILL_MODE", getSelectedFillMode());
        ge.setDouble("FILL_START", (double) sliderStart.getValue());
        ge.setDouble("FILL_FINISH", (double) sliderEnd.getValue());
        ge.setDouble("FILL_HORIZ", (double) horizInput.getValue());
        ge.setDouble("FILL_VERT", (double) vertInput.getValue());
        ge.setInt("FILL_ANGLE", (int) angleInput.getValue());
        ge.setInt("FILL_REPEATS", (int) repeatInput.getValue());
        ge.setDouble("FILL_XWEIGHT", (double) xsInput.getValue());
        ge.setDouble("FILL_YWEIGHT", (double) ysInput.getValue());
        ge.setDouble("FILL_DIFFUSE", (double) diffInput.getValue());
        ge.setBoolean("FILL_INVERT", invertChk.isSelected());
        if (tsengAvail) ge.setDouble("FILL_TSENG", (double) tsengInput.getValue());
        ge.setString(getPreselectKey(), (String) gradientCombo.getSelectedItem());
    }

    private void getConfigOpts() {
        var ge = editor.getGlobalEditor();
        setSelectedFillMode(ge.getString("FILL_MODE", "LINEAR"));
        sliderStart.setValue(ge.getDouble("FILL_START", 0.0));
        sliderEnd.setValue(ge.getDouble("FILL_FINISH", 1.0));
        horizInput.setValue(ge.getDouble("FILL_HORIZ", 0.5));
        vertInput.setValue(ge.getDouble("FILL_VERT", 0.5));
        angleInput.setValue(ge.getInt("FILL_ANGLE", 0));
        repeatInput.setValue(ge.getInt("FILL_REPEATS", 0));
        xsInput.setValue(ge.getDouble("FILL_XWEIGHT", 0.8));
        ysInput.setValue(ge.getDouble("FILL_YWEIGHT", 1.4));
        diffInput.setValue(ge.getDouble("FILL_DIFFUSE", 0.0));
        tsengInput.setValue(ge.getDouble("FILL_TSENG", 0.1));
        invertChk.setSelected(ge.getBoolean("FILL_INVERT", false));
    }

    private void setSelectedFillMode(String fillMode) {
        switch (fillMode) {
            case "LINEAR":
            default:
                btnLinear.setSelected(true);
                break;
            case "BOX":
                btnBox.setSelected(true);
                break;
            case "RADIAL":
                btnRadial.setSelected(true);
                break;
            case "CONICAL":
                btnConic.setSelected(true);
                break;
            case "SLIME":
                btnSlime.setSelected(true);
                break;
        }
    }

    private String getSelectedFillMode() {
        if (btnLinear.isSelected()) return "LINEAR";
        if (btnBox.isSelected()) return "BOX";
        if (btnRadial.isSelected()) return "RADIAL";
        if (btnConic.isSelected()) return "CONICAL";
        if (btnSlime.isSelected()) return "SLIME";
        throw new UnsupportedOperationException();
    }

    private void genList(byte[][] filled) {
        int width = filled[0].length;
        int height = filled.length;
        int count = 0;
        for (byte[] bytes : filled) {
            for (byte b : bytes) {
                count += b;
            }
        }
        xPositions = new int[count];
        yPositions = new int[count];
        tileFills = new Tile[count];
        int i = 0;
        maxX = Integer.MIN_VALUE;
        maxY = Integer.MIN_VALUE;
        minX = Integer.MAX_VALUE;
        minY = Integer.MAX_VALUE;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (filled[y][x] == 1) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                    xPositions[i] = x;
                    yPositions[i] = y;
                    i++;
                }
            }
        }
    }

    private void updateContents() {
        updateFillMode();
        updateAccess();
        updateFill();
        updateListener("updateFill");
    }

    private void updateListener(String command) {
        ActionEvent e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, command);
        listener.actionPerformed(e);
    }

    public int[] getXs() {
        return xPositions;
    }
    public int[] getYs() {
        return yPositions;
    }
    public Tile[] getTiles() {
        return tileFills;
    }

    private void updateFillMode() {

        if (btnLinear.isSelected()) {
            fillMode = LINEAR;
        } else if (btnBox.isSelected()) {
            fillMode = BOX;
        } else if (btnRadial.isSelected()) {
            fillMode = RADIAL;
        } else if (btnConic.isSelected()) {
            fillMode = CONIC;
        } else if (btnSlime.isSelected()) {
            fillMode = SLIME;
        }
    }

    private void calcTseng() {
        tseng = 0.0;
        tsengMatr = null;
        if (tsengAvail) {
            tseng = (double) tsengInput.getValue();
            tsengMatr = new double[][]{{0.4, 0.0, 0.1, 0.0, 0.3, 0.0, 0.1, 0.0},
                    {0.0, 0.9, 0.0, 0.5, 0.0, 0.9, 0.0, 0.6},
                    {0.1, 0.0, 0.2, 0.0, 0.1, 0.0, 0.2, 0.0},
                    {0.0, 0.8, 0.0, 1.0, 0.0, 0.7, 0.0, 0.9},
                    {0.3, 0.0, 0.1, 0.0, 0.4, 0.0, 0.1, 0.0},
                    {0.0, 0.9, 0.0, 0.6, 0.0, 0.9, 0.0, 0.5},
                    {0.1, 0.0, 0.2, 0.0, 0.1, 0.0, 0.2, 0.0},
                    {0.0, 0.7, 0.0, 0.9, 0.0, 0.8, 0.0, 1.0}};
        }
    }

    private void updateFill() {
        updateGradientTiles();

        calcTseng();

        if (fillMode == SLIME) {
            // Special handling for slime fill
            slimeFill();
            return;
        }
        if (fillMode == LINEAR) {
            // Precompute min/max for this angle
            precomputeLinear();
        }

        for (int i = 0; i < xPositions.length; i++) {
            int x = xPositions[i];
            int y = yPositions[i];
            double fx = doublePos(x, minX, maxX);
            double fy = doublePos(y, minY, maxY);

            double activationValue = translateXY(i, fx, fy);
            boolean tsenged = false;

            fillTile(i, activationValue, x, y);
        }
    }

    private void precomputeLinear() {
        var intAng = (int) angleInput.getValue();
        double min = Double.MAX_VALUE * 1.0;
        double max = Double.MAX_VALUE * -1.0;
        double t;
        t = linearTranslate(0.0, 0.0, intAng);
        min = Math.min(min, t); max = Math.max(max, t);
        t = linearTranslate(0.0, 1.0, intAng);
        min = Math.min(min, t); max = Math.max(max, t);
        t = linearTranslate(1.0, 0.0, intAng);
        min = Math.min(min, t); max = Math.max(max, t);
        t = linearTranslate(1.0, 1.0, intAng);
        min = Math.min(min, t); max = Math.max(max, t);
        linearMin = min;
        linearMax = max;
    }

    private void updateGradientTiles() {
        String encodedBuffer = (String) gradientCombo.getSelectedItem();
        gradientTiles = Clip.decode(encodedBuffer).getTiles();
    }

    private double translateXY(int i, double fx, double fy) {
        // Translate x and y into an activation value
        var horizontal = (double) horizInput.getValue();
        var vertical = (double) vertInput.getValue();
        var intAng = (int) angleInput.getValue();
        var normAng = 1.0 * intAng / 360.0;
        switch (fillMode) {
            case BOX: {
                double x = 1.0 - Math.abs(fx - horizontal) * 2.0;
                double y = 1.0 - Math.abs(fy - vertical) * 2.0;
                double v = Math.min(x, y);
                return v;
            }
            case RADIAL: {
                double x = fx - horizontal;
                double y = fy - vertical;
                double v = 1.0 - (double) Math.sqrt(x * x + y * y);
                return v;
            }
            case CONIC: {
                double x = fx - horizontal;
                double y = fy - vertical;
                double a = (double) ((Math.atan2(y, x) + Math.PI) / (Math.PI * 2.0));

                double v = (a + normAng) % 1.0;
                return v;
            }
            case LINEAR: {
                double v = linearTranslate(fx, fy, intAng);
                double diff = linearMax - linearMin;
                if (diff < 0.000001) {
                    v = 0.5;
                } else {
                    v = (v - linearMin) / diff;
                }

                return v;
            }
        }
        throw new RuntimeException("Unsupported fill mode");
    }

    private double linearTranslate(double fx, double fy, int angAdd) {
        switch (angAdd) {
            case 0:
            case 360:
                return fx;
            case 90:
                return 1.0 - fy;
            case 180:
                return 1.0 - fx;
            case 270:
                return fy;
            default:
                double linearAng = (Math.PI * angAdd / 180.0);
                double dx = fx - 0.5;
                double dy = fy - 0.5;
                double mag = Math.sqrt(dx * dx + dy * dy);
                double ang = Math.atan2(dy, dx);
                ang += linearAng;
                return Math.cos(ang) * mag + 0.5;
        }
    }

    private void fillTile(int i, double val, int x, int y) {
        // For now, compress the value into the gradient range and pick something
        val = Util.clamp(val, 0.0, 1.0);
        double diffusion = (double) diffInput.getValue();
        val += rng.nextGaussian() * diffusion;

        // 0 repeats: 0.0 -> 1.0
        // 1 repeats: 0.0 -> 1.0 -> 0.0
        // 2 repeats: 0.0 -> 1.0 -> 0.0 -> 1.0

        val *= ((int) repeatInput.getValue() + 1);
        val = 1.0 - Math.abs((val % 2.0) - 1.0);

        double min = (double) sliderStart.getValue();
        double max = (double) sliderEnd.getValue();
        double rmin = Math.min(min, max);
        double rmax = Math.max(min, max);
        double rdiff = rmax - rmin;
        val = val * rdiff + rmin;
        val = Util.clamp(val, 0.0, 1.0);

        boolean tsenged = false;
        if (tsengMatr != null) {
            double r = rng.nextDouble() * 0.1 + tseng;
            if (tsengMatr[x % 8][y % 8] > 1.1 - r) {
                tsenged = true;
            }
        }

        if (invertChk.isSelected() ^ tsenged) val = 1.0 - val;

        int idx = (int)(val * gradientTiles.length);
        if (idx == gradientTiles.length) idx--;
        tileFills[i] = gradientTiles[idx];
    }

    private double doublePos(int val, int min, int max) {
        if (val == min && val == max) return 0.5;
        return 1.0 * (val - min) / (max - min);
    }

    private void slimeFill() {
        int width = filled[0].length;
        int height = filled.length;
        float[][] slimeArea = new float[height][width];
        for (var slimeRow : slimeArea) {
            Arrays.fill(slimeRow, Float.MAX_VALUE);
        }
        ArrayDeque<ArrayList<Integer>> queue = new ArrayDeque<>();
        // Initialise all edges at 0.0f
        for (int i = 0; i < xPositions.length; i++) {
            int x = xPositions[i];
            int y = yPositions[i];
            if (isEdge(x, y, width, height)) {
                slimeArea[y][x] = 0.0f;
                queue.add(pair(x, y));
            }
        }

        float xScale = (float) ((double) xsInput.getValue());
        float yScale = (float) ((double) ysInput.getValue());
        float max = 0.0f;

        while (!queue.isEmpty()) {
            var pos = queue.pop();
            int x = pos.get(0);
            int y = pos.get(1);
            float val = slimeArea[y][x];
            max = Math.max(max, slimeExpand(x + 1, y, width, height, val + xScale, queue, slimeArea));
            max = Math.max(max, slimeExpand(x - 1, y, width, height, val + xScale, queue, slimeArea));
            max = Math.max(max, slimeExpand(x, y + 1, width, height, val + yScale, queue, slimeArea));
            max = Math.max(max, slimeExpand(x, y - 1, width, height, val + yScale, queue, slimeArea));
        }
        for (int i = 0; i < xPositions.length; i++) {
            int x = xPositions[i];
            int y = yPositions[i];
            float v = slimeArea[y][x];
            if (max >= 0.000001) {
                v = v / max;
            } else {
                v = 0.0f;
            }
            fillTile(i, v, x, y);
        }
    }

    private float slimeExpand(int x, int y, int width, int height, float v, ArrayDeque<ArrayList<Integer>> queue, float[][] slimeArea) {
        if (x < 0 || y < 0 || x >= width || y >= height) return 0.0f;
        if (filled[y][x] == 0) return 0.0f;
        if (v < slimeArea[y][x]) {
            slimeArea[y][x] = v;
            queue.add(pair(x, y));
            return v;
        }
        return 0.0f;
    }

    private boolean isEdge(int x, int y, int width, int height) {
//        if (x == 0 || y == 0 || x == width - 1 || y == height - 1) {
//            return true;
//        }
        if (y < height - 1 && filled[y + 1][x] == 0) return true;
        if (y > 0 && filled[y - 1][x] == 0) return true;
        if (x > 0 && filled[y][x - 1] == 0) return true;
        if (x < width - 1 && filled[y][x + 1] == 0) return true;
        return false;
    }


    private void updateAccess() {
        switch (fillMode) {
            case LINEAR:
                setState(horizLabel, horizInput, false);
                setState(vertLabel, vertInput, false);
                setState(angleLabel, angleInput, true);
                setState(xsLabel, xsInput, false);
                setState(ysLabel, ysInput, false);
            break;
            case BOX:
            case RADIAL:
                setState(horizLabel, horizInput, true);
                setState(vertLabel, vertInput, true);
                setState(angleLabel, angleInput, false);
                setState(xsLabel, xsInput, false);
                setState(ysLabel, ysInput, false);
                break;
            case CONIC:
                setState(horizLabel, horizInput, true);
                setState(vertLabel, vertInput, true);
                setState(angleLabel, angleInput, true);
                setState(xsLabel, xsInput, false);
                setState(ysLabel, ysInput, false);
                break;
            case SLIME:
                setState(horizLabel, horizInput, false);
                setState(vertLabel, vertInput, false);
                setState(angleLabel, angleInput, false);
                setState(xsLabel, xsInput, true);
                setState(ysLabel, ysInput, true);
            break;
        }
    }

    private void setState(JLabel lbl, JSpinner spin, boolean state) {
        lbl.setEnabled(state);
        spin.setEnabled(state);
    }
}
