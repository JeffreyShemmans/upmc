/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package upmc;

import com.fazecast.jSerialComm.SerialPort;

import java.awt.Component;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout;
import javax.swing.border.CompoundBorder;
import javax.swing.LayoutStyle.ComponentPlacement;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import java.awt.Color;
import javax.swing.border.TitledBorder;
import java.awt.Font;
import javax.swing.border.MatteBorder;
import java.awt.Dimension;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.text.MaskFormatter;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JRadioButton;
import javax.swing.DefaultComboBoxModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComboBox;
import javax.swing.SwingConstants;

/**
 * 
 * @author jeff
 */
public class PortSettings extends javax.swing.JDialog
{
    private String port;
    private int baud = 9600;
    
    // NONE = 0, ODD = 1, EVEN = 2
    private int parity = 0;         
    
    // DATABITS_5 = 5 ... DATABITS_8 = 8
    private int databits = 8;       
    
    // STOPBITS_1 = 1, STOPBITS_1_5 = 3 STOPBITS_2 = 2
    private int stopbits = 1;       
    
    // NONE = 0, HARDWARE = 1, SOFTWARE = 2
    private int flowcontrol = 0;    

    public final static int NULL = 0;
    public final static int TCP_SERVER = 1;
    public final static int TCP_CLIENT = 2;
    public static int tcpMode = TCP_CLIENT;

    private String ipaddress;
    private int tcpport;
    private boolean appendNewLine = true;
    private int appendNewLineTimeout = 50;
    private int interfaceType;
    private CommsManager.ProtocolType protocolType;
    private JRadioButton rbnServer;
    private JRadioButton rbnClient;

    public int getInterfaceType()
    {
        return interfaceType;
    }

    public void setInterfaceType(int value)
    {
        interfaceType = value;
        if (interfaceType == CommsManager.INTERFACE_SERIAL)
        {
            rbnTcp.setSelected(false);
            rbnSerial.setSelected(true);
            SetSerialPanal();
        }
        else
        {
            rbnSerial.setSelected(false);
            rbnTcp.setSelected(true);
            SetTcpPanal();
        }
    }

    public CommsManager.ProtocolType getProtocolType()
    {
        return protocolType;
    }

    public void setProtocolType(CommsManager.ProtocolType value)
    {
        protocolType = value;
        if (protocolType == CommsManager.ProtocolType.HEX)
        {
            rbnHex.setSelected(true);
            rbnText.setSelected(false);
            rbnNsp.setSelected(false);
            rbnNSP2.setSelected(false);

        }
        else if (protocolType == CommsManager.ProtocolType.TEXT)
        {
            rbnHex.setSelected(false);
            rbnText.setSelected(true);
            rbnNsp.setSelected(false);
            rbnNSP2.setSelected(false);
        }
        else if (protocolType == CommsManager.ProtocolType.NSP)
        {
            rbnHex.setSelected(false);
            rbnText.setSelected(false);
            rbnNsp.setSelected(true);
            rbnNSP2.setSelected(false);
        }
        else if (protocolType == CommsManager.ProtocolType.NSP2)
        {
            rbnHex.setSelected(false);
            rbnText.setSelected(false);
            rbnNsp.setSelected(false);
            rbnNSP2.setSelected(true);
        }
    }

    public String getPort()
    {
        return port;
    }

    public void setPort(String value)
    {
        port = value;
        cmbPort.setSelectedItem(port);
    }

    public int getBaud()
    {
        return baud;
    }

    public void setBaud(int value)
    {
        baud = value;
        cmbBaud.setSelectedItem(Integer.toString(value));
    }

    public int getParity()
    {
        return parity;
    }

    public void setParity(int value)
    {
        parity = value;
        cmbParity.setSelectedIndex(value);
    }

    public int getDataBits()
    {
        return databits;
    }

    public void setDataBits(int value)
    {
        databits = value;
        cmbDataBits.setSelectedIndex(value - 5);
    }

    public int getStopBits()
    {
        return stopbits;
    }

    public void setStopBits(int value)
    {
        stopbits = value;
        cmbStopBits.setSelectedIndex(value - 1);
    }
    
    public int getFlowControl()
    {
        return flowcontrol;
    }

