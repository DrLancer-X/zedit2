package zedit2;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

public class Help {
    private static Help helpInstance;
    private JDialog dialog;
    private String currentFile;
    public Help(WorldEditor editor) {
        if (helpInstance != null) {
            helpInstance.dialog.toFront();
        } else {
            openHelp(editor);
        }
    }

    private void openHelp(WorldEditor editor) {
        var canvas = editor.getCanvas();
        dialog = new JDialog();
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                helpInstance = null;
            }
        });

        JEditorPane editorPane = new JEditorPane();
        JScrollPane scrollPane = new JScrollPane(editorPane);
        editorPane.setEditable(false);
        URL url = null;
        try {
            url = Main.class.getClassLoader().getResource("help/index.html");
            currentFile = url.getFile();
            editorPane.setPage(url);
            int w = editor.getGlobalEditor().getInt("HELPBROWSER_WIDTH");
            int h = editor.getGlobalEditor().getInt("HELPBROWSER_HEIGHT");
            editorPane.setPreferredSize(new Dimension(w, h));
            dialog.setTitle("ZEdit2 help");
            dialog.setIconImage(canvas.extractCharImage('?', 0x9F, 1, 1, false, "$"));
            editorPane.addHyperlinkListener(new HyperlinkListener() {
                @Override
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
                        try {
                            if (e.getURL().toString().startsWith("http")) {
                                Desktop.getDesktop().browse(e.getURL().toURI());
                            } else {
                                var ref = e.getURL().getRef();
                                System.out.println("ref: " + ref);
                                if (!e.getURL().getFile().equals(currentFile)) {
                                    System.out.println("Loading page");
                                    editorPane.setPage(e.getURL());
                                }
                                editorPane.scrollToReference(ref);

                            }
                        } catch (IOException | URISyntaxException ignored) {
                        }
                    }
                }
            });
            dialog.add(scrollPane);
            dialog.pack();
            dialog.setLocationRelativeTo(editor.getFrameForRelativePositioning());
            dialog.setVisible(true);
            helpInstance = this;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //var res = Main.class.getResource("help/test.html");
    }
}
