package upmc;

import java.io.File;
import javax.swing.*;
import javax.swing.filechooser.*;

public class ChooserFilter extends FileFilter
{
    public final static int FILE_UPM = 1;
    public final static int FILE_LOG = 2;
    public final static int FILE_SIM = 3;
    public static int fileType = FILE_UPM;

    public void setFileType(int value)
    {
        fileType = value;
    }

    // Accept all directories and all gif, jpg, tiff, or png files.
    public boolean accept(File f)
    {
        if (f.isDirectory())
        {
            return true;
        }

        String extension = Utils.getExtension(f);
        if (extension != null)
        {
            if ((fileType == FILE_SIM) && (extension.equals(Utils.sim)))
            {
                return true;
            }
            else if ((fileType == FILE_UPM) && (extension.equals(Utils.upm) || extension.equals(Utils.xml) || extension.equals(Utils.nsp)))
            {
                return true;
            }
            else
            {
                return false;
            }
        }

        return false;
    }

    // The description of this filter
    public String getDescription()
    {
        if (fileType == FILE_LOG)
            return "Log Files";
        else if (fileType == FILE_SIM)
            return "Simulation Files";
        else
            return "UPMC Files";
    }
}
