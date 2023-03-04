package zedit2;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;

public class Menu implements Iterable<MenuEntry> {
    private ArrayList<MenuEntry> list = new ArrayList<>();
    private String title;
    public Menu(String title)
    {
        this.title = title;
    }

    public void add(MenuEntry entry) {
        list.add(entry);
    }
    public void add(String name, String key, ActionListener act) {
        list.add(new MenuEntry(name, key, act));
    }
    public void add(String name, ChangeListener act, boolean init) {
        list.add(new MenuEntry(name, act, init));
    }
    public void add() {
        list.add(new MenuEntry());
    }
    public void add(JMenu submenu, String key) {
        list.add(new MenuEntry(submenu, key));
    }

    public String getTitle() {
        return title;
    }

    @Override
    public Iterator<MenuEntry> iterator() {
        return list.iterator();
    }


}
