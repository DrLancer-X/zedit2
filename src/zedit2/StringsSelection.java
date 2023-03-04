package zedit2;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StringsSelection implements Transferable {
    private final List<String> strings;
    public static final DataFlavor flavor = new DataFlavor(List.class, "application/x-array-of-strings");

    public StringsSelection(List<String> strings) {
        this.strings = strings;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{flavor};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return (this.flavor.equals(flavor));
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (!isDataFlavorSupported(flavor)) {
            throw new UnsupportedFlavorException(flavor);
        }
        return strings;
    }

    public List<String> getStrings() {
        return strings;
    }
}