    public void setFlowControl(int value)
    {
        flowcontrol = value;
        cmbFlowControl.setSelectedIndex(value);
    }

    public String getTcpAddress()
    {
        return ipaddress;
    }

    public void setTcpAddress(String value)
    {
        ipaddress = value;
        // jComboBox2.setSelectedItem(ipaddress);
    }

    public int getTcpPort()
    {
        return tcpport;
    }

    public void setTcpPort(int value)
    {
        tcpport = value;
    }

    public int getTcpMode()
    {
        return tcpMode;
    }

    public void setTcpMode(int value)
    {
        tcpMode = value;
        if(tcpMode == TCP_CLIENT)
        {
            rbnClient.setSelected(true);
            rbnServer.setSelected(false);
            txfHostIp.setEnabled(true);
        }
        if(tcpMode == TCP_SERVER)
        {
            rbnClient.setSelected(false);
            rbnServer.setSelected(true);
            txfHostIp.setEnabled(false);
        }
    }

    public boolean getAppendNewLine()
    {
        return appendNewLine;
    }

    public void setAppendNewLine(Boolean value)
    {
        appendNewLine = value;
        chckbxAppendNewline.setSelected(value);
    }

    public Integer getAppendNewLineTimeout()
    {
        return appendNewLineTimeout;
    }

    public void setAppendNewLineTimeout(Integer value)
    {
        appendNewLineTimeout = value;
        spnAppendTimeout.setValue(value);
    }

