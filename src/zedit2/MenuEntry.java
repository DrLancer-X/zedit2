package zedit2;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;

public class MenuEntry {
    private String title;
    private String key;
    private ActionListener act;
    private ChangeListener cAct;
    private int type;
    private boolean init;
    private JMenu submenu;

    private static final int TYPE_MENUITEM = 1;
    private static final int TYPE_SEPARATOR = 2;
    private static final int TYPE_CHECKBOX = 3;
    private static final int TYPE_SUBMENU = 4;

    public MenuEntry(String title, String key, ActionListener act) {
        type = TYPE_MENUITEM;
        this.title = title;
        this.key = key;
        this.act = act;
    }
    public MenuEntry(String title, ChangeListener act, boolean init) {
        type = TYPE_CHECKBOX;
        this.title = title;
        this.cAct = act;
        this.init = init;
    }
    public MenuEntry() {
        type = TYPE_SEPARATOR;
    }

    public MenuEntry(JMenu submenu, String key) {
        type = TYPE_SUBMENU;
        this.title = submenu.getText();
        this.submenu = submenu;
        this.key = key;
    }

    private void setTitle(GlobalEditor ge, JMenuItem item) {
        String text = title;

        if (key != null) {
            var keyStroke = KeyStroke.getKeyStroke(ge.getString("K_" + key));
            var keyString = Util.keyStrokeString(keyStroke);
            if (keyString != null) {
                text = String.format("%s (%s)", title, keyString);
            }
        }
        item.setText(text);
    }

    public void addToJMenu(GlobalEditor ge, JMenu menu) {
        switch (type) {
            case TYPE_MENUITEM: {
                JMenuItem item = new JMenuItem();
                setTitle(ge, item);
                item.addActionListener(act);
                menu.add(item);
                break;
            }
            case TYPE_CHECKBOX: {
                var menuItem = new JCheckBoxMenuItem();
                setTitle(ge, menuItem);
                menuItem.setSelected(init);
                menuItem.addChangeListener(cAct);
                menu.add(menuItem);
                break;
            }
            case TYPE_SEPARATOR: {
                menu.addSeparator();
                break;
            }
            case TYPE_SUBMENU: {
                setTitle(ge, submenu);
                menu.add(submenu);
                break;
            }
            default:
                throw new RuntimeException("Invalid item value");
        }
    }
}
