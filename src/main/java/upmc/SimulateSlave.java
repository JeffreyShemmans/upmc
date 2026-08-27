package upmc;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.text.TabExpander;
import javax.swing.border.TitledBorder;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import java.awt.GridLayout;
import javax.swing.JButton;

import java.awt.Event;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.border.LineBorder;
import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.EventObject;
import java.util.List;

import javax.swing.ScrollPaneConstants;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.JLabel;

import javax.swing.event.*;
import javax.swing.JCheckBox;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class SimulateSlave extends JFrame
{
    private JPanel contentPane;
    private JTable table;
    private JCheckBox chckbxEnableSimulator;

    private JComboBox<String> cmbReceive = new JComboBox<String>();
    private JComboBox<String> cmbTransmit = new JComboBox<String>();
    private JLabel lblStatus;
    final DefaultTableModel model;

    // TableColumn receiveColumn = table.getColumnModel().getColumn(0);
    // TableColumn transmitColumn = table.getColumnModel().getColumn(1);
    TableColumn receiveColumn;
    TableColumn transmitColumn;
    boolean simEnable = false;

    private PropertyChangeSupport propChangeSupport = new PropertyChangeSupport(this); // Properties

    public JTable getDataSet()
    {
        return table;
    }

    public void setDataSet(JTable value)
    {
        table = value;
    }

    public boolean getSimSlaveEnable()
    {
        return simEnable;
    }

    public void setSimSlaveEnablr(Boolean value)
    {
        simEnable = value;
    }

    public void setNewMessages(ArrayList<String> value)
    {
        cmbReceive.removeAllItems();
        cmbTransmit.removeAllItems();
        for (String msg : value)
        {
            cmbReceive.addItem(msg);
            cmbTransmit.addItem(msg);
        }
    }

    /**
     * Add a property listener for the named property.
     * 
     * @param property
     *            the sole property name for which to register
     * @param listener
     *            the property change listener
     */
    public synchronized void addPropertyChangeListener(String property, PropertyChangeListener listener)
    {
        propChangeSupport.addPropertyChangeListener(property, listener);
    }

    /**
     * Remove a property listener.
     * 
     * @param listener
     *            the property change listener
     */
    public synchronized void removePropertyChangeListener(PropertyChangeListener listener)
    {
        propChangeSupport.removePropertyChangeListener(listener);
    }

    /**
     * Remove a property listener for the named property.
     * 
     * @param property
     *            the sole property name for which to stop receiving events
     * @param listener
     *            the property change listener
     */
    public synchronized void removePropertyChangeListener(String property, PropertyChangeListener listener)
    {
        propChangeSupport.removePropertyChangeListener(property, listener);
    }

    /**
     * Create the frame.
     */
    public SimulateSlave()// java.awt.Frame parent, boolean modal)
    {
        // super(parent, modal);

        // setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setBounds(100, 100, 773, 288);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(new LineBorder(new Color(184, 207, 229)), "Setup", TitledBorder.LEADING, TitledBorder.TOP, null,
                null));
        panel.setLayout(new GridLayout(1, 0, 0, 0));

        JScrollPane scrollPane = new JScrollPane();
        panel.add(scrollPane);

        model = new DefaultTableModel();
        table = new JTable(model);
        table.setFont(new Font("Dialog", Font.BOLD, 10));

        // Create a couple of columns
        model.addColumn("Receive");
        model.addColumn("Transmit");

        // Append a row
        model.addRow(new Object[] { "", "" });
        // there are now 2 rows with 2 columns

        table.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseReleased(MouseEvent e)
            {
                int viewRow = table.getSelectedRow();
                int viewCol = table.getSelectedColumn();

                // lblStatus.setText(String.format("Selected Row %d. " + "Selected Column %d.", viewRow, viewCol));
                // if((cmbReceive.getSelectedIndex() > 0)
            }
        });

        // table.setModel(new DefaultTableModel(new Object[][] { { null, null }, }, new String[] { "Receive", "Transmit" }));
        scrollPane.setViewportView(table);

        chckbxEnableSimulator = new JCheckBox("Enable");
        chckbxEnableSimulator.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                int loopRx;
                for (loopRx = 0; loopRx < table.getRowCount(); loopRx++)
                {
                    if ((table.getValueAt(loopRx, 0) == "") || (table.getValueAt(loopRx, 0) == null))
                        break;
                }
                int loopTx;
                for (loopTx = 0; loopTx < table.getRowCount(); loopTx++)
                {
                    if ((table.getValueAt(loopTx, 1) == "") || (table.getValueAt(loopTx, 1) == null))
                        break;
                }

                if ((loopRx == 0) && (loopTx == 0))
                {
                    lblStatus.setText("Nothing to update");
                }
                else if (loopRx != loopTx)
                {
                    lblStatus.setText("Qty Error");
                    simEnable = false;
                    chckbxEnableSimulator.setSelected(false);
                    return;
                }
                else
                {
                    if (chckbxEnableSimulator.isSelected())
                        simEnable = true;
                    else
                        simEnable = false;
                    
                    propChangeSupport.firePropertyChange("UpdateButton", "New", "Old");
                    lblStatus.setText("Successful");
                }
            }
        });
