package upmc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.input.SAXBuilder;
import java.util.List;
import java.io.IOException;
import org.jdom.JDOMException;
import org.w3c.dom.*;

import javax.swing.JOptionPane;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class XmlParser
{

    private final static int UPM_VERSION = 3;

    private ArrayList<String> portSettings = new ArrayList<String>();

    public ArrayList<String> getPortSettings()
    {
        return portSettings;
    }

    public void CreateXml(File file, ArrayList<String> portSettings, ArrayList<String> msg)
    {
        Document document = new Document();
        Element root = new Element("upmc");

        Element version = new Element("Versions");
        version.setAttribute("upmVersion", Integer.toString(UPM_VERSION));
        root.addContent(version);

        Element spChild = new Element("SerialPort");
        spChild.setAttribute("PortName", portSettings.get(0));
        spChild.setAttribute("BaudRate", portSettings.get(1));
        spChild.setAttribute("DataBits", portSettings.get(2));
        spChild.setAttribute("StopBits", portSettings.get(3));
        spChild.setAttribute("Parity", portSettings.get(4));
        spChild.setAttribute("Handshake", portSettings.get(5));
        spChild.setAttribute("Mode", portSettings.get(6));
        spChild.setAttribute("NL", portSettings.get(7));
        spChild.setAttribute("NLTimeout", portSettings.get(8));
        spChild.setAttribute("Interface", portSettings.get(9));
        spChild.setAttribute("TCPMode", portSettings.get(10));
        spChild.setAttribute("TCPAddress", portSettings.get(11));
        spChild.setAttribute("TCPPort", portSettings.get(12));

        // spChild.setText("PortName");
        root.addContent(spChild);

        //
        // Creating a child for the root element. Here we can see how to
        // set the text of an xml element.
        //
        Element msgChild = new Element("Messages");
        for (int loop = 0; loop < msg.size(); loop++)
        {
            msgChild.addContent(new Element("Msg" + (loop + 1)).setText(msg.get(loop)));
        }

        //
        // Add the child to the root element and add the root element as
        // the document content.
        //
        root.addContent(msgChild);
        document.setContent(root);

        try
        {
            // FileWriter writer = new FileWriter("spmc.xml");
            FileWriter writer;
            if ((file.getName().endsWith(".upm")) || (file.getName().endsWith(".xml")))
            {
                writer = new FileWriter(file.getAbsoluteFile());
            }
            else
            {
                writer = new FileWriter(file.getAbsoluteFile() + ".upm");
            }
            XMLOutputter outputter = new XMLOutputter();

            //
            // Set the XLMOutputter to pretty formatter. This formatter
            // use the TextMode.TRIM, which mean it will remove the
            // trailing white-spaces of both side (left and right)
            //
            outputter.setFormat(Format.getPrettyFormat());

            //
            // Write the document to a file and also display it on the
            // screen through System.out.
            //
            outputter.output(document, writer);
            outputter.output(document, System.out);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public ArrayList<String> ReadXml(File file)
    {
        ArrayList<String> msg = new ArrayList<String>();
        SAXBuilder builder = new SAXBuilder();
        File xmlFile = new File(file.getAbsolutePath());
//        UPMC_UI frame = new UPMC_UI();

        int ver = 0;
        boolean modify = false;

        try
        {
            Document document = (Document) builder.build(xmlFile);
            Element rootNode = document.getRootElement();

            String rootName = rootNode.getName();

            if (rootName.equals("upmc") == false)
            {
                int ret = JOptionPane.showConfirmDialog(UPMC_UI.frame, "Migrate SPMC file to UPMC Format?", "Migrate", JOptionPane.YES_NO_OPTION);
                if (ret == JOptionPane.YES_OPTION)
                {
                    modify = true;
                    List serialPort = rootNode.getChildren("SerialPort");
                    Element port = (Element) serialPort.get(0);
                    portSettings.clear();
                    portSettings.add(port.getAttributeValue("PortName"));
                    portSettings.add(port.getAttributeValue("BaudRate"));
                    portSettings.add(port.getAttributeValue("DataBits"));
                    
                    String temp = port.getAttributeValue("StopBits");
                    if (temp.equals("Two"))
                        portSettings.add("2");
                    else if (temp.equals("OnePointFive"))
                        portSettings.add("3");
                    else
                        portSettings.add("1");
                    
                    temp = port.getAttributeValue("Parity");
                    if (temp.equals("None"))
                        portSettings.add("0");
                    else if (temp.equals("Odd"))
                        portSettings.add("1");
                    else if (temp.equals("Even"))
                        portSettings.add("2");

                    temp = port.getAttributeValue("Handshake");
                    if (temp.equals("None"))
                        portSettings.add("0");
                    else if (temp.equals("XOnXoff"))
                        portSettings.add("1");
                    else if (temp.equals("RequestToSend"))
                        portSettings.add("2");

                    temp = port.getAttributeValue("Mode");
                    if (temp.equals("Hex"))
                    {
                        temp = port.getAttributeValue("NSP"); // TCPAddress
                        if(temp.equals("TRUE"))
                            portSettings.add("NSP1");       // Mode
                        else
                            portSettings.add("HEX");       // Mode
                        
                        portSettings.add("false");      // NL
                        portSettings.add("50");         // NLTimeout
                        portSettings.add("1");          // Interface = Serial
                        portSettings.add("0");          // TCPMode = 0 => NULL
                        portSettings.add("127.0.0.1");  // TCPAddress
                        portSettings.add("10001");      // TCPPort
                    }
                    else if (temp.equals("TCP"))
                    {
                        temp = port.getAttributeValue("NSP");   // TCPAddress
                        if(temp.equals("TRUE"))
                            portSettings.add("NSP1");           // Mode
                        else
                            portSettings.add("HEX");            // Mode
                        
                        portSettings.add("false");              // NL
                        portSettings.add("50");                 // NLTimeout
                        portSettings.add("2");                  // Interface = TCP
                        portSettings.add("2");                  // TCPMode = 2 => CLIENT
                        portSettings.add(port.getAttributeValue("HostAddress")); // TCPAddress
                        portSettings.add(port.getAttributeValue("HostPort")); // TCPPort
                    }
                    else if (temp.equals("Text"))
                    {
                        portSettings.add("TEXT");               // Mode
                        
                        temp = port.getAttributeValue("CR");
                        if (temp.equals("TRUE"))
                            portSettings.add("true"); // NL
                        else
                            portSettings.add("false"); // NL

                        portSettings.add("50"); // NLTimeout
                        portSettings.add("1"); // Interface = TCP
                        portSettings.add("0"); // TCPMode = 2 => CLIENT
                        portSettings.add("102.0.0.1"); // TCPAddress
                        portSettings.add("10001"); // TCPPort
                    }
                }
                else
                {
                    JOptionPane.showConfirmDialog(UPMC_UI.frame, "Okay, maybe next time", "Sorry", JOptionPane.PLAIN_MESSAGE);
                    msg.add("Error");
                    return msg;
                }
            }
            else
            {

                List Version = rootNode.getChildren("Versions");

                try
                {
                    if (Version.isEmpty() == false)
                    {
                        Element version = (Element) Version.get(0);
                        ver = Integer.parseInt(version.getAttributeValue("upmVersion"));
                    }
                }
                catch (Exception e)
                {
                    System.out.println("Error reading the Version from the xml saved file");
                }
                finally
                {
                    if (ver < UPM_VERSION)
                    {
                        int ret = JOptionPane.showConfirmDialog(UPMC_UI.frame, "Migrate UPMC file?", "Migrate", JOptionPane.YES_NO_OPTION);
                        if (ret == JOptionPane.YES_OPTION)
                        {
                            modify = true;
                        }
                        else
                        {
                            JOptionPane.showConfirmDialog(UPMC_UI.frame, "I really think you should, maybe next time.", "Sorry", JOptionPane.PLAIN_MESSAGE);
                            msg.add("Error");
                            return msg;
                        }
                    }
                }

                List serialPort = rootNode.getChildren("SerialPort");
                Element port = (Element) serialPort.get(0);
                portSettings.clear();
                portSettings.add(port.getAttributeValue("PortName"));
                portSettings.add(port.getAttributeValue("BaudRate"));
                portSettings.add(port.getAttributeValue("DataBits"));
                portSettings.add(port.getAttributeValue("StopBits"));
                portSettings.add(port.getAttributeValue("Parity"));
                if (modify)
                    portSettings.add("0");
                else
                    portSettings.add(port.getAttributeValue("Handshake"));
                portSettings.add(port.getAttributeValue("Mode"));
                portSettings.add(port.getAttributeValue("NL"));
                portSettings.add(port.getAttributeValue("NLTimeout"));
                portSettings.add(port.getAttributeValue("Interface"));
                portSettings.add(port.getAttributeValue("TCPMode"));
                portSettings.add(port.getAttributeValue("TCPAddress"));
                portSettings.add(port.getAttributeValue("TCPPort"));
            }
            
            List list = rootNode.getChildren("Messages");

            for (int i = 0; i < list.size(); i++)
            {
                Element node = (Element) list.get(i);

                int count = node.getChildren().size();

                for (int loop = 0; loop < count; loop++)
                {
                    msg.add(node.getChildText("Msg" + (loop + 1)));
                }
            }
        }
        catch (IOException io)
        {
            System.out.println(io.getMessage());
        }
        catch (JDOMException jdomex)
        {
            System.out.println(jdomex.getMessage());
        }
        if (modify)
            CreateXml(file, portSettings, msg);
        return msg;
    }
}