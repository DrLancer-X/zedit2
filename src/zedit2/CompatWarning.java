package zedit2;

import java.util.ArrayList;
import java.util.HashMap;

public class CompatWarning {
    private boolean szzt;
    private int warningLevel = 0;
    private String prefix = "";
    HashMap<Integer, ArrayList<String>> messages = new HashMap<>();

    public CompatWarning(boolean szzt) {
        this.szzt = szzt;
    }

    public void warn(int level, String message) {
        warningLevel = Math.max(warningLevel, level);
        if (!messages.containsKey(level)) {
            messages.put(level, new ArrayList<>());
        }
        messages.get(level).add(prefix + message);
    }

    public void mergeIn(CompatWarning other, String prefix) {
        warningLevel = Math.max(warningLevel, other.warningLevel);
        for (int level : other.messages.keySet()) {
            if (!messages.containsKey(level)) {
                messages.put(level, new ArrayList<>());
            }
            for (var message : other.messages.get(level)) {
                messages.get(level).add(message);
            }
        }
    }

    public int getWarningLevel() {
        return warningLevel;
    }

    public String getMessages(int level) {
        if (!messages.containsKey(level)) {
            return "";
        } else {
            int msgCount = 0;
            var output = new StringBuilder();
            var msgs = messages.get(level);
            boolean firstLine = true;
            for (var msg : msgs) {
                if (firstLine) {
                    firstLine = false;
                } else {
                    output.append('\n');
                }
                if (msgs.size() > 1) output.append("‧ ");
                output.append(msg);
                msgCount++;
                if (msgCount > 10) {
                    output.append(String.format("\n...%d other warnings hidden...", msgs.size() - msgCount));
                    break;
                }
            }
            return output.toString();
        }
    }

    public void setPrefix(String s) {
        prefix = s;
    }

    public boolean isSuperZZT() {
        return szzt;
    }
}