    /**
     * Creates new form PortSettings
     */
    public PortSettings(java.awt.Frame parent, boolean modal)
    {
        super(parent, modal);
        setPreferredSize(new Dimension(420, 300));
        getContentPane().setPreferredSize(new Dimension(500, 275));
        getContentPane().setMaximumSize(new Dimension(600, 275));
        setMaximumSize(new Dimension(600, 300));
        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                disposing();
            }
        });
        setTitle("Port Setup");
        // /* Force the parent to wait for child to dispose */
        // setModalityType(ModalityType.APPLICATION_MODAL);
        initComponents();
        GetSystemPorts();

        Component[] com = jPanel2.getComponents();
        // Inside you action event where you want to disable everything
        // Do the following
        for (int a = 0; a < com.length; a++)
        {
            com[a].setEnabled(false);
        }

        rbnNsp.setSelected(true);
        interfaceType = CommsManager.INTERFACE_SERIAL;
        protocolType = CommsManager.ProtocolType.NSP;
    }

    private void GetSystemPorts()
    {
        int count = 0;
        cmbPort.removeAllItems();

//        int cntr = 0;
        for (SerialPort serialPort : SerialPort.getCommPorts())
        {
            count++;
            String portName = serialPort.getSystemPortName();
            System.out.print("Port " + portName + " ");
            cmbPort.addItem(portName);
        }
        cmbPort.setEnabled(count > 0);
//        System.out.print("Searched through " + cntr + " files");
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents()
    {

        jPanel4 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        txfHostPort = new javax.swing.JTextField();
        txfHostIp = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        rbnSerial = new javax.swing.JRadioButton();
        rbnTcp = new javax.swing.JRadioButton();
        jPanel3 = new javax.swing.JPanel();
        cmbStopBits = new javax.swing.JComboBox();
        cmbStopBits.setPreferredSize(new Dimension(32, 18));
        cmbPort = new javax.swing.JComboBox();
        cmbPort.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                GetSystemPorts();
            }
        });
        cmbPort.setPreferredSize(new Dimension(32, 18));
        jLabel5 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        cmbDataBits = new javax.swing.JComboBox();
        cmbDataBits.setPreferredSize(new Dimension(32, 18));
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        cmbBaud = new javax.swing.JComboBox();
        cmbBaud.setPreferredSize(new Dimension(32, 18));
        jPanel1 = new javax.swing.JPanel();
        rbnText = new javax.swing.JRadioButton();
        rbnNSP2 = new javax.swing.JRadioButton();
        rbnNsp = new javax.swing.JRadioButton();
        rbnHex = new javax.swing.JRadioButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jPanel4.setBorder(new MatteBorder(1, 1, 1, 1, (Color) new Color(0, 0, 0)));

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("TCP"));

        jLabel6.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        jLabel6.setText("IP Addr");

        jLabel7.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        jLabel7.setText("Port");

        txfHostPort.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        txfHostPort.setText("10001");

        txfHostIp.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        txfHostIp.setText("127.0.0.1");

        rbnServer = new JRadioButton("Server");
        rbnServer.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                rbnServer.setSelected(true);
                rbnClient.setSelected(false);
                txfHostIp.setEnabled(false);
                tcpMode = TCP_SERVER;
            }
        });

        rbnClient = new JRadioButton("Client");
        rbnClient.setSelected(true);
        rbnClient.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                rbnServer.setSelected(false);
                rbnClient.setSelected(true);
                txfHostIp.setEnabled(true);
                tcpMode = TCP_CLIENT;
            }
        });

        javax.swing.GroupLayout gl_jPanel2 = new javax.swing.GroupLayout(jPanel2);
        gl_jPanel2.setHorizontalGroup(gl_jPanel2.createParallelGroup(Alignment.LEADING).addGroup(
                gl_jPanel2
                        .createSequentialGroup()
                        .addGroup(
                                gl_jPanel2
                                        .createParallelGroup(Alignment.LEADING)
                                        .addGroup(
                                                gl_jPanel2
                                                        .createSequentialGroup()
                                                        .addGroup(
                                                                gl_jPanel2.createParallelGroup(Alignment.LEADING).addComponent(jLabel6)
                                                                        .addComponent(jLabel7))
                                                        .addPreferredGap(ComponentPlacement.RELATED)
                                                        .addGroup(
                                                                gl_jPanel2.createParallelGroup(Alignment.LEADING)
                                                                        .addComponent(txfHostPort, 89, 89, 89)
                                                                        .addComponent(txfHostIp, 89, 89, 89)))
                                        .addGroup(
                                                gl_jPanel2.createSequentialGroup().addComponent(rbnServer)
                                                        .addPreferredGap(ComponentPlacement.RELATED).addComponent(rbnClient)))
                        .addContainerGap()));
        gl_jPanel2.setVerticalGroup(gl_jPanel2.createParallelGroup(Alignment.LEADING).addGroup(
                gl_jPanel2
                        .createSequentialGroup()
                        .addGap(4)
                        .addGroup(
                                gl_jPanel2
                                        .createParallelGroup(Alignment.BASELINE)
                                        .addComponent(jLabel6)
                                        .addComponent(txfHostIp, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
                                                GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addGroup(
                                gl_jPanel2
                                        .createParallelGroup(Alignment.BASELINE)
                                        .addComponent(jLabel7)
                                        .addComponent(txfHostPort, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
                                                GroupLayout.PREFERRED_SIZE)).addPreferredGap(ComponentPlacement.UNRELATED)
                        .addGroup(gl_jPanel2.createParallelGroup(Alignment.BASELINE).addComponent(rbnServer).addComponent(rbnClient))
                        .addContainerGap(11, Short.MAX_VALUE)));
        jPanel2.setLayout(gl_jPanel2);

        jButton1.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        jButton1.setText("Save");
        jButton1.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jButton1ActionPerformed(evt);
            }
        });

        rbnSerial.setSelected(true);
        rbnSerial.setText("Serial");
        rbnSerial.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jRadioButton1ActionPerformed(evt);
            }
        });

        rbnTcp.setText("TCP");
        rbnTcp.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jRadioButton2ActionPerformed(evt);
            }
        });

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Serial"));

        cmbStopBits.setFont(new java.awt.Font("Dialog", 1, 10));
        cmbStopBits.setModel(new DefaultComboBoxModel(new String[] { "1", "2", "1.5" }));
        cmbStopBits.setMaximumSize(new java.awt.Dimension(64, 22));

        cmbPort.setFont(new java.awt.Font("Dialog", 1, 10));
        cmbPort.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cmbPort.setMaximumSize(new java.awt.Dimension(64, 22));

        jLabel5.setFont(new java.awt.Font("Dialog", 1, 10));
        jLabel5.setText("StopBits");

        jLabel4.setFont(new java.awt.Font("Dialog", 1, 10));
        jLabel4.setText("DataBits");

        cmbDataBits.setFont(new java.awt.Font("Dialog", 1, 10));
        cmbDataBits.setModel(new DefaultComboBoxModel(new String[] { "5", "6", "7", "8" }));
        cmbDataBits.setSelectedIndex(3);
        cmbDataBits.setMaximumSize(new java.awt.Dimension(64, 22));

        jLabel1.setFont(new java.awt.Font("Dialog", 1, 10));
        jLabel1.setText("Port");

        jLabel2.setFont(new java.awt.Font("Dialog", 1, 10));
        jLabel2.setText("Baudrate");

        cmbBaud.setFont(new java.awt.Font("Dialog", 1, 10));
        cmbBaud.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1200", "2400", "4800", "9600", "19200", "38400", "57600",
                "115200" }));
        cmbBaud.setSelectedIndex(3);
        cmbBaud.setMaximumSize(new java.awt.Dimension(64, 22));
        
        cmbFlowControl = new JComboBox();
        cmbFlowControl.setModel(new DefaultComboBoxModel(new String[] {"NONE", "HARDWARE", "SOFTWARE"}));
        cmbFlowControl.setPreferredSize(new Dimension(32, 18));
        cmbFlowControl.setMaximumSize(new Dimension(64, 22));
        cmbFlowControl.setFont(new Font("Dialog", Font.BOLD, 10));
        
        JLabel lblFlowcontrol = new JLabel();
        lblFlowcontrol.setText("FlowCtrl");
        lblFlowcontrol.setFont(new Font("Dialog", Font.BOLD, 10));
        // cmbParity = new javax.swing.JComboBox();
        cmbParity = new javax.swing.JComboBox<String>();
        cmbParity.setPreferredSize(new Dimension(32, 18));
        
                cmbParity.setFont(new java.awt.Font("Dialog", 1, 10));
                cmbParity.setModel(new DefaultComboBoxModel(new String[] { "NONE", "ODD", "EVEN" }));
                cmbParity.setMaximumSize(new java.awt.Dimension(64, 22));
        jLabel3 = new javax.swing.JLabel();
        
                jLabel3.setFont(new java.awt.Font("Dialog", 1, 10));
                jLabel3.setText("Parity");

        javax.swing.GroupLayout gl_jPanel3 = new javax.swing.GroupLayout(jPanel3);
        gl_jPanel3.setHorizontalGroup(
            gl_jPanel3.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_jPanel3.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(gl_jPanel3.createParallelGroup(Alignment.LEADING)
                        .addComponent(jLabel1)
                        .addComponent(jLabel2)
                        .addComponent(jLabel4)
                        .addComponent(jLabel3)
                        .addComponent(jLabel5)
                        .addComponent(lblFlowcontrol, GroupLayout.PREFERRED_SIZE, 47, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(ComponentPlacement.UNRELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(gl_jPanel3.createParallelGroup(Alignment.LEADING, false)
                        .addComponent(cmbPort, Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 116, GroupLayout.PREFERRED_SIZE)
                        .addComponent(cmbBaud, Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 116, GroupLayout.PREFERRED_SIZE)
                        .addComponent(cmbDataBits, Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 116, GroupLayout.PREFERRED_SIZE)
                        .addComponent(cmbParity, Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 116, GroupLayout.PREFERRED_SIZE)
                        .addComponent(cmbStopBits, Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 116, GroupLayout.PREFERRED_SIZE)
                        .addComponent(cmbFlowControl, Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 116, GroupLayout.PREFERRED_SIZE))
                    .addContainerGap())
        );
        gl_jPanel3.setVerticalGroup(
            gl_jPanel3.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_jPanel3.createSequentialGroup()
                    .addGroup(gl_jPanel3.createParallelGroup(Alignment.BASELINE)
                        .addComponent(jLabel1)
                        .addComponent(cmbPort, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(gl_jPanel3.createParallelGroup(Alignment.BASELINE)
                        .addComponent(cmbBaud, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel2))
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(gl_jPanel3.createParallelGroup(Alignment.BASELINE)
                        .addComponent(cmbDataBits, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel4))
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(gl_jPanel3.createParallelGroup(Alignment.BASELINE)
                        .addComponent(cmbParity, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel3))
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(gl_jPanel3.createParallelGroup(Alignment.BASELINE)
                        .addComponent(cmbStopBits, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel5))
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(gl_jPanel3.createParallelGroup(Alignment.BASELINE)
                        .addComponent(cmbFlowControl, GroupLayout.PREFERRED_SIZE, 18, GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblFlowcontrol, GroupLayout.PREFERRED_SIZE, 13, GroupLayout.PREFERRED_SIZE))
                    .addContainerGap())
        );
        gl_jPanel3.linkSize(SwingConstants.HORIZONTAL, new Component[] {cmbStopBits, cmbPort, cmbDataBits, cmbBaud, cmbFlowControl, cmbParity});
        jPanel3.setLayout(gl_jPanel3);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Protocol"));

        rbnText.setFont(new java.awt.Font("Dialog", 1, 10));
        rbnText.setText("TEXT");
        rbnText.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jRadioButton4ActionPerformed(evt);
            }
        });

        rbnNSP2.setFont(new java.awt.Font("Dialog", 1, 10));
        rbnNSP2.setText("NSP2");
        rbnNSP2.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jRadioButton5ActionPerformed(evt);
            }
        });

        rbnNsp.setFont(new java.awt.Font("Dialog", 1, 10));
        rbnNsp.setText("NSP");
        rbnNsp.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jRadioButton8ActionPerformed(evt);
            }
        });

        rbnHex.setFont(new java.awt.Font("Dialog", 1, 10));
        rbnHex.setText("HEX");
        rbnHex.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jRadioButton3ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout gl_jPanel1 = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(gl_jPanel1);
        gl_jPanel1.setHorizontalGroup(gl_jPanel1.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(
                gl_jPanel1
                        .createSequentialGroup()
                        .addGroup(
                                gl_jPanel1
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(
                                                gl_jPanel1.createSequentialGroup().addComponent(rbnHex)
                                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                        .addComponent(rbnText))
                                        .addGroup(
                                                gl_jPanel1.createSequentialGroup().addComponent(rbnNsp)
                                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                        .addComponent(rbnNSP2))).addGap(0, 17, Short.MAX_VALUE)));
        gl_jPanel1.setVerticalGroup(gl_jPanel1.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(
                gl_jPanel1
                        .createSequentialGroup()
                        .addGroup(
                                gl_jPanel1.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(rbnHex)
                                        .addComponent(rbnText))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(
                                gl_jPanel1.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(rbnNSP2)
                                        .addComponent(rbnNsp))));

        panel_1 = new JPanel();
        panel_1.setBorder(new TitledBorder(new LineBorder(new Color(184, 207, 229)), "Newline", TitledBorder.LEADING, TitledBorder.TOP,
                null, null));

        javax.swing.GroupLayout gl_jPanel4 = new javax.swing.GroupLayout(jPanel4);
        gl_jPanel4.setHorizontalGroup(
            gl_jPanel4.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_jPanel4.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(gl_jPanel4.createParallelGroup(Alignment.TRAILING)
                        .addComponent(panel_1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel3, GroupLayout.PREFERRED_SIZE, 207, Short.MAX_VALUE)
                        .addGroup(gl_jPanel4.createSequentialGroup()
                            .addComponent(rbnSerial)
                            .addGap(138)))
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(gl_jPanel4.createParallelGroup(Alignment.LEADING)
                        .addComponent(jPanel1, GroupLayout.DEFAULT_SIZE, 179, Short.MAX_VALUE)
                        .addGroup(gl_jPanel4.createSequentialGroup()
                            .addComponent(rbnTcp)
                            .addGap(113))
                        .addComponent(jPanel2, GroupLayout.DEFAULT_SIZE, 179, Short.MAX_VALUE)
                        .addComponent(jButton1))
                    .addGap(22))
        );
        gl_jPanel4.setVerticalGroup(
            gl_jPanel4.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_jPanel4.createSequentialGroup()
                    .addGap(10)
                    .addGroup(gl_jPanel4.createParallelGroup(Alignment.LEADING, false)
                        .addGroup(gl_jPanel4.createSequentialGroup()
                            .addComponent(rbnTcp)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(jPanel2, GroupLayout.PREFERRED_SIZE, 96, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(jPanel1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                        .addGroup(gl_jPanel4.createSequentialGroup()
                            .addComponent(rbnSerial)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(jPanel3, 0, 0, Short.MAX_VALUE)))
                    .addPreferredGap(ComponentPlacement.RELATED, 3, Short.MAX_VALUE)
                    .addGroup(gl_jPanel4.createParallelGroup(Alignment.TRAILING)
                        .addComponent(panel_1, GroupLayout.PREFERRED_SIZE, 46, GroupLayout.PREFERRED_SIZE)
                        .addComponent(jButton1))
                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        panel_1.setLayout(null);

        chckbxAppendNewline = new JCheckBox("Append After");
        chckbxAppendNewline.setToolTipText("Has no effect in NSP mode");
        chckbxAppendNewline.setSelected(true);
        chckbxAppendNewline.setFont(new Font("Dialog", Font.PLAIN, 10));
        chckbxAppendNewline.setBounds(8, 13, 95, 23);
        panel_1.add(chckbxAppendNewline);

        spnAppendTimeout = new JSpinner();
        spnAppendTimeout.setBounds(111, 14, 41, 20);
        panel_1.add(spnAppendTimeout);
        spnAppendTimeout.setModel(new SpinnerNumberModel(50, 1, 100, 1));

        JLabel lblMs = new JLabel("mS");
        lblMs.setBounds(158, 16, 34, 15);
        panel_1.add(lblMs);
        jPanel4.setLayout(gl_jPanel4);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING).addComponent(jPanel4, GroupLayout.DEFAULT_SIZE, 410,
                Short.MAX_VALUE));
        layout.setVerticalGroup(layout.createParallelGroup(Alignment.LEADING).addGroup(
                layout.createSequentialGroup().addComponent(jPanel4, GroupLayout.DEFAULT_SIZE, 258, Short.MAX_VALUE).addContainerGap()));
        getContentPane().setLayout(layout);

        pack();
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt)
    {
        disposing();
    }

    private void jRadioButton1ActionPerformed(java.awt.event.ActionEvent evt)
    {
        rbnTcp.setSelected(false); /* Serial selected */
        rbnSerial.setSelected(true);
        interfaceType = CommsManager.INTERFACE_SERIAL;

        SetSerialPanal();
    }

    /**
     * 
     */
    private void SetSerialPanal()
    {
        Component[] com = jPanel2.getComponents();
        // Inside you action event where you want to disable everything
        // Do the following
        for (int a = 0; a < com.length; a++)
        {
            com[a].setEnabled(false);
        }

        com = jPanel3.getComponents();
        // Inside you action event where you want to disable everything
        // Do the following
        for (int a = 0; a < com.length; a++)
        {
            com[a].setEnabled(true);
        }
    }

    private void jRadioButton2ActionPerformed(java.awt.event.ActionEvent evt)
    {
        rbnSerial.setSelected(false); /* TCP Selected */
        rbnTcp.setSelected(true);
        interfaceType = CommsManager.INTERFACE_TCP;

        SetTcpPanal();
    }

    /**
     * 
     */
    private void SetTcpPanal()
    {
        Component[] com = jPanel3.getComponents();
        // Inside you action event where you want to disable everything
        // Do the following
        for (int a = 0; a < com.length; a++)
        {
            com[a].setEnabled(false);
        }

        com = jPanel2.getComponents();
        // Inside you action event where you want to disable everything
        // Do the following
        for (int a = 0; a < com.length; a++)
        {
            com[a].setEnabled(true);
        }
    }

    private void jRadioButton3ActionPerformed(java.awt.event.ActionEvent evt)
    {
        rbnText.setSelected(false); /* HEX Selected */
        rbnNSP2.setSelected(false);
        rbnNsp.setSelected(false);
        rbnHex.setSelected(true);
        protocolType = protocolType.HEX;
    }

    private void jRadioButton4ActionPerformed(java.awt.event.ActionEvent evt)
    {
        rbnHex.setSelected(false); /* TEXT Selected */
        rbnNSP2.setSelected(false);
        rbnNsp.setSelected(false);
        rbnText.setSelected(true);
        protocolType = protocolType.TEXT;
    }

    private void jRadioButton5ActionPerformed(java.awt.event.ActionEvent evt)
    {
        rbnHex.setSelected(false); /* NSP2 Selected */
        rbnText.setSelected(false);
        rbnNsp.setSelected(false);
        rbnNSP2.setSelected(true);
        protocolType = protocolType.NSP2;
    }

    private void jRadioButton8ActionPerformed(java.awt.event.ActionEvent evt)
    {
        rbnHex.setSelected(false); /* NSP Selected */
        rbnText.setSelected(false);
        rbnNSP2.setSelected(false);
        rbnNsp.setSelected(true);
        protocolType = protocolType.NSP;
    }

    private void disposing()
    {
        if (cmbPort.getItemCount() == 0)
            port = "None";
        else
            port = cmbPort.getSelectedItem().toString();

        // Parity -> NONE = 0, ODD = 1, EVEN = 2
        // SerialPort.STOPBITS_1 = 1, STOPBITS_1_5 = 3 STOPBITS_2 = 2

        baud = Integer.parseInt(cmbBaud.getSelectedItem().toString());
        parity = cmbParity.getSelectedIndex();
        databits = Integer.parseInt(cmbDataBits.getSelectedItem().toString());
        stopbits = cmbStopBits.getSelectedIndex() + 1;
        flowcontrol = cmbFlowControl.getSelectedIndex();

        ipaddress = txfHostIp.getText();
        tcpport = Integer.parseInt(txfHostPort.getText());

        appendNewLine = chckbxAppendNewline.isSelected();
        appendNewLineTimeout = (Integer) spnAppendTimeout.getValue();

        dispose();
    }

    // /**
    // * @param args the command line arguments
    // */
    // public static void main(String args[]) {
    // /*
    // * Set the Nimbus look and feel
    // */
    // //<editor-fold defaultstate="collapsed"
    // desc=" Look and feel setting code (optional) ">
    // /*
    // * If Nimbus (introduced in Java SE 6) is not available, stay with the
    // * default look and feel. For details see
    // *
    // http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
    // */
    // try {
    // for (javax.swing.UIManager.LookAndFeelInfo info :
    // javax.swing.UIManager.getInstalledLookAndFeels()) {
    // if ("Nimbus".equals(info.getName())) {
    // javax.swing.UIManager.setLookAndFeel(info.getClassName());
    // break;
    // }
    // }
    // } catch (ClassNotFoundException ex) {
    // java.util.logging.Logger.getLogger(PortSettings.class.getName()).log(java.util.logging.Level.SEVERE,
    // null, ex);
    // } catch (InstantiationException ex) {
    // java.util.logging.Logger.getLogger(PortSettings.class.getName()).log(java.util.logging.Level.SEVERE,
    // null, ex);
    // } catch (IllegalAccessException ex) {
    // java.util.logging.Logger.getLogger(PortSettings.class.getName()).log(java.util.logging.Level.SEVERE,
    // null, ex);
    // } catch (javax.swing.UnsupportedLookAndFeelException ex) {
    // java.util.logging.Logger.getLogger(PortSettings.class.getName()).log(java.util.logging.Level.SEVERE,
    // null, ex);
    // }
    // //</editor-fold>
    //
    // /*
    // * Create and display the dialog
    // */
    // java.awt.EventQueue.invokeLater(new Runnable() {
    //
    // public void run() {
    // PortSettings dialog = new PortSettings(new javax.swing.JFrame(), true);
    // dialog.addWindowListener(new java.awt.event.WindowAdapter() {
    //
    // @Override
    // public void windowClosing(java.awt.event.WindowEvent e) {
    // System.exit(0);
    // }
    // });
    // dialog.setVisible(true);
    // }
    // });
    // }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JComboBox cmbPort;
    private javax.swing.JComboBox cmbBaud;
    private javax.swing.JComboBox cmbParity;
    private javax.swing.JComboBox cmbDataBits;
    private javax.swing.JComboBox cmbStopBits;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JRadioButton rbnSerial;
    private javax.swing.JRadioButton rbnTcp;
    private javax.swing.JRadioButton rbnHex;
    private javax.swing.JRadioButton rbnText;
    private javax.swing.JRadioButton rbnNSP2;
    private javax.swing.JRadioButton rbnNsp;
    private javax.swing.JTextField txfHostPort;
    private javax.swing.JTextField txfHostIp;
    private JCheckBox chckbxAppendNewline;
    private JSpinner spnAppendTimeout;
    private JPanel panel_1;
    private JComboBox cmbFlowControl;
}