//        btnUpdate.addActionListener(new ActionListener()
//        {
//            public void actionPerformed(ActionEvent e)
//            {
//                int loopRx;
//                for (loopRx = 0; loopRx < table.getRowCount(); loopRx++)
//                {
//                    if ((table.getValueAt(loopRx, 0) == "") || (table.getValueAt(loopRx, 0) == null))
//                        break;
//                }
//                int loopTx;
//                for (loopTx = 0; loopTx < table.getRowCount(); loopTx++)
//                {
//                    if ((table.getValueAt(loopTx, 1) == "") || (table.getValueAt(loopTx, 1) == null))
//                        break;
//                }
//
//                if ((loopRx == 0) && (loopTx == 0))
//                {
//                    lblStatus.setText("Nothing to update");
//                }
//                else if (loopRx != loopTx)
//                {
//                    lblStatus.setText("Qty Error");
//                }
//                else
//                {
//                    propChangeSupport.firePropertyChange("UpdateButton", "New", "Old");
//                    lblStatus.setText("Successful");
//                }
//            }
//        });

        lblStatus = new JLabel("Status");
        lblStatus.setFont(new Font("Dialog", Font.BOLD, 9));

        JButton btnSave = new JButton("Save");
        btnSave.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                saveSimSetup();
            }
        });
        btnSave.setFont(new Font("Dialog", Font.BOLD, 9));

        JButton btnLoad = new JButton("Load");
        btnLoad.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                loadSimSetup();
            }
        });
        btnLoad.setFont(new Font("Dialog", Font.BOLD, 9));

        JButton btnAddLine = new JButton("Add Line");
        btnAddLine.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                model.addRow(new Object[] { "", "" });
                if(chckbxEnableSimulator.isSelected())
                    chckbxEnableSimulator.setSelected(false);
            }
        });
        btnAddLine.setFont(new Font("Dialog", Font.BOLD, 9));

        JButton btnDelLine = new JButton("Del Line");
        btnDelLine.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                table.requestFocus();
                table.setFocusable(true);
                
                int rows = model.getRowCount();
                
                if (rows > 1)
                {
                    model.removeRow(model.getRowCount()-1);
                }
                if(chckbxEnableSimulator.isSelected())
                    propChangeSupport.firePropertyChange("UpdateButton", "New", "Old");
            }
        });
        btnDelLine.setFont(new Font("Dialog", Font.BOLD, 9));
        
        JButton btnClrLines = new JButton("Clr Lines");
        btnClrLines.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                model.getDataVector().removeAllElements();
                table.repaint();
                simEnable = false;
                chckbxEnableSimulator.setSelected(false);
                propChangeSupport.firePropertyChange("UpdateButton", "New", "Old");
            }
        });
        btnClrLines.setFont(new Font("Dialog", Font.BOLD, 9));
        GroupLayout gl_contentPane = new GroupLayout(contentPane);
        gl_contentPane.setHorizontalGroup(
            gl_contentPane.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_contentPane.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
                        .addComponent(panel, GroupLayout.DEFAULT_SIZE, 733, Short.MAX_VALUE)
                        .addGroup(gl_contentPane.createSequentialGroup()
                            .addComponent(btnAddLine)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(btnDelLine)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(btnClrLines)
                            .addPreferredGap(ComponentPlacement.RELATED, 290, Short.MAX_VALUE)
                            .addComponent(btnSave)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(btnLoad)
                            .addPreferredGap(ComponentPlacement.UNRELATED)
                            .addComponent(chckbxEnableSimulator))
                        .addComponent(lblStatus))
                    .addContainerGap())
        );
        gl_contentPane.setVerticalGroup(
            gl_contentPane.createParallelGroup(Alignment.TRAILING)
                .addGroup(gl_contentPane.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(panel, GroupLayout.DEFAULT_SIZE, 193, Short.MAX_VALUE)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(gl_contentPane.createParallelGroup(Alignment.BASELINE)
                        .addComponent(btnAddLine)
                        .addComponent(btnDelLine)
                        .addComponent(btnClrLines)
                        .addComponent(btnSave)
                        .addComponent(btnLoad)
                        .addComponent(chckbxEnableSimulator))
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(lblStatus))
        );
        contentPane.setLayout(gl_contentPane);

        receiveColumn = table.getColumnModel().getColumn(0);
        transmitColumn = table.getColumnModel().getColumn(1);
        receiveColumn.setCellEditor(new DefaultCellEditor(cmbReceive));
        transmitColumn.setCellEditor(new DefaultCellEditor(cmbTransmit));
    }

    private void saveSimSetup()
    {
        final JFileChooser fileChooser = new JFileChooser();
        ChooserFilter chooserFilter = new ChooserFilter();

        chooserFilter.setFileType(ChooserFilter.FILE_SIM);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.addChoosableFileFilter(chooserFilter);

        fileChooser.setFileView(new ChooserFileView()); // Add custom icons for file types.
        fileChooser.setAccessory(new ImagePreview(fileChooser)); // Add the preview pane.

        int returnVal = fileChooser.showSaveDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION)
        {
            File file = fileChooser.getSelectedFile();

            Document document = new Document();
            Element root = new Element("upmc_sim");

            //
            // Creating a child for the root element. Here we can see how to
            // set the text of an xml element.
            //
            Element msgChild = new Element("Messages");
            for (int loop = 0; loop < table.getRowCount(); loop++)
            {
                if (table.getValueAt(loop, 0) != null)
                {
                    msgChild.addContent(new Element("Msg" + (loop + 1)).setText(table.getValueAt(loop, 0).toString() + ","
                            + table.getValueAt(loop, 1).toString()));
                }
            }

            //
            // Add the child to the root element and add the root element as
            // the document content.
            //
            root.addContent(msgChild);
            document.setContent(root);

            try
            {
                FileWriter writer;
                if (file.getName().endsWith(".sim"))
                {
                    writer = new FileWriter(file.getAbsoluteFile());
                }
                else
                {
                    writer = new FileWriter(file.getAbsoluteFile() + ".sim");
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
                writer.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    private void loadSimSetup()
    {
        ChooserFilter chooserFilter = new ChooserFilter();

        final JFileChooser fileChooser = new JFileChooser();
        chooserFilter.setFileType(ChooserFilter.FILE_SIM);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.addChoosableFileFilter(chooserFilter);

        fileChooser.setFileView(new ChooserFileView()); // Add custom icons for file types.

        fileChooser.setAccessory(new ImagePreview(fileChooser)); // Add the preview pane.

        int returnVal = fileChooser.showOpenDialog(this); // In response to a button click:

        if (returnVal == JFileChooser.APPROVE_OPTION)
        {
            File file = fileChooser.getSelectedFile();
            
            model.getDataVector().removeAllElements();

            SAXBuilder builder = new SAXBuilder();
            File xmlFile = new File(file.getAbsolutePath());

            try
            {
                Document document = (Document) builder.build(xmlFile);
                Element rootNode = document.getRootElement();

                List list = rootNode.getChildren("Messages");

                for (int i = 0; i < list.size(); i++)
                {
                    Element node = (Element) list.get(i);

                    int count = node.getChildren().size();
                    for(int loop = 0; loop < count; loop++)
                        model.addRow(new Object[] { "", "" });

                    for (int loop = 0; loop < count; loop++)
                    {
                        String msg[] = node.getChildText("Msg" + (loop + 1)).split(",");
                        table.setValueAt(msg[0], loop, 0);
                        table.setValueAt(msg[1], loop, 1);
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
        }
    }
}
