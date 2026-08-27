package upmc;

import java.io.File;
import javax.swing.*;
import javax.swing.filechooser.*;

public class ChooserFileView extends FileView {
    ImageIcon SPMCIcon = Utils.createImageIcon("images/SPMC.ico");
    ImageIcon AboutIcon = Utils.createImageIcon("images/AboutApp.ico");

    public String getName(File f) {
        return null; //let the L&F FileView figure this out
    }

    public String getDescription(File f) {
        return null; //let the L&F FileView figure this out
    }

    public Boolean isTraversable(File f) {
        return null; //let the L&F FileView figure this out
    }

    public String getTypeDescription(File f) {
        String extension = Utils.getExtension(f);
        String type = null;

        if (extension != null) {
            if (extension.equals(Utils.upm)) {
                type = "NSP Image";
            } else if (extension.equals(Utils.xml)){
                type = "XML Image";
            }
        }
        return type;
    }

    public Icon getIcon(File f) {
        String extension = Utils.getExtension(f);
        Icon icon = null;

        if (extension != null) {
            if (extension.equals(Utils.upm)) {
                icon = SPMCIcon;
            } else if (extension.equals(Utils.xml)) {
                icon = SPMCIcon;
            }
        }
        return icon;
    }
}
