package zedit2;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class ConvertImage extends JDialog {
    private Type t;
    private final WorldEditor editor;
    private BufferedImage image;
    private boolean matcherEnabled = true;

    private JLabel sourceImageLabel;
    private JLabel destImageLabel;
    private Timer previewTimer;

    private JSpinner srcTop, srcBottom, srcLeft, srcRight, sizeW, sizeH, macroW, macroH, objCount;
    private ChangeListener changeListener;
    private boolean convertSetup = false;
    private ArrayList<JCheckBox> elementCheckboxes;
    private Converter converter;
    private JButton ok;
    private JCheckBox livePreviewChk;

    private int checkingValue = 0;

    private int bufferW, bufferH;
    private Tile[] buffer = null;

    private HashSet<String> ditherOnly = new HashSet<>(Arrays.asList("Empty", "Water", "Floor", "Solid", "Normal", "Breakable"));
    private HashSet<String> allGfx = new HashSet<>(Arrays.asList("Player", "Ammo", "Torch", "Gem", "Key", "Door", "Scroll", "Passage", "Duplicator",
            "Bomb", "Energizer", "Bullet", "Water", "Floor", "Solid", "Normal", "Breakable", "Boulder", "SliderNS", "SliderEW",
            "BlinkWall", "Ricochet", "HBlinkRay", "Bear", "Ruffian", "Slime", "Shark", "Pusher", "Lion", "Tiger",
            "VBlinkRay", "Head", "Segment", "BlueText", "GreenText", "CyanText", "RedText", "PurpleText", "BrownText",
            "BlackText", "WaterN", "WaterS", "WaterE", "WaterW", "Roton", "DragonPup", "Pairer", "Spider", "Lava",
            "BlackBText", "BlueBText", "GreenBText", "CyanBText", "RedBText", "PurpleBText", "BrownBText", "GreyBText", "GreyText"));

    public ConvertImage(WorldEditor worldEditor, Image sourceImage) {
        this.editor = worldEditor;
        getBufferedImage(sourceImage, e -> createGUI());
    }

    private void createGUI() {
        setTitle("Convert Image");
        setIconImage(editor.getCanvas().extractCharImage(20, 110, 1, 1, false, "$"));
        Util.addEscClose(this, getRootPane());
        setModalityType(JDialog.ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                terminateConverter();
            }
        });
        var cp = getContentPane();
        var ge = editor.getGlobalEditor();
        cp.setLayout(new BorderLayout());

        changeListener = e -> convert();

        previewTimer = null;
        sourceImageLabel = new JLabel();
        destImageLabel = new JLabel();
        sourceImageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        sourceImageLabel.setVerticalAlignment(SwingConstants.CENTER);
        destImageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        destImageLabel.setVerticalAlignment(SwingConstants.CENTER);

        var l = new JScrollPane(sourceImageLabel);
        var r = new JScrollPane(destImageLabel);
        l.setPreferredSize(new Dimension(256, 256));
        r.setPreferredSize(new Dimension(256, 256));
        AdjustmentListener adjListenerH = e -> scrollMatch(e, l, r, true);
        AdjustmentListener adjListenerV = e -> scrollMatch(e, l, r, false);
        l.getHorizontalScrollBar().addAdjustmentListener(adjListenerH);
        l.getVerticalScrollBar().addAdjustmentListener(adjListenerV);
        r.getHorizontalScrollBar().addAdjustmentListener(adjListenerH);
        r.getVerticalScrollBar().addAdjustmentListener(adjListenerV);
        var imgPane = new JPanel(new GridLayout(1, 2, 4, 4));
        imgPane.add(l);
        imgPane.add(r);

        var cfgPane = new JPanel(new BorderLayout());

        //var splitPane = new JPanel(JSplitPane.VERTICAL_SPLIT, imgPane, cfgPane);
        var splitPane = new JPanel(new BorderLayout());
        splitPane.add(imgPane, BorderLayout.CENTER);
        splitPane.add(cfgPane, BorderLayout.SOUTH);

        var optionsBox = new JPanel(new GridLayout(0, 1, 0, 0));

        int w = image.getWidth();
        int h = image.getHeight();
        srcLeft = addSpinner(optionsBox, "Left:", new SpinnerNumberModel(0, 0, w - 1, 1));
        srcRight = addSpinner(optionsBox, "Right:", new SpinnerNumberModel(w - 1, 0, w - 1, 1));
        srcTop = addSpinner(optionsBox, "Top:", new SpinnerNumberModel(0, 0, h - 1, 1));
        srcBottom = addSpinner(optionsBox, "Bottom:", new SpinnerNumberModel(h - 1, 0, h - 1, 1));
        macroW = addSpinner(optionsBox, "Macroblock W:", new SpinnerListModel(new Integer[]{1, 2, 4, 8}));
        macroH = addSpinner(optionsBox, "Macroblock H:", new SpinnerListModel(new Integer[]{1, 2, 7, 14}));
        macroW.setValue(ge.getInt("CONVERT_MACROW", 8));
        macroH.setValue(ge.getInt("CONVERT_MACROH", 14));

        int defaultCharW = Math.max(1, (w + 4) / 8);
        int defaultCharH = Math.max(1, (h + 7) / 14);
        if (defaultCharW > editor.getWidth()) {
            defaultCharW = editor.getWidth();
            defaultCharH = Math.max(1, (h * 8 * editor.getWidth() / w + 7) / 14);
        }
        if (defaultCharH > editor.getHeight()) {
            defaultCharH = editor.getHeight();
            defaultCharW = Math.max(1, (w * 14 * editor.getHeight() / h + 4) / 8);
        }

        sizeW = addSpinner(optionsBox, "Output Width:", new SpinnerNumberModel(defaultCharW, 1, editor.getWidth(), 1));
        sizeH = addSpinner(optionsBox, "Output Height:", new SpinnerNumberModel(defaultCharH, 1, editor.getHeight(), 1));
        objCount = addSpinner(optionsBox, "Max Stats:", new SpinnerNumberModel(0, 0, 32767, 1));

        objCount.setValue(ge.getInt("CONVERT_MAXSTATS", 0));

        var usePanel = new JPanel(new GridLayout(0, 5));

        var leftPane = new JPanel(new BorderLayout());
        leftPane.add(optionsBox, BorderLayout.CENTER);

        cfgPane.add(leftPane, BorderLayout.WEST);
        cfgPane.add(usePanel, BorderLayout.CENTER);

        var buttonBar = new JPanel(new BorderLayout());
        var buttonGroup = new JPanel(new GridLayout(1, 0, 4, 0));
        buttonGroup.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        leftPane.add(buttonBar, BorderLayout.SOUTH);
        buttonBar.add(buttonGroup, BorderLayout.EAST);

        livePreviewChk = new JCheckBox("Preview Output Image", ge.getBoolean("CONVERT_LIVEPREVIEW", true));

        buttonBar.add(livePreviewChk, BorderLayout.NORTH);

        ok = new JButton("OK");
        getRootPane().setDefaultButton(ok);
        ok.setEnabled(false);
        ok.addActionListener(e -> {
            var szzt = editor.getWorldData().isSuperZZT();
            ge.setBlockBuffer(bufferW, bufferH, buffer, false, szzt);
            ge.setInt("CONVERT_MACROW", (int) macroW.getValue());
            ge.setInt("CONVERT_MACROH", (int) macroH.getValue());
            ge.setInt("CONVERT_MAXSTATS", (int) objCount.getValue());
            ge.setString(szzt ? "CONVERT_SZZT_TYPES" : "CONVERT_ZZT_TYPES", getElementsString());
            ge.setBoolean("CONVERT_LIVEPREVIEW", livePreviewChk.isSelected());
            dispose();
        });
        var cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());
        buttonGroup.add(ok);
        buttonGroup.add(cancel);

        boolean szzt = editor.getWorldData().isSuperZZT();
        elementCheckboxes = new ArrayList<>();
        ItemListener itemListener = e -> convert();
        HashSet<String> alreadySelected = new HashSet<>();

        for (int i = 0; i < 255; i++) {
            String name = ZType.getName(szzt, i);
            if (name.startsWith("Unknown")) continue;
            var chkb = new JCheckBox(name);
            chkb.setSelected(alreadySelected.contains(name));
            var minSize = chkb.getMinimumSize();
            minSize.height -= 8;
            minSize.width -= 8;
            chkb.setPreferredSize(minSize);
            chkb.addItemListener(itemListener);
            elementCheckboxes.add(chkb);
            usePanel.add(chkb);
        }
        setElementsString(ge.getString(szzt ? "CONVERT_SZZT_TYPES" : "CONVERT_ZZT_TYPES"));

        addButton(usePanel, "Select all", e -> {
            for (var chkb : elementCheckboxes) chkb.setSelected(true);
        });
        addButton(usePanel, "Deselect all", e -> {
            for (var chkb : elementCheckboxes) chkb.setSelected(false);
        });
        addButton(usePanel, "Dither only", e -> {
            for (var chkb : elementCheckboxes) chkb.setSelected(ditherOnly.contains(chkb.getText()));
        });
        addButton(usePanel, "All gfx", e -> {
            for (var chkb : elementCheckboxes) chkb.setSelected(allGfx.contains(chkb.getText()));
        });
        usePanel.setBorder(BorderFactory.createTitledBorder("Element use"));

        // Crop Source N / S / E / W
        // Destination W / H
        // Macroblock W (1, 2, 4, 8) / H (1, 2, 7, 14)
        // Objects
        // Types to use

        cp.add(splitPane, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(editor.getFrameForRelativePositioning());
        convertSetup = true;
        convert();
        setVisible(true);
    }

    private void setElementsString(String string) {
        if (string == null) return;
        if (string.length() != elementCheckboxes.size()) return;
        for (int i = 0; i < string.length(); i++) {
            elementCheckboxes.get(i).setSelected(string.charAt(i) == 'X');
        }
    }

    private String getElementsString() {
        StringBuilder str = new StringBuilder(elementCheckboxes.size());
        for (var chkb : elementCheckboxes) {
            str.append(chkb.isSelected() ? 'X' : '.');
        }
        return str.toString();
    }

    private void addButton(JPanel usePanel, String buttonText, ActionListener listener) {
        JButton button = new JButton(buttonText);
        usePanel.add(button);

        button.addActionListener(listener);
        var minSize = button.getMinimumSize();
        minSize.height -= 8;
        button.setPreferredSize(minSize);
    }


    private void convert() {
        if (!convertSetup) return;
        terminateConverter();
        ok.setEnabled(false);
        buffer = null;

        int left = (int) srcLeft.getValue();
        int right = (int) srcRight.getValue();
        int top = (int) srcTop.getValue();
        int bottom = (int) srcBottom.getValue();
        int mw = (int) macroW.getValue();
        int mh = (int) macroH.getValue();
        int outw = (int) sizeW.getValue();
        int outh = (int) sizeH.getValue();
        int maxObjs = (int) objCount.getValue();



        var croppedSourceImage = cropImage(image, left, right, top, bottom);
        var scaledImage = scaleImage(croppedSourceImage, outw * DosCanvas.CHAR_W, outh * DosCanvas.CHAR_H);
        updateSourceImage(scaledImage);
        var outputImage = new BufferedImage(outw * DosCanvas.CHAR_W, outh * DosCanvas.CHAR_H, BufferedImage.TYPE_INT_RGB);
        var outg = outputImage.getGraphics();
        outg.setColor(new Color(0x7F7F7F));
        outg.fillRect(0, 0, outputImage.getWidth(), outputImage.getHeight());
        updateDestImage(outputImage);

        boolean szzt = editor.getWorldData().isSuperZZT();
        converter = new Converter(szzt, outw, outh, mw, mh, maxObjs, scaledImage);
        checkingValue++;
        converter.setCheckVal(checkingValue);
        //var usableElements = new ArrayList<Integer>();
        boolean anySelected = false;
        for (var chkb : elementCheckboxes) {
            if (chkb.isSelected()) {
                var elementName = chkb.getText();
                var id = ZType.getId(szzt, elementName);
                //usableElements.add(id);
                var tile = new Tile(id, 15);
                var chr = ZType.getChar(szzt, tile);
                converter.addElement(id, chr);
                anySelected = true;
            }
        }

        var output = new Tile[outw * outh];
        var outputChr = new int[outw * outh];
        var outputVcol = new int[outw * outh];
        Arrays.fill(outputChr, -1);
        converter.setBlink(editor.getGlobalEditor().getBoolean("BLINKING", true));
        converter.setGfx(editor.getCanvas());
        converter.setCallback(new ConverterCallback() {
            @Override
            public void converted(int checkVal, int x, int y, int id, int col, int chr, int vcol) {
                //System.out.println("Converted called from: " + Thread.currentThread());
                if (checkVal != checkingValue) return;
                Tile t;
                if (id == ZType.OBJECT && chr != 32) {
                    var stat = new Stat(szzt);
                    stat.setCycle(3);
                    stat.setP1(chr);
                    t = new Tile(id, col, stat);
                } else {
                    t = new Tile(id, col);
                }
                output[y * outw + x] = t;
                outputChr[y * outw + x] = chr;
                outputVcol[y * outw + x] = vcol;
            }

            @Override
            public void finished(int checkVal) {
                if (checkVal != checkingValue) return;
                //System.out.println("finished called from: " + Thread.currentThread());
                bufferW = outw;
                bufferH = outh;
                buffer = output;
                ok.setEnabled(true);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        updatePreview(outw, outh, outputChr, outputVcol, outputImage);
                    }
                });
                previewTimer.stop();
                previewTimer = null;
            }
        });

        if (anySelected) {
            converter.beginConvert();
            previewTimer = createPreviewTimer(outw, outh, outputChr, outputVcol, outputImage);
            previewTimer.start();
        }

        //(outw, outh, mw, mh, maxObjs, usableElements, scaledImage);
    }

    private void updatePreview(int outw, int outh, int[] outputChr, int[] outputVcol, BufferedImage outputImage) {
        if (!livePreviewChk.isSelected()) return;
        //System.out.println("updatePreview called from: " + Thread.currentThread());
        var g = outputImage.getGraphics();
        var canvas = editor.getCanvas();
        for (int y = 0; y < outh; y++) {
            for (int x = 0; x < outw; x++) {
                int i = y * outw + x;
                int chr = outputChr[i];
                if (chr != -1) {
                    int vcol = outputVcol[i];
                    var tileGfx = canvas.extractCharImage(chr, vcol, 1, 1, false, "$");
                    g.drawImage(tileGfx, x * DosCanvas.CHAR_W, y * DosCanvas.CHAR_H, null);
                }
            }
        }
        updateDestImage(outputImage);
    }

    private Timer createPreviewTimer(int outw, int outh, int[] outputChr, int[] outputVcol, BufferedImage outputImage) {
        Timer timer = new Timer(250, (e) -> {
            updatePreview(outw, outh, outputChr, outputVcol, outputImage);
        });
        timer.setRepeats(true);
        timer.setCoalesce(true);
        return timer;
    }

    private void updateSourceImage(BufferedImage scaledImage) {
        var image = superZZTScale(scaledImage);

        sourceImageLabel.setIcon(new ImageIcon(image));
    }

    private Image superZZTScale(BufferedImage scaledImage) {
        if (editor.getWorldData().isSuperZZT()) {
            return scaledImage.getScaledInstance(scaledImage.getWidth() * 2, scaledImage.getHeight(), Image.SCALE_REPLICATE);
        }
        return scaledImage;
    }

    private void updateDestImage(BufferedImage outputImage) {
        var image = superZZTScale(outputImage);
        destImageLabel.setIcon(new ImageIcon(image));
    }

    private void terminateConverter() {
        if (converter != null) {
            converter.stop();
            converter = null;
        }
    }


    private BufferedImage scaleImage(BufferedImage croppedSourceImage, int w, int h) {
        if (croppedSourceImage.getWidth() == w && croppedSourceImage.getHeight() == h) return croppedSourceImage;
        var newImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        var g = newImg.getGraphics();
        var g2 = (Graphics2D) g;
        RenderingHints rh = new RenderingHints(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHints(rh);
        g.drawImage(croppedSourceImage, 0, 0, newImg.getWidth(), newImg.getHeight(),
                0,0, croppedSourceImage.getWidth(), croppedSourceImage.getHeight(), null);
        return newImg;
    }

    private BufferedImage cropImage(BufferedImage image, int left, int right, int top, int bottom) {
        if (left == 0 && top == 0 && right == image.getWidth() - 1 && bottom == image.getHeight() - 1) return image;

        int width = Math.max(1, right - left);
        int height = Math.max(1, bottom - top);
        var newImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var g = newImg.getGraphics();
        g.drawImage(image, -left, -top, null);
        return newImg;
    }

    private void getBufferedImage(Image sourceImage, ActionListener act) {
        int w = sourceImage.getWidth(null);
        int h = sourceImage.getHeight(null);
        if (w == -1 || h == -1) {
            waitForBufferedImage(sourceImage, act);
        }
        image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        boolean drawnImage = image.getGraphics().drawImage(sourceImage, 0, 0, null);
        if (!drawnImage) {
            waitForBufferedImage(sourceImage, act);
        }
        ActionEvent e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "Buffered image");
        act.actionPerformed(e);
    }

    private void waitForBufferedImage(Image sourceImage, ActionListener act) {
        var timer = new Timer(10, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getBufferedImage(sourceImage, act);
            }
        });
        timer.start();
    }

    private JSpinner addSpinner(JPanel optionsBox, String label, SpinnerModel model) {
        JPanel indivBox = new JPanel(new GridLayout(1, 2, 4, 0));
        indivBox.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        var lbl = new JLabel(label);
        lbl.setHorizontalAlignment(SwingConstants.RIGHT);
        indivBox.add(lbl);
        var spinner = new JSpinner(model);
        final ConvertImage obj = this;
        spinner.addChangeListener(changeListener);
        indivBox.add(spinner);
        optionsBox.add(indivBox);
        return spinner;
    }

    private void scrollMatch(AdjustmentEvent e, JScrollPane l, JScrollPane r, boolean horiz) {
        if (matcherEnabled) {
            matcherEnabled = false;
            var scrollBar = e.getSource();
            if (horiz) {
                l.getHorizontalScrollBar().setValue(e.getValue());
                r.getHorizontalScrollBar().setValue(e.getValue());
            } else {
                l.getVerticalScrollBar().setValue(e.getValue());
                r.getVerticalScrollBar().setValue(e.getValue());
            }

            matcherEnabled = true;
        }
    }
}
