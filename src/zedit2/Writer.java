package zedit2;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Writer {
    private File file;
    private FileWriter writer;

    public Writer(File file) throws IOException {
        int num = 1;
        while (file.exists() && !file.isDirectory()) {
            System.out.println("file " + file + " exists. Trying again");
            num++;
            var ext = getExtension(file);
            var basename = getBasename(file);
            System.out.println("About to construct " + basename + "-" + num + ext);
            file = new File(basename + "-" + num + ext);
            System.out.println("Constructed " + file);
            if (num >= 99) break;
            System.out.println("Looping");
        }

        this.file = file;
        this.writer = new FileWriter(file);
    }

    private static String getBasename(File file) {
        String filename = file.toString();
        int dotPos = filename.lastIndexOf('.');
        if (dotPos == -1) return "";
        return filename.substring(0, dotPos);
    }

    private static String getExtension(File file) {
        String filename = file.toString();
        int dotPos = filename.lastIndexOf('.');
        if (dotPos == -1) return filename;
        return filename.substring(dotPos);
    }

    public File getFile() {
        return file;
    }

    public FileWriter getWriter() {
        return writer;
    }
}
