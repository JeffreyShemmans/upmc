package upmc;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.border.TitledBorder;
import javax.swing.LayoutStyle.ComponentPlacement;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JSlider;
import javax.swing.Timer;

import javax.swing.JSeparator;
import java.awt.Font;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseListener;

import java.io.Console;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.CookieHandler;
import java.util.ArrayList;
import javax.swing.JTextArea;

import upmc.CommsManager.ProtocolType;
import upmc.TcpServer.State;

import javax.swing.JScrollPane;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.EventListenerList;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
//import org.eclipse.wb.swing.FocusTraversalOnArray;
import java.awt.Component;
import javax.swing.ImageIcon;
import java.awt.Toolkit;
import javax.swing.UIManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.ComponentAdapter;
import javax.swing.JCheckBoxMenuItem;

public class UPMC_UI extends JFrame
{

	private CommsManager commsManager;
	private PortSettings portSettings;
	private SimulateSlave simSlave;
	private ExtraViewer extraViewerFrame;

	private ArrayList<String> messages = new ArrayList<String>();
	private int msgCounter = 0;

	private JPanel contentPane;
	private JTextField txfNewMessage;
	private JTextField txfHeader;
	private JTextField txfHCS;
	private JTextField txfBody;
	private JTextField txfBCS;
	private JLabel lblStatus;
	private JLabel lblMessageName;
	private JLabel lblHeader;
	private JLabel lblHCS;
	private JLabel lblBody;
	private JLabel lblBCS;
	private JLabel lblPort;
	private JLabel lblRepeatInfo;

	private JTextArea txaTransmit;
	private JTextArea txaReceive;
	private JButton btnOpenPort;
	private JButton btnSendData;
	private JButton btnLogData;

	private JSlider slider;

	private Timer timer1;

	private int repeatVal = 1010;
	private boolean fileUpToDate = true;

	private JComboBox<String> comboBox;
	// private JComboBox comboBox;

	private File savedFile = null;
	// private File saveFileLog = null;
	private XmlParser xmlParser = new XmlParser();

	private JPopupMenu popup;
	private JMenuItem menuItemClear;
	private JCheckBoxMenuItem chkExtraViewer;
	public static UPMC_UI frame;

	private enum PopupSource
	{
		TxArea, RxArea, NONE
	};

	private PopupSource popupSource = PopupSource.NONE;

	public class MessageData
	{
		int tcpMode = PortSettings.TCP_CLIENT;

		int interfaceType;
		CommsManager.ProtocolType protocolType;

		String MsgName;
		String Header;
		String HeaderCS;
		String Body;
		String BodyCS;

		boolean logToFile = false;
		File saveFileLog = null;
		FileWriter logfile;

		boolean appendNewline = false;
		int appendNewLineTimeout = 50;
		
		boolean extraViewerEnabled = false;
	}

	MessageData msgData = new MessageData();

	class PopupListener extends MouseAdapter
	{
		public void mousePressed(MouseEvent e)
		{
			maybeShowPopup(e);
		}

		public void mouseReleased(MouseEvent e)
		{
			maybeShowPopup(e);
		}

		private void maybeShowPopup(MouseEvent e)
		{
			if (e.isPopupTrigger())
			{
				popup.show(e.getComponent(), e.getX(), e.getY());
				if (e.getComponent().equals(txaTransmit))
				{
					popupSource = PopupSource.TxArea;
					// lblStatus.setText("txaTransmit clicked X:" + e.getX() +
					// " Y:" + e.getY());
					// e.getComponent().repaint();
				}
				else if (e.getComponent().equals(txaReceive))
				{
					popupSource = PopupSource.RxArea;
					// lblStatus.setText("txaReceive clicked");
				}
			}
		}
	}

	/**
	 * Launch the application.
	 */
	public static void main(String[] args)
	{
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				try
				{
					// UPMC_UI frame = new UPMC_UI();
					frame = new UPMC_UI();
					frame.setVisible(true);
					frame.txfNewMessage.requestFocus();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		});
	}

	private void disposing()
	{
		int ret = cleanup();

		if (ret > 0)
			frame.dispose();
	}

	/**
     * 
     */
	private int cleanup()
	{
		repeatVal = 1010;
		slider.setValue(0);
		timer1.stop();

		if (commsManager.isConnected())
		{
			commsManager.disconnect();
			btnOpenPort.setText("Open");
			btnSendData.setEnabled(false);
			btnSendData.setText("Send");
		}

		try
		{
			if (msgData.logfile != null)
				msgData.logfile.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return -1;
		}

		try
		{
			if (simSlave != null)
				simSlave.dispose();

			if (extraViewerFrame != null)
				extraViewerFrame.dispose();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return -1;
		}

		if (fileUpToDate == false)
		{
			int ret = JOptionPane.showConfirmDialog(this, "Save Messages?", "Save", JOptionPane.YES_NO_CANCEL_OPTION);
			if (ret == JOptionPane.YES_OPTION)
			{
				saveFile();
				return 1;
			}
			else if (ret == JOptionPane.CANCEL_OPTION)
			{
				return 0;
			}
		}
		return 2;
	}

	/**
	 * Create the frame.
	 */
	public UPMC_UI()
	{
		// addComponentListener(new ComponentAdapter()
		// {
		// @Override
		// public void componentMoved(ComponentEvent e)
		// {
		//
		// if (simSlave != null)
		// {
		// Point points = frame.getLocation();
		// simSlave.setLocation(points.x, points.y + contentPane.getHeight() +
		// 45);
		// }
		// }
		// });
		setIconImage(Toolkit.getDefaultToolkit().getImage(UPMC_UI.class.getResource("/upmc/images/upmc1.png")));
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent arg0)
			{
				disposing();
			}
		});
		setTitle("Universal Port Message Compiler");
		// setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setBounds(100, 100, 773, 371);

		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);

		JMenuItem mntmNew = new JMenuItem("New");
		mntmNew.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent arg0)
			{
				ClearAll();
			}
		});
		mntmNew.setIcon(new ImageIcon(UPMC_UI.class.getResource("/upmc/images/new1.png")));
		mnFile.add(mntmNew);

		JMenuItem mntmOpen = new JMenuItem("Open");
		mntmOpen.setIcon(new ImageIcon(UPMC_UI.class.getResource("/upmc/images/open1.png")));
		mntmOpen.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent arg0)
			{
				openFile();
			}
		});

		JSeparator separator_2 = new JSeparator();
		mnFile.add(separator_2);
		mnFile.add(mntmOpen);

		JMenuItem mntmSave = new JMenuItem("Save");
		mntmSave.setIcon(new ImageIcon(UPMC_UI.class.getResource("/upmc/images/save1.png")));
		mntmSave.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent arg0)
			{
				saveFile();
			}
		});

		JSeparator separator = new JSeparator();
		mnFile.add(separator);
		mnFile.add(mntmSave);

		JMenuItem mntmSaveas = new JMenuItem("SaveAs");
		mntmSaveas.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent arg0)
			{
				saveAsFile();
			}
		});
		mnFile.add(mntmSaveas);

		JMenuItem mntmExit = new JMenuItem("Exit");
		mntmExit.setIcon(new ImageIcon(UPMC_UI.class.getResource("/upmc/images/exit1.png")));
		mntmExit.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent arg0)
			{
				disposing();
				// System.exit(NORMAL);
			}
		});

		JSeparator separator_1 = new JSeparator();
		mnFile.add(separator_1);
		mnFile.add(mntmExit);

		JMenu Port = new JMenu("Port");
		menuBar.add(Port);

		JMenuItem mntmSetup = new JMenuItem("Setup");
		mntmSetup.setIcon(new ImageIcon(UPMC_UI.class.getResource("/upmc/images/setup1.png")));
		mntmSetup.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent arg0)
			{
				openPortSettings();
			}
		});
		Port.add(mntmSetup);

		JMenu mnSimulate = new JMenu("Extra");
		menuBar.add(mnSimulate);

		JMenuItem mntmSlaveDevice = new JMenuItem("Simulate");
		mntmSlaveDevice.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent arg0)
			{

				// if (comboBox.getItemCount() > 0)
				// {
				if (simSlave == null)
				{
					simSlave = new SimulateSlave();

					simSlave.addPropertyChangeListener("UpdateButton", new PropertyChangeListener()
					{
						@Override
						public void propertyChange(PropertyChangeEvent evt)
						{
							final String prop = evt.getPropertyName();
							// State newState = (State) evt.getNewValue();
							System.out.println("Property changed Received: " + prop);
							if (prop.equals("UpdateButton"))
							{
								System.out.println("Update Received");
								updateSimulatorInfo();
							}
						} // end prop change
					});
				}
				simSlave.setVisible(true);

				Point points = frame.getLocation();
				simSlave.setLocation(points.x, points.y + contentPane.getHeight() + 45);
				simSlave.setNewMessages(messages);
				// }
				// else
				// lblStatus.setText("Please define messages first");
			}
		});
		mnSimulate.add(mntmSlaveDevice);

		JSeparator separator_3 = new JSeparator();
		mnSimulate.add(separator_3);

		chkExtraViewer = new JCheckBoxMenuItem("ExtraView");
		chkExtraViewer.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent arg0)
			{
				extraView();
			}
		});
		mnSimulate.add(chkExtraViewer);

		JMenu mnHelp = new JMenu("Help");
		menuBar.add(mnHelp);

		JMenuItem mntmAbout = new JMenuItem("About");
		mntmAbout.setIcon(new ImageIcon(UPMC_UI.class.getResource("/upmc/images/about1.png")));
		mntmAbout.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent arg0)
			{
				showAbout();
			}
		});
		mnHelp.add(mntmAbout);
		contentPane = new JPanel();
		contentPane.setBackground(UIManager.getColor("CheckBox.background"));
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);

		JPanel pnlTransmission = new JPanel();
		pnlTransmission.setBorder(new TitledBorder(UIManager.getBorder("InternalFrame.paletteBorder"), "Transmission", TitledBorder.LEADING, TitledBorder.TOP, null, null));

		JPanel pnlMessageDetail = new JPanel();
		pnlMessageDetail.setBorder(new TitledBorder(UIManager.getBorder("InternalFrame.paletteBorder"), "Message Detail", TitledBorder.LEADING, TitledBorder.TOP, null, null));

		JPanel pnlActions = new JPanel();
		pnlActions.setBorder(new TitledBorder(UIManager.getBorder("InternalFrame.paletteBorder"), "Actions", TitledBorder.LEADING, TitledBorder.TOP, null, null));

		JPanel pnlStatusGeneral = new JPanel();
		pnlStatusGeneral.setBorder(UIManager.getBorder("InternalFrame.paletteBorder"));

		JPanel pnlStatusPort = new JPanel();
		pnlStatusPort.setBorder(UIManager.getBorder("InternalFrame.paletteBorder"));
		pnlStatusPort.setLayout(null);

		lblPort = new JLabel("Port");
		lblPort.setBounds(1, 1, 333, 17);
		lblPort.setFont(new Font("Dialog", Font.BOLD, 9));
		pnlStatusPort.add(lblPort);
		GroupLayout gl_contentPane = new GroupLayout(contentPane);
		gl_contentPane.setHorizontalGroup(gl_contentPane.createParallelGroup(Alignment.TRAILING).addGroup(
				gl_contentPane
						.createSequentialGroup()
						.addContainerGap()
						.addComponent(pnlTransmission, GroupLayout.DEFAULT_SIZE, 391, Short.MAX_VALUE)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(
								gl_contentPane.createParallelGroup(Alignment.LEADING, false).addComponent(pnlMessageDetail, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 336, Short.MAX_VALUE)
										.addComponent(pnlActions, Alignment.TRAILING, 0, 0, Short.MAX_VALUE)
										.addComponent(pnlStatusPort, Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 336, GroupLayout.PREFERRED_SIZE)
										.addComponent(pnlStatusGeneral, GroupLayout.DEFAULT_SIZE, 336, Short.MAX_VALUE)).addContainerGap()));
		gl_contentPane.setVerticalGroup(gl_contentPane.createParallelGroup(Alignment.LEADING).addGroup(
				gl_contentPane
						.createSequentialGroup()
						.addGroup(
								gl_contentPane
										.createParallelGroup(Alignment.BASELINE)
										.addComponent(pnlTransmission, GroupLayout.DEFAULT_SIZE, 301, Short.MAX_VALUE)
										.addGroup(
												gl_contentPane.createSequentialGroup().addComponent(pnlMessageDetail, GroupLayout.PREFERRED_SIZE, 153, GroupLayout.PREFERRED_SIZE).addGap(10)
														.addComponent(pnlActions, GroupLayout.PREFERRED_SIZE, 73, GroupLayout.PREFERRED_SIZE).addGap(9)
														.addComponent(pnlStatusPort, GroupLayout.PREFERRED_SIZE, 19, GroupLayout.PREFERRED_SIZE).addGap(5)
														.addComponent(pnlStatusGeneral, GroupLayout.PREFERRED_SIZE, 19, GroupLayout.PREFERRED_SIZE))).addContainerGap()));
		pnlStatusGeneral.setLayout(null);

		lblStatus = new JLabel("Status");
		lblStatus.setBounds(1, 1, 333, 17);
		pnlStatusGeneral.add(lblStatus);
		lblStatus.setFont(new Font("Dialog", Font.BOLD, 9));

		// slider = new JSlider();
		slider = new JSlider(0, 96);
		slider.setValue(0);
		slider.addChangeListener(new ChangeListener()
		{
			public void stateChanged(ChangeEvent arg0)
			{
				repeatVal = 1010 - (slider.getValue() * 10);
				if (repeatVal > 1000)
					lblRepeatInfo.setText("Stop");
				else
					lblRepeatInfo.setText(Integer.toString(repeatVal) + "mS");
			}
		});
		slider.setBounds(12, 45, 246, 20);

		timer1 = new Timer(1000, new ActionListener()
		{
			public void actionPerformed(ActionEvent arg0)
			{
				if (repeatVal <= 1000)
				{
					timer1.setDelay(repeatVal);
					timer1.start();
				}
				else
				{
					timer1.stop();
					btnSendData.setText("Send");
				}

				sendIt();
			}
		});

		JPanel panel_4 = new JPanel();
		panel_4.setBounds(12, 18, 312, 20);
		panel_4.setLayout(new GridLayout(1, 4, 2, 5));

		btnSendData = new JButton("Send");
		btnSendData.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent arg0)
			{
				sendData();
			}
		});
		btnSendData.setEnabled(false);
		btnSendData.setFont(new Font("Dialog", Font.BOLD, 9));
		panel_4.add(btnSendData);

		btnOpenPort = new JButton("Open");
		btnOpenPort.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent arg0)
			{
				open_savePort();
			}
		});
		btnOpenPort.setFont(new Font("Dialog", Font.BOLD, 9));
		panel_4.add(btnOpenPort);

		btnLogData = new JButton("Log");
		btnLogData.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent arg0)
			{
				logFileHandle();
			}
		});
		btnLogData.setFont(new Font("Dialog", Font.BOLD, 9));
		panel_4.add(btnLogData);

		JButton btnDebugData = new JButton("Debug");
		btnDebugData.setEnabled(false);
		btnDebugData.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent arg0)
			{

			}
		});
		btnDebugData.setFont(new Font("Dialog", Font.BOLD, 9));
		panel_4.add(btnDebugData);
		pnlActions.setLayout(null);
		pnlActions.add(slider);
		pnlActions.add(panel_4);

		lblRepeatInfo = new JLabel("Stop");
		lblRepeatInfo.setFont(new Font("Dialog", Font.BOLD, 10));
		lblRepeatInfo.setBounds(263, 45, 51, 15);
		pnlActions.add(lblRepeatInfo);

		lblMessageName = new JLabel("Msg Name");
		lblMessageName.setFont(new Font("Dialog", Font.BOLD, 10));
		lblMessageName.setBounds(17, 17, 73, 15);

		lblHeader = new JLabel("Header");
		lblHeader.setFont(new Font("Dialog", Font.BOLD, 10));
		lblHeader.setBounds(205, 17, 46, 15);

		lblHCS = new JLabel("CS");
		lblHCS.setFont(new Font("Dialog", Font.BOLD, 10));
		lblHCS.setBounds(306, 17, 18, 15);

		txfNewMessage = new JTextField();
		txfNewMessage.setFont(new Font("Dialog", Font.PLAIN, 10));
		txfNewMessage.setBounds(12, 32, 188, 19);
		txfNewMessage.setColumns(10);

		txfHeader = new JTextField();
		txfHeader.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyReleased(KeyEvent e)
			{
				fixHeader();
			}
		});
		txfHeader.setFont(new Font("Dialog", Font.PLAIN, 10));
		txfHeader.setBounds(203, 32, 99, 19);
		txfHeader.setColumns(10);

		txfHCS = new JTextField();
		txfHCS.setFont(new Font("Dialog", Font.PLAIN, 10));
		txfHCS.setBounds(306, 32, 18, 19);
		txfHCS.setColumns(10);

		txfBody = new JTextField();
		txfBody.setFont(new Font("Dialog", Font.PLAIN, 10));
		txfBody.setBounds(12, 65, 290, 19);
		txfBody.setColumns(10);

		lblBody = new JLabel("Body");
		lblBody.setFont(new Font("Dialog", Font.BOLD, 10));
		lblBody.setBounds(17, 50, 34, 15);

		txfBCS = new JTextField();
		txfBCS.setFont(new Font("Dialog", Font.PLAIN, 10));
		txfBCS.setBounds(306, 65, 18, 19);
		txfBCS.setColumns(10);

		lblBCS = new JLabel("CS");
		lblBCS.setFont(new Font("Dialog", Font.BOLD, 10));
		lblBCS.setBounds(306, 50, 18, 15);

		comboBox = new JComboBox<String>();
		comboBox.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseReleased(MouseEvent e)
			{
				// lblStatus.setText(Integer.toString(comboBox.getItemCount()));
				if (comboBox.getItemCount() == 0) // Nothing loaded yet, go open
													// saved messages /*
					openFile();
			}
		});
		comboBox.addMouseWheelListener(new MouseWheelListener()
		{
			public void mouseWheelMoved(MouseWheelEvent arg0)
			{
				if (arg0.getWheelRotation() < 0)
				{
					if (comboBox.getSelectedIndex() > 0)
						comboBox.setSelectedIndex(comboBox.getSelectedIndex() - 1);
				}
				else
				{
					if (comboBox.getSelectedIndex() < comboBox.getItemCount() - 1)
						comboBox.setSelectedIndex(comboBox.getSelectedIndex() + 1);
				}
			}
		});
		// comboBox = new JComboBox();
		comboBox.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent arg0)
			{
				updateMsg();
			}
		});
		comboBox.setFont(new Font("Dialog", Font.BOLD, 10));
		comboBox.setBounds(12, 90, 312, 24);
		pnlMessageDetail.setLayout(null);
		pnlMessageDetail.add(lblMessageName);
		pnlMessageDetail.add(txfNewMessage);
		pnlMessageDetail.add(lblHeader);
		pnlMessageDetail.add(txfBCS);
		pnlMessageDetail.add(txfHeader);
		pnlMessageDetail.add(lblBody);
		pnlMessageDetail.add(txfBody);
		pnlMessageDetail.add(lblHCS);
		pnlMessageDetail.add(txfHCS);
		pnlMessageDetail.add(lblBCS);
		pnlMessageDetail.add(comboBox);

		JPanel panel_3 = new JPanel();
		panel_3.setBounds(12, 122, 312, 20);
		pnlMessageDetail.add(panel_3);
		panel_3.setLayout(new GridLayout(1, 4, 2, 5));

		JButton btnNew = new JButton("New");
		btnNew.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent arg0)
			{
				newMsg();
			}
		});
		panel_3.add(btnNew);
		btnNew.setFont(new Font("Dialog", Font.BOLD, 9));

		JButton btnCreate = new JButton("Create");
		btnCreate.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent arg0)
			{
				createMsg();
			}
		});
		panel_3.add(btnCreate);
		btnCreate.setFont(new Font("Dialog", Font.BOLD, 9));

		JButton btnEdit = new JButton("Update");
		btnEdit.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent arg0)
			{
				editMsg();
			}
		});
		panel_3.add(btnEdit);
		btnEdit.setFont(new Font("Dialog", Font.BOLD, 9));

		JButton btnDelete = new JButton("Del");
		btnDelete.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent arg0)
			{
				deleteMsg();
			}
		});
		panel_3.add(btnDelete);
		btnDelete.setFont(new Font("Dialog", Font.BOLD, 9));
		pnlTransmission.setLayout(new GridLayout(0, 1, 5, 5));

		JScrollPane scrollPane = new JScrollPane();
		pnlTransmission.add(scrollPane);

		popup = new JPopupMenu();
		menuItemClear = new JMenuItem("Clear Messages");

		menuItemClear.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (popupSource == PopupSource.TxArea)
				{
					lblStatus.setText("Clearing Tx messages\n");
					txaTransmit.setText("");
				}
				else if (popupSource == PopupSource.RxArea)
				{
					lblStatus.setText("Clearing Rx messages\n");
					txaReceive.setText("");
				}
			}
		});

		popup.add(menuItemClear);

		// Add listener to components that can bring up popup menus.
		MouseListener popupListener = new PopupListener();

		txaTransmit = new JTextArea();
		// txaTransmit.setForeground(SystemColor.activeCaption);
		txaTransmit.setForeground(Color.BLUE);
		// txaTransmit.setFont(new Font("Dialog", Font.BOLD, 11));
		txaTransmit.setFont(new Font("Tahoma", Font.BOLD, 11));
		txaTransmit.addMouseListener(popupListener);
		// txaTransmit.setText("Hello World!");
		scrollPane.setViewportView(txaTransmit);
		txaTransmit.setEditable(false);

		JScrollPane scrollPane_1 = new JScrollPane();
		pnlTransmission.add(scrollPane_1);

		txaReceive = new JTextArea();
		// txaReceive.setForeground(SystemColor.desktop);
		txaReceive.setForeground(Color.decode("0x206f34"));
		// txaReceive.setFont(new Font("Dialog", Font.BOLD, 11));
		txaReceive.setFont(new Font("Tahoma", Font.BOLD, 11));
		txaReceive.addMouseListener(popupListener);
		// txaReceive.setText("Hello World!");
		scrollPane_1.setViewportView(txaReceive);
		txaReceive.setEditable(false);
		contentPane.setLayout(gl_contentPane);

		extraViewerFrame = new ExtraViewer();
		commsManager = new CommsManager(txaTransmit, txaReceive, extraViewerFrame, lblStatus);
		// contentPane.setFocusTraversalPolicy(new FocusTraversalOnArray(new
		// Component[] { txfNewMessage, txfHeader, txfBody, btnCreate, btnEdit,
		// btnDelete, btnNew, btnOpenPort, btnSendData, btnLogData,
		// btnDebugData, slider, pnlStatusGeneral, lblStatus, pnlTransmission,
		// scrollPane, txaTransmit, scrollPane_1, txaReceive, pnlStatusPort,
		// lblPort, pnlActions, panel_4, lblRepeatInfo, pnlMessageDetail,
		// lblMessageName, lblHeader, txfBCS, lblBody, lblHCS, txfHCS, lblBCS,
		// comboBox, panel_3 }));

		contentPane.repaint();
	}

	/**
	 * Method to handle the openfile dialog
	 * 
	 * @param none
	 * 
	 * @return none
	 */
	private void ClearAll()
	{
		cleanup();

		txaTransmit.setText("");
		txaReceive.setText("");
		txfBCS.setText("");
		txfBody.setText("");
		txfHCS.setText("");
		txfHeader.setText("");
		txfNewMessage.setText("");
		messages.removeAll(messages);
		comboBox.removeAllItems();
		fileUpToDate = true;
	}

	/**
	 * Method to handle the openfile dialog
	 * 
	 * @param none
	 * 
	 * @return none
	 */
	private void fixHeader()
	{

		// if((key_counter % 2) == 0)
		// txfHeader.setText(txfHeader.getText() + " ");
	}

	/**
	 * Method to handle the openfile dialog
	 * 
	 * @param none
	 * 
	 * @return none
	 */
	private void openFile()
	{
		if (fileUpToDate == false)
		{
			int ret = JOptionPane.showConfirmDialog(this, "Save Messages?", "Save", JOptionPane.YES_NO_OPTION);
			if (ret == JOptionPane.YES_OPTION)
			{
				saveFile();
			}
		}

		if (commsManager.isConnected())
			commsManager.disconnect();

		ChooserFilter chooserFilter = new ChooserFilter();

		final JFileChooser fileChooser = new JFileChooser();
		chooserFilter.setFileType(ChooserFilter.FILE_UPM);
		fileChooser.setAcceptAllFileFilterUsed(false);
		fileChooser.addChoosableFileFilter(chooserFilter);

		fileChooser.setFileView(new ChooserFileView()); // Add custom icons for
														// file types.

		fileChooser.setAccessory(new ImagePreview(fileChooser)); // Add the
																	// preview
																	// pane.

		int returnVal = fileChooser.showOpenDialog(this); // In response to a
															// button click:

		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			int itemsToRemove = comboBox.getItemCount() - 1;

			File file = fileChooser.getSelectedFile();
			savedFile = file;

			updateFilename(file);

			messages.clear(); // Clear all the data in arraylist to null
			messages = xmlParser.ReadXml(file);

			if (messages.get(0).equals("Error"))
				return;

			if (portSettings == null)
			{
				portSettings = new PortSettings(this, rootPaneCheckingEnabled);
			}

			ArrayList<String> xmlportSettings = xmlParser.getPortSettings();
			portSettings.setPort(xmlportSettings.get(0));
			portSettings.setBaud(Integer.parseInt(xmlportSettings.get(1)));
			portSettings.setDataBits(Integer.parseInt(xmlportSettings.get(2)));
			portSettings.setStopBits(Integer.parseInt(xmlportSettings.get(3)));
			portSettings.setParity(Integer.parseInt(xmlportSettings.get(4)));
			portSettings.setFlowControl(Integer.parseInt(xmlportSettings.get(5)));

			if (xmlportSettings.get(6).equals("Hex"))
				portSettings.setProtocolType(ProtocolType.HEX);
			else if (xmlportSettings.get(6).equals("Text"))
				portSettings.setProtocolType(ProtocolType.TEXT);
			else if (xmlportSettings.get(6).equals("NSP"))
				portSettings.setProtocolType(ProtocolType.NSP);
			else if (xmlportSettings.get(6).equals("NSP2"))
				portSettings.setProtocolType(ProtocolType.NSP2);
			else if (xmlportSettings.get(6).equals("Modbus"))
				portSettings.setProtocolType(ProtocolType.MODBUS);

			portSettings.setAppendNewLine(Boolean.parseBoolean(xmlportSettings.get(7)));
			portSettings.setAppendNewLineTimeout(Integer.parseInt(xmlportSettings.get(8)));
			portSettings.setInterfaceType(Integer.parseInt(xmlportSettings.get(9)));
			portSettings.setTcpMode(Integer.parseInt(xmlportSettings.get(10)));
			portSettings.setTcpAddress(xmlportSettings.get(11));
			portSettings.setTcpPort(Integer.parseInt(xmlportSettings.get(12)));

			getPortSettings();

			for (int loop = 0; loop < messages.size(); loop++)
			{
				String temp = messages.get(loop);
				String[] temp2 = temp.split(":");
				comboBox.addItem(temp2[0]);
			}
			for (; itemsToRemove >= 0; itemsToRemove--)
			{
				comboBox.removeItemAt(itemsToRemove);
			}
		}
		else
		{
			lblStatus.setText("Open command cancelled by user.");
		}
	}

	private void updateFilename(File file)
	{
		String filename_full = file.toString();
		if (System.getProperty("os.name").toLowerCase().contains("linux"))
		{
			String filename_only = filename_full.substring(filename_full.lastIndexOf('/') + 1, filename_full.length());
			frame.setTitle("Universal Port Message Compiler - " + filename_only);
		}
		else if (System.getProperty("os.name").toLowerCase().contains("win"))
		{
			String filename_only = filename_full.substring(filename_full.lastIndexOf('\\') + 1, filename_full.length());
			frame.setTitle("Universal Port Message Compiler - " + filename_only);
		}
		else
		{
			frame.setTitle("Universal Port Message Compiler - " + filename_full);
		}
	}

	/**
	 * Method to determine if the messages should be saved or not.
	 * 
	 * @param none
	 * 
	 * @return none
	 */
	private void saveFile()
	{
		if (savedFile == null)
		{
			saveAsFile();
		}
		else
		{
			saveXml();
		}

		// if ((savedFile == null) && (fileUpToDate == false)) {
		// saveAsFile();
		// } else if ((savedFile != null) && (fileUpToDate == false)) {
		// saveXml();
		// }
	}

	/**
	 * Method to determine if the messages should be saved or not.
	 * 
	 * @param none
	 * 
	 * @return none
	 */
	private void saveAsFile()
	{
		final JFileChooser fileChooser = new JFileChooser();
		ChooserFilter chooserFilter = new ChooserFilter();

		if (portSettings == null)
		{
			openPortSettings();
		}

		chooserFilter.setFileType(ChooserFilter.FILE_UPM);
		fileChooser.setAcceptAllFileFilterUsed(false);
		fileChooser.addChoosableFileFilter(chooserFilter);

		fileChooser.setFileView(new ChooserFileView()); // Add custom icons for
														// file types.
		fileChooser.setAccessory(new ImagePreview(fileChooser)); // Add the
																	// preview
																	// pane.

		int returnVal = fileChooser.showSaveDialog(this);

		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			File file = fileChooser.getSelectedFile();
			savedFile = file;
			updateFilename(file);
			saveXml();
		}
	}

	/**
	 * Method to call the writeXml method in the XmlParser class.
	 * 
	 * @param none
	 * 
	 * @return none
	 */
	private void saveXml()
	{
		ArrayList<String> portSettingsXML = new ArrayList<String>();

		getPortSettings();

		if (portSettings.getPort().isEmpty())
			portSettingsXML.add("/dev/ttyS0");
		else
			portSettingsXML.add(portSettings.getPort());

		portSettingsXML.add(Integer.toString(portSettings.getBaud()));
		portSettingsXML.add(Integer.toString(portSettings.getDataBits()));
		portSettingsXML.add(Integer.toString(portSettings.getStopBits()));
		portSettingsXML.add(Integer.toString(portSettings.getParity()));
		portSettingsXML.add(Integer.toString(portSettings.getFlowControl()));

		if (msgData.protocolType == CommsManager.ProtocolType.HEX)
			portSettingsXML.add("Hex");
		else if (msgData.protocolType == CommsManager.ProtocolType.TEXT)
			portSettingsXML.add("Text");
		else if (msgData.protocolType == CommsManager.ProtocolType.NSP)
			portSettingsXML.add("NSP");
		else if (msgData.protocolType == CommsManager.ProtocolType.NSP2)
			portSettingsXML.add("NSP2");
		else if (msgData.protocolType == CommsManager.ProtocolType.MODBUS)
			portSettingsXML.add("Modbus");
		else
			portSettingsXML.add("None");

		portSettingsXML.add(Boolean.toString(portSettings.getAppendNewLine()));
		portSettingsXML.add(Integer.toString(portSettings.getAppendNewLineTimeout()));

		portSettingsXML.add(Integer.toString(portSettings.getInterfaceType()));
		portSettingsXML.add(Integer.toString(portSettings.getTcpMode()));
		portSettingsXML.add(portSettings.getTcpAddress());
		portSettingsXML.add(Integer.toString(portSettings.getTcpPort()));

		xmlParser.CreateXml(savedFile, portSettingsXML, messages);
		fileUpToDate = true;
	}

	/**
	 * Method to handle the openPortSetting dialog
	 */
	private void openPortSettings()
	{
		boolean wasOpen = false;

		if (commsManager.isConnected())
		{
			wasOpen = true;
			commsManager.disconnect();
		}
		if (portSettings == null)
		{
			portSettings = new PortSettings(this, rootPaneCheckingEnabled);
			portSettings.setLocationRelativeTo(this);
		}
		portSettings.setVisible(true);

		getPortSettings();
		if (wasOpen)
		{
			connect();
			// if (msgData.interfaceType == CommsManager.INTERFACE_SERIAL)
			// commsManager.connect(portSettings.getPort(),
			// portSettings.getBaud(), portSettings.getParity(),
			// portSettings.getDataBits(),
			// portSettings.getStopBits(), portSettings.getFlowControl(),
			// msgData);
			// else if (msgData.interfaceType == CommsManager.INTERFACE_TCP)
			// commsManager.connect(portSettings.getTcpAddress(),
			// portSettings.getTcpPort(), msgData);
		}
	}

	/**
	 * Method to get the port setting from portsettings dialog
	 */
	private void getPortSettings()
	{
		msgData.protocolType = portSettings.getProtocolType();
		if ((msgData.protocolType == CommsManager.ProtocolType.HEX) || (msgData.protocolType == CommsManager.ProtocolType.TEXT))
		{
			txfHeader.setVisible(false);
			txfHCS.setVisible(false);
			txfBCS.setVisible(false);
			lblHeader.setVisible(false);
			lblHCS.setVisible(false);
			lblBCS.setVisible(false);
		}
		else if (msgData.protocolType == CommsManager.ProtocolType.NSP)
		{
			txfHeader.setVisible(true);
			txfHCS.setVisible(true);
			txfBCS.setVisible(true);
			lblHeader.setVisible(true);
			lblHCS.setVisible(true);
			lblBCS.setVisible(true);

			txfNewMessage.setBounds(12, 32, 188, 19);
			txfHeader.setBounds(203, 32, 99, 19);
			txfHCS.setBounds(306, 32, 18, 19);
			txfBody.setBounds(12, 65, 290, 19);
			txfBCS.setBounds(306, 65, 18, 19);
			lblHeader.setBounds(205, 17, 46, 15);
		}
		else if (msgData.protocolType == CommsManager.ProtocolType.NSP2)
		{
			txfHeader.setVisible(true);
			txfHCS.setVisible(true);
			txfBCS.setVisible(true);
			lblHeader.setVisible(true);
			lblHCS.setVisible(true);
			lblBCS.setVisible(true);

			txfNewMessage.setBounds(12, 32, 116, 19);
			txfHeader.setBounds(131, 32, 154, 19);
			txfHCS.setBounds(289, 32, 35, 19);
			txfBody.setBounds(12, 65, 273, 19);
			txfBCS.setBounds(289, 65, 35, 19);
			lblHeader.setBounds(131, 17, 46, 15);
		}

		// interfaceType = portSettings.getInterfaceType();
		msgData.interfaceType = portSettings.getInterfaceType();
		if (msgData.interfaceType == CommsManager.INTERFACE_TCP)
			msgData.tcpMode = portSettings.getTcpMode();

		if (msgData.interfaceType == CommsManager.INTERFACE_SERIAL)
		{
			msgData.appendNewline = portSettings.getAppendNewLine();
			msgData.appendNewLineTimeout = portSettings.getAppendNewLineTimeout();
			lblPort.setText("Port " + portSettings.getPort() + " " + portSettings.getBaud() + " " + portSettings.getDataBits() + "-"
					+ (portSettings.getParity() == 0 ? "N" : portSettings.getParity() == 1 ? "O" : "E") + "-" + portSettings.getStopBits()
					+ (portSettings.getFlowControl() == 0 ? "-N" : portSettings.getFlowControl() == 1 ? "-HW" : "-SW") + " Protocol: " + msgData.protocolType
					+ ((msgData.protocolType == CommsManager.ProtocolType.NSP) || (msgData.protocolType == CommsManager.ProtocolType.NSP2) ? "" : "-" + msgData.appendNewLineTimeout));
		}
		else if ((msgData.interfaceType == CommsManager.INTERFACE_TCP) && (msgData.tcpMode == PortSettings.TCP_CLIENT))
		{
			msgData.appendNewline = portSettings.getAppendNewLine();
			msgData.appendNewLineTimeout = portSettings.getAppendNewLineTimeout();
			lblPort.setText("TCP Client - IP: " + portSettings.getTcpAddress() + " Port: " + portSettings.getTcpPort() + " Protocol: " + msgData.protocolType
					+ ((msgData.protocolType == CommsManager.ProtocolType.NSP) || (msgData.protocolType == CommsManager.ProtocolType.NSP2) ? "" : "-" + msgData.appendNewLineTimeout));
		}
		else if ((msgData.interfaceType == CommsManager.INTERFACE_TCP) && (msgData.tcpMode == PortSettings.TCP_SERVER))
		{
			msgData.appendNewline = portSettings.getAppendNewLine();
			msgData.appendNewLineTimeout = portSettings.getAppendNewLineTimeout();
			lblPort.setText("TCP Server - Listening Port: " + portSettings.getTcpPort() + " Protocol: " + msgData.protocolType
					+ ((msgData.protocolType == CommsManager.ProtocolType.NSP) || (msgData.protocolType == CommsManager.ProtocolType.NSP2) ? "" : "-" + msgData.appendNewLineTimeout));
		}
	}

	private void showAbout()
	{
		JOptionPane.showMessageDialog(this, "A little app to compile different protocols for serial and tcp ports\n" + "Version " + getApplicationVersion() + "\n\n"

		+ "Changelog:\n" + "Version 1.2.0:\n- Replace RXTX with jSerialComm\n- Add standalone packaging and CI\n" + "Version 1.1.8:\n- Fix buffer length for NSP2\n" + "Version 1.1.7:\n- Add ExtraView\n" +"Version 1.1.6:\n" + "- Made that the TX and Rx Boxes only keep 500 lines of data\n" + "Version 1.1.5:\n" + "- Bug fix on nsp1 & nsp2 multiple packet stream error\n"
		+ "Version 1.1.4:\n" + "- Bug fix on nsp2 receive timeout\n" + "Version 1.1.3:\n" + "- Made simulation table variable\n" + "- Added support for old SPMC nsp type files\n"
		+ "Version 1.1.2:\n" + "- Fixed incorrect TCP server save / load settings\n" + "- Fixed close and open issue when clicking on port settings, when port was open\n"
		+ "- Added Flowcontrol options\n" + "- Bug Fixes\n" + "Version 1.1.1:\n" + "- Fixed issue when usb cable disconnected, causing app to crash with open button\n"
		+ "- Added filename to titlebar\n" + "- Bug Fixes\n" + "Version 1.0.8:\n" + "- Started to implement slave simulator (still buggy)\n"
		+ "- Added load and save option for simulator setups\n" + "Version 1.0.7:\n" + "- Implemented NSP2\n" + "- Removed Modbus protocol\n" + "Version 1.0.6:\n"
		+ "- Fixed log file extension (*.log)\n" + "- Add to save file the the interface and tcp info\n" + "- Implememted the 'File->New' feature\n"
		+ "- Changed the xml file extension to *.upm (was *.nsp)\n" + "- Minor bug fixes\n" + "Version 1.0.5:\n" + "- Implemented TCP Server and TCP Slave\n"
		+ "- Add to save file the append newline and newline timeout (Not compattible with nsp file anymore)\n" + "- Bug fixes\n" + "Version 1.0.4:\n" + "- Fixed slider at stop possition\n"
		+ "- Fixed 'open port' when no port is available\n" + "- Fixed minor bugs, issues & icons\n" + "- Added Text mode\n" + "- Changed colours and font size in the Tx and Rx text areas\n",
		"UPMC - About", 3);
	}

	private static String getApplicationVersion()
	{
		String version = UPMC_UI.class.getPackage().getImplementationVersion();
		return version == null ? "development" : version;
	}

	private void newMsg()
	{
		txfNewMessage.setText(null);
		txfHeader.setText(null);
		txfHCS.setText(null);
		txfBody.setText(null);
		txfBCS.setText(null);
		txfNewMessage.requestFocus();
	}

	private void createMsg()
	{
		/* CREATE Button clicked */

		if (portSettings == null)
			openPortSettings();

		switch (msgData.protocolType)
		{
			case NSP:
				if (txfNewMessage.getText().isEmpty())
				{
					lblStatus.setText("Enter message name!");
				}
				else
				{
					lblStatus.setText("Creating Message " + msgCounter);

					msgData.MsgName = txfNewMessage.getText().trim();
					msgData.Header = txfHeader.getText().trim();
					msgData.Body = txfBody.getText().trim();

					String ret = commsManager.CheckNSPMessage(msgData);

					if (ret == "error")
						return;

					txfHeader.setText(msgData.Header);
					txfHCS.setText(msgData.HeaderCS);
					txfBody.setText(msgData.Body);
					txfBCS.setText(msgData.BodyCS);

					messages.add(ret);
					comboBox.addItem(txfNewMessage.getText());
					txfNewMessage.requestFocus();
				}
				break;

			case NSP2:
				if (txfNewMessage.getText().isEmpty())
				{
					lblStatus.setText("Enter message name!");
				}
				else
				{
					lblStatus.setText("Creating Message " + msgCounter);

					msgData.MsgName = txfNewMessage.getText().trim();
					msgData.Header = txfHeader.getText().trim();
					msgData.Body = txfBody.getText().trim();

					String ret = commsManager.CheckNSPMessage(msgData);

					if (ret == "error")
						return;

					txfHeader.setText(msgData.Header);
					txfHCS.setText(msgData.HeaderCS);
					txfBody.setText(msgData.Body);
					txfBCS.setText(msgData.BodyCS);

					messages.add(ret);
					comboBox.addItem(txfNewMessage.getText());
					txfNewMessage.requestFocus();
				}
				break;

			case HEX:
			case TEXT:
				if (txfNewMessage.getText().isEmpty())
				{
					lblStatus.setText("Enter message name!");
				}
				else
				{
					lblStatus.setText("Creating Message " + msgCounter);

					msgData.MsgName = txfNewMessage.getText().trim();
					msgData.Body = txfBody.getText().trim();

					String ret = commsManager.CheckNSPMessage(msgData);

					if (ret == "error")
						return;

					txfBody.setText(msgData.Body);

					messages.add(ret);
					comboBox.addItem(txfNewMessage.getText());
					txfNewMessage.requestFocus();
				}
				break;
			case MODBUS:
			default:
				lblStatus.setText("Protocol Not supported yet, watch this space!");
				break;
		}

		updateSimulatorInfo();

		fileUpToDate = false; // Indicate that the file needs saving
	}

	private void updateSimulatorInfo()
	{
		if (simSlave != null)
		{
			simSlave.setNewMessages(messages);
			if (simSlave.getSimSlaveEnable())
				commsManager.setDataSet(simSlave.getDataSet(), true);
			else
				commsManager.setDataSet(simSlave.getDataSet(), false);
		}
	}

	private void extraView()
	{
		if (chkExtraViewer.isSelected())
		{
			extraViewerFrame.setVisible(true);
			msgData.extraViewerEnabled = true;
			commsManager.setExtraViewEnabled(true);
		}
		else
		{
			extraViewerFrame.setVisible(false);
			msgData.extraViewerEnabled = false;
			commsManager.setExtraViewEnabled(false);
		}
	}

	private void editMsg()
	{
		/* EDIT Button clicked */
		if (portSettings == null)
			openPortSettings();

		switch (msgData.protocolType)
		{
			case NSP:
			case NSP2:
				if (txfNewMessage.getText().isEmpty())
				{
					lblStatus.setText("Enter message name!");
				}
				else
				{
					lblStatus.setText("Editing Message " + msgCounter);

					msgData.MsgName = txfNewMessage.getText().trim();
					msgData.Header = txfHeader.getText().trim();
					msgData.Body = txfBody.getText().trim();

					String ret = commsManager.CheckNSPMessage(msgData);

					txfHeader.setText(msgData.Header);
					txfHCS.setText(msgData.HeaderCS);
					txfBody.setText(msgData.Body);
					txfBCS.setText(msgData.BodyCS);

					messages.set(comboBox.getSelectedIndex(), ret);
					comboBox.insertItemAt(txfNewMessage.getText(), comboBox.getSelectedIndex());
					comboBox.removeItemAt(comboBox.getSelectedIndex());
					txfNewMessage.requestFocus();
				}
				break;

			case HEX:
			case TEXT:
				if (txfNewMessage.getText().isEmpty())
				{
					lblStatus.setText("Enter message name!");
				}
				else
				{
					lblStatus.setText("Creating Message " + msgCounter);

					msgData.MsgName = txfNewMessage.getText().trim();
					msgData.Body = txfBody.getText().trim();

					String ret = commsManager.CheckNSPMessage(msgData);

					if (ret == "error")
						return;

					txfBody.setText(msgData.Body);

					messages.set(comboBox.getSelectedIndex(), ret);
					comboBox.insertItemAt(txfNewMessage.getText(), comboBox.getSelectedIndex());
					comboBox.removeItemAt(comboBox.getSelectedIndex());
					txfNewMessage.requestFocus();
				}
				break;
			case MODBUS:
			default:
				lblStatus.setText("Protocol Not supported yet, watch this space!");
				break;
		}
		updateSimulatorInfo();
		fileUpToDate = false; // Indicate that the file needs saving
	}

	private void deleteMsg()
	{
		// DELETE Button Clicked
		int ret = JOptionPane.showConfirmDialog(this, "Delete Message?", "Delete", JOptionPane.YES_NO_OPTION);
		if (ret == JOptionPane.YES_OPTION)
		{
			messages.remove(comboBox.getSelectedIndex());
			comboBox.removeItemAt(comboBox.getSelectedIndex());
			txfNewMessage.requestFocus();
		}
		fileUpToDate = false; // Indicate that the file needs saving
	}

	private void updateMsg()
	{
		if (comboBox.getItemCount() > 0)
		{
			String[] tmp = messages.get(comboBox.getSelectedIndex()).split(":");

			if (msgData.protocolType == CommsManager.ProtocolType.NSP)
			{
				txfNewMessage.setText(tmp[0].trim());
				txfHeader.setText(tmp[1].trim().substring(0, 18));
				txfHCS.setText(tmp[1].trim().substring(18, 20));

				int bdyLen = (tmp[1].length() - 4 - 21);
				if (bdyLen > 0)
				{
					txfBody.setText(tmp[1].substring(22, 22 + bdyLen).toUpperCase().trim());
					txfBCS.setText(tmp[1].substring(22 + bdyLen, 22 + bdyLen + 3).toUpperCase().trim());
					lblStatus.setText("Item (" + comboBox.getSelectedIndex() + ") :" + txfHeader.getText().trim() + " " + txfHCS.getText().trim() + " " + txfBody.getText().trim() + " "
							+ txfBCS.getText().trim());
				}
				else
				{
					lblStatus.setText("Item (" + comboBox.getSelectedIndex() + ") :" + txfHeader.getText() + " " + txfHCS.getText());
					txfBody.setText(null);
					txfBCS.setText(null);
				}
			}
			if (msgData.protocolType == CommsManager.ProtocolType.NSP2)
			{
				txfNewMessage.setText(tmp[0].trim());
				txfHeader.setText(tmp[1].trim().substring(0, 30));
				txfHCS.setText(tmp[1].trim().substring(30, 35));

				// int bdyLen = (tmp[1].length() - 4 - 36);
				int bdyLen = (tmp[1].length() - 7 - 36);
				if (bdyLen > 0)
				{
					txfBody.setText(tmp[1].substring(37, 37 + bdyLen).toUpperCase().trim());
					// txfBCS.setText(tmp[1].substring(37 + bdyLen, 37 + bdyLen
					// + 3).toUpperCase().trim());
					txfBCS.setText(tmp[1].substring(37 + bdyLen, 37 + bdyLen + 6).toUpperCase().trim());
					lblStatus.setText("Item (" + comboBox.getSelectedIndex() + ") :" + txfHeader.getText().trim() + " " + txfHCS.getText().trim() + " " + txfBody.getText().trim() + " "
							+ txfBCS.getText().trim());
				}
				else
				{
					lblStatus.setText("Item (" + comboBox.getSelectedIndex() + ") :" + txfHeader.getText() + " " + txfHCS.getText());
					txfBody.setText(null);
					txfBCS.setText(null);
				}
			}
			else if (msgData.protocolType == CommsManager.ProtocolType.HEX)
			{
				txfNewMessage.setText(tmp[0].trim());

				txfBody.setText(tmp[1].toUpperCase().trim());

				lblStatus.setText("Item (" + comboBox.getSelectedIndex() + ") :" + txfBody.getText().trim());
			}
			else if (msgData.protocolType == CommsManager.ProtocolType.TEXT)
			{
				txfNewMessage.setText(tmp[0].trim());

				txfBody.setText(tmp[1].trim());

				lblStatus.setText("Item (" + comboBox.getSelectedIndex() + ") :" + txfBody.getText().trim());
			}
		}
	}

	/**
	 * Method for sending data to the commsManager class. The data can either be
	 * hex or text
	 * 
	 * @param none
	 * 
	 * @return none
	 */
	private void sendData()
	{
		if ((repeatVal <= 1000) && (btnSendData.getText() == "Send"))
		{
			btnSendData.setText("Stop");
			timer1.setDelay(repeatVal);
			timer1.start();
		}
		else if (btnSendData.getText() == "Stop")
		{
			timer1.stop();
			btnSendData.setText("Send");
		}

		sendIt();
	}

	private void sendIt()
	{
		if ((msgData.protocolType == CommsManager.ProtocolType.NSP) || (msgData.protocolType == CommsManager.ProtocolType.NSP2))
		{
			if ((txfHeader.getText().isEmpty()) || (txfHCS.getText().isEmpty()))
			{
				lblStatus.setText("Enter message detail or select from combobox!");
				return;
			}
			msgData.MsgName = txfNewMessage.getText();
			msgData.Header = txfHeader.getText();
			msgData.HeaderCS = txfHCS.getText();
			msgData.Body = txfBody.getText();
			msgData.BodyCS = txfBCS.getText();
		}
		else if ((msgData.protocolType == CommsManager.ProtocolType.HEX) || msgData.protocolType == CommsManager.ProtocolType.TEXT)
		{
			if (txfBody.getText().isEmpty())
			{
				lblStatus.setText("Enter message detail or select from combobox!");
				return;
			}
			msgData.MsgName = txfNewMessage.getText();
			msgData.Body = txfBody.getText();
		}
		else
		{
			lblStatus.setText("Protocol not supported yet. Please watc this space!");
		}

		if (commsManager.isConnected())
		{
			commsManager.writeData(msgData);
		}
	}

	/**
	 * Simple method for opening and closing of the selected serial port
	 * 
	 * @param none
	 * 
	 * @return none
	 */
	private void open_savePort()
	{
		if (portSettings == null)
			openPortSettings();

		updateSimulatorInfo();

		if (commsManager.isConnected())
		{
			timer1.stop();
			commsManager.disconnect();
			// btnOpenPort.setText("Open");
			// btnSendData.setEnabled(false);
			// btnSendData.setText("Send");
		}
		else
		{
			connect();
		}
	}

	/**
     * 
     */
	private void connect()
	{
		try
		{
			commsManager.addPropertyChangeListener("CommsManager", new PropertyChangeListener()
			{
				// @Override
				public void propertyChange(PropertyChangeEvent evt)
				{
					final String prop = evt.getPropertyName();
					final Object oldVal = evt.getOldValue();
					final Object newVal = evt.getNewValue();

					if (newVal == CommsManager.CONNECTION_STARTED)
					{
						btnOpenPort.setText("Close");
						btnSendData.setEnabled(true);
					}

					if (newVal == CommsManager.CONNECTION_STOPPED)
					{
						try
						{
							Thread.sleep(100);
						}
						catch (Exception e)
						{
							System.out.print("Sleep Issue @ Connection Close");
						}
						btnOpenPort.setText("Open");
						btnSendData.setEnabled(false);
						btnSendData.setText("Send");
						commsManager.removePropertyChangeListener("CommsManager", this);
					}

					if (newVal == CommsManager.CONNECTION_INTERRUPT)
					{
						btnOpenPort.setText("Open");
						btnSendData.setEnabled(false);
						commsManager.disconnect();
					}
				}
			});

			if (msgData.interfaceType == CommsManager.INTERFACE_SERIAL)
			{
				commsManager.connect(portSettings.getPort(), portSettings.getBaud(), portSettings.getParity(), portSettings.getDataBits(), portSettings.getStopBits(), portSettings.getFlowControl(),
						msgData);
			}
			else if (msgData.interfaceType == CommsManager.INTERFACE_TCP)
			{

				commsManager.connect(portSettings.getTcpAddress(), portSettings.getTcpPort(), msgData);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Simple method for opening and closing of the selected serial port
	 * 
	 * @param none
	 * 
	 * @return none
	 */
	private void logFileHandle()
	{
		try
		{
			if (msgData.logToFile)
			{
				btnLogData.setText("Log");
				msgData.logToFile = false;
				msgData.logfile.close();
				commsManager.setLogToFile(false);
			}
			else
			{
				ChooserFilter chooserFilter = new ChooserFilter();

				JFileChooser fileChooser = new JFileChooser();
				chooserFilter.setFileType(ChooserFilter.FILE_LOG);
				fileChooser.setAcceptAllFileFilterUsed(false);
				fileChooser.addChoosableFileFilter(chooserFilter);

				while (true)
				{
					int choice = fileChooser.showSaveDialog(this);
					if (choice == JFileChooser.APPROVE_OPTION)
					{
						File chosen = fileChooser.getSelectedFile();
						if (!chosen.exists())
						{
							msgData.saveFileLog = chosen;
							btnLogData.setText("Stop");
							msgData.logToFile = true;

							if ((chosen.getName().endsWith(".log")) || (chosen.getName().endsWith(".txt")))
							{
								msgData.logfile = new FileWriter(chosen.getAbsoluteFile());
							}
							else
							{
								msgData.logfile = new FileWriter(chosen.getAbsoluteFile() + ".log");
							}

							commsManager.setLogFile(msgData.logfile);
							commsManager.setLogToFile(true);
							break;
						}
						else
						{
							int confirm = JOptionPane.showConfirmDialog(this, "Overwrite file? " + chosen.getName());
							if (confirm == JOptionPane.OK_OPTION)
							{
								msgData.saveFileLog = chosen;
								btnLogData.setText("Stop");
								msgData.logToFile = true;
								msgData.logfile = new FileWriter(msgData.saveFileLog);
								commsManager.setLogFile(msgData.logfile);
								commsManager.setLogToFile(true);
								break;
							}
							else if (confirm == JOptionPane.NO_OPTION)
							{
								continue;
							}
							break;
						}
					}
					else
					{
						break;
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();

		}
	}
}
