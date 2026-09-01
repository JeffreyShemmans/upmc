/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package upmc;

import com.fazecast.jSerialComm.SerialPort;
import java.net.*;
//import java.io.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.JLabel;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;

import java.text.SimpleDateFormat;
//import java.lang.Number;
import java.util.logging.*;
//import net.sourceforge.juint.*;
//import java.io.UnsupportedEncodingException;
import upmc.UPMC_UI.MessageData;

/**
 * Used to simplify communication over a serial port. Using jSerialComm, one
 * connection per instance of this class can be handled.
 * In addition to handling a connection, information about the available Serial
 * ports can be received using this class.
 * 
 * A separate {@link Thread} is started to handle messages that are being
 * received over the Serial interface.
 * 
 * This class also makes packages out of a stream of bytes received, using a
 * {@link #divider}, and sending these packages as an array of <b>int</b>s (each
 * between 0 and 255) to a function implemented by a class implementing the
 * {@link net.Network_iface}-interface.
 * 
 * @author Raphael Blatter (raphael@blatter.sg)
 * @author heavily using code examples from the former RXTX implementation
 */
public class CommsManager
{
	private static final Logger LOGGER = Logger.getLogger(CommsManager.class.getName());

	// public enum InterfaceType
	// {
	// Serial, TCP
	// };

	public final static int INTERFACE_NONE = 0;
	public final static int INTERFACE_SERIAL = 1;
	public final static int INTERFACE_TCP = 2;

	public final static String CONNECTION_STOPPED = "STOPPED";
	public final static String CONNECTION_STARTING = "STARTING";
	public final static String CONNECTION_STARTED = "STARTED";
	public final static String CONNECTION_STOPPING = "STOPPING";
	public final static String CONNECTION_INTERRUPT = "INTERRUPT";

	private final static int NSP1_HEADER_LEN = 7;
	private final static int NSP1_HEADER_AND_BODY_CS_LEN = 8;
	private final static int NSP2_HEADER_LEN = 12;
	private final static int NSP2_HEADER_AND_BODY_CS_LEN = 14;

	public enum ProtocolType
	{
		HEX, TEXT, NSP, NSP2, MODBUS, NONE
	}

	// CCITT CRC-16 info
	private final static int CRC_POLYNOM = 0x8408;
	private final static int CRC_PRESET = 0xFFFF;

	// The Thread used to receive the data from the Serial interface.
	private SerialPort serialPort = null;
	private Socket tcpClientSocket = null;
	private final List<TcpClientSession> tcpServerClients = new CopyOnWriteArrayList<TcpClientSession>();
	private int tcpMode = 0;

	private volatile boolean connectState = false;
	private String connection = CONNECTION_STOPPED;

	// Tcp variables
	private InputStream inStream = null;
	private OutputStream outStream = null;
	private Thread inStreamThread = null;
	private volatile boolean threadEnd = false;

	private TcpServer tcpServer = null;

	private TcpServer.State conn = TcpServer.State.STOPPED;
	private PropertyChangeSupport propSupport = new PropertyChangeSupport(this); // Properties

	private int interfaceType = INTERFACE_NONE;
	private ProtocolType protocolType = null;
	private JTextArea rxBox = null;
	private JTextArea txBox = null;
	private JTextArea extraBox = null;
	private JLabel statusLabel = null;
	private boolean appendNewLine = true;
	private int appendNewLineTimeout = 50;
	private JTable table = null;
	private String[] simulateReceiveMsg;
	private String[] simulateTransmitMsg;
	private int noOfSimulateReceiveMsg = 0;
	
	ExtraViewer extraViewerFrame;

	/**
	 * Link to the instance of the class implementing {@link net.Network_iface}.
	 */
	// private Network_iface contact;
	/**
	 * A small <b>int</b> representing the number to be used to distinguish
	 * between two consecutive packages. It can only take a value between 0 and
	 * 255. Note that data is only sent to
	 * {@link net.Network_iface#parseInput(int, int, int[])} once the following
	 * 'divider' could be identified.
	 * 
	 * As a default, <b>255</b> is used as a divider (unless specified otherwise
	 * in the constructor).
	 * 
	 * @see net.Network#Network(int, Network_iface, int)
	 */
	private int divider;
	int numTempBytes = 0, numTotBytes = 0;

	private FileWriter logfile;
	private boolean logTofile = false;
	private boolean extraView = false;

	private DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

	private final class TcpClientSession
	{
		private final Socket socket;
		private final InputStream input;
		private final OutputStream output;
		private volatile boolean open = true;
		private Thread readerThread;

		TcpClientSession(Socket socket) throws IOException
		{
			this.socket = socket;
			this.input = socket.getInputStream();
			this.output = socket.getOutputStream();
		}

		void start()
		{
			readerThread = new Thread(new InputStreamReader(input, this, true),
					"UPMC TCP client " + socket.getRemoteSocketAddress());
			readerThread.start();
		}

		synchronized void write(byte[] data) throws IOException
		{
			if (!open)
				throw new IOException("TCP client is closed");
			output.write(data);
			output.flush();
		}

		void close()
		{
			open = false;
			try
			{
				socket.close();
			}
			catch (IOException ignored)
			{
			}
		}
	}

	// Date date = new Date();

	// ActionAdapter buttonListener = null;

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
		propSupport.addPropertyChangeListener(property, listener);
	}

	/**
	 * Add a property listener.
	 * 
	 * @param listener
	 *            the property change listener
	 */
	public synchronized void addPropertyChangeListener(PropertyChangeListener listener)
	{
		propSupport.addPropertyChangeListener(listener);
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
		propSupport.removePropertyChangeListener(property, listener);
	}

	public int getInterfaceType()
	{
		return interfaceType;
	}

	public void setInterfaceType(int value)
	{
		interfaceType = value;
	}

	public ProtocolType getProtocolType()
	{
		return protocolType;
	}

	public void setProtocolType(ProtocolType value)
	{
		protocolType = value;
	}

	public FileWriter getLogFile()
	{
		return logfile;
	}

	public void setLogFile(FileWriter value)
	{
		logfile = value;
	}

	public boolean getLogToFile()
	{
		return logTofile;
	}

	public void setLogToFile(Boolean value)
	{
		logTofile = value;
	}
	
	public boolean getExtraViewEnabled()
	{
		return extraView;
	}

	public void setExtraViewEnabled(Boolean value)
	{
		extraView = value;
	}

	public JTable getDataSet()
	{
		return table;
	}

	public void setDataSet(JTable value, boolean enabled)
	{
		if (enabled)
		{
			table = value;

			simulateReceiveMsg = new String[table.getRowCount()];
			simulateTransmitMsg = new String[table.getRowCount()];
			noOfSimulateReceiveMsg = 0;
			for (int loop = 0; loop < simulateReceiveMsg.length; loop++)
			{
				if (table.getValueAt(loop, 0) != null)
				{
					simulateReceiveMsg[loop] = table.getValueAt(loop, 0).toString().substring(table.getValueAt(loop, 0).toString().indexOf(":") + 1).trim();
					simulateTransmitMsg[loop] = table.getValueAt(loop, 1).toString().substring(table.getValueAt(loop, 1).toString().indexOf(":") + 1).trim();
					noOfSimulateReceiveMsg++;
				}
			}
		}
		else
		{
			noOfSimulateReceiveMsg = 0;
		}

	}

	/**
	 * @param id
	 *            <b>int</b> identifying the specific instance of the
	 *            Network-class. While having only a single instance,
	 *            {@link #id} is irrelevant. However, having more than one open
	 *            connection (using more than one instance of Network),
	 *            {@link #id} helps identifying which Serial connection a
	 *            message or a log entry came from.
	 * 
	 * @param contact
	 *            Link to the instance of the class implementing
	 *            {@link net.Network_iface}.
	 * 
	 * @param divider
	 *            A small <b>int</b> representing the number to be used to
	 *            distinguish between two consecutive packages. It can take a
	 *            value between 0 and 255. Note that data is only sent to
	 *            {@link net.Network_iface#parseInput(int, int, int[])} once the
	 *            following {@link #divider} could be identified.
	 */
	// public CommsManager(int id, Network_iface contact, int divider) {
	public CommsManager(int id, int divider)
	{
		// this.contact = contact;
		this.divider = divider;
		if (this.divider > 255)
			this.divider = 255;
		if (this.divider < 0)
			this.divider = 0;
	}

	/**
	 * Just as {@link #Network(int, Network_iface, int)}, but with a default
	 * {@link #divider} of <b>255</b>.
	 * 
	 * @see #Network(int, Network_iface, int)
	 */
	// public CommsManager(int id, Network_iface contact) {
	public CommsManager(int id)
	{
		// this(id, contact, 255);
		this(id, 255);
	}

	/**
	 * Just as {@link #Network(int, Network_iface, int)}, but with a default
	 * {@link #divider} of <b>255</b> and a default {@link #id} of 0. This
	 * constructor may mainly be used if only one Serial connection is needed at
	 * any time.
	 * 
	 * @see #Network(int, Network_iface, int)
	 */
	// public CommsManager(Network_iface contact) {
	public CommsManager(JTextArea txTextPane, JTextArea rxTextPane)
	{
		// this(0, contact);
		this(0);
		txBox = txTextPane;
		rxBox = rxTextPane;
	}

	/**
	 * Just as {@link #Network(int, Network_iface, int)}, but with a default
	 * {@link #divider} of <b>255</b> and a default {@link #id} of 0. This
	 * constructor may mainly be used if only one Serial connection is needed at
	 * any time.
	 * 
	 * @see #Network(int, Network_iface, int)
	 */
	public CommsManager(JTextArea txTextArea, JTextArea rxTextArea, ExtraViewer extraframe, JLabel status)
	{
		this(0);
		txBox = txTextArea;
		rxBox = rxTextArea;
		extraViewerFrame = extraframe;
//		extraBox = extraTextArea;
		statusLabel = status;
	}

	/**
	 * This method is used to get a list of all the available Serial ports
	 * (note: only Serial ports are considered). Any one of the elements
	 * contained in the returned {@link Vector} can be used as a parameter in
	 * {@link #connect(String)} or {@link #connect(String, int)} to open a
	 * Serial connection.
	 * 
	 * @return A {@link Vector} containing {@link String}s showing all available
	 *         Serial ports.
	 */
	@SuppressWarnings("unchecked")
	public Vector<String> getPortList()
	{
		Vector<String> portVect = new Vector<String>();
		for (SerialPort port : SerialPort.getCommPorts())
		{
			portVect.add(port.getSystemPortName());
		}
		// contact.writeLog(id, "found the following ports:");
		System.out.println("found the following ports:");
		for (int i = 0; i < portVect.size(); i++)
		{
			// contact.writeLog(id, ("   " + (String) portVect.elementAt(i)));
			System.out.println("   " + (String) portVect.elementAt(i));
		}

		return portVect;
	}

	/**
	 * Method to calculate the XOR checksum from a given space seperated Hex
	 * string (10 20 30...)
	 * 
	 * @param msg
	 *            The message to calculate the checksum from
	 * @return checksum The calculated checksum byte value in string fromat
	 */
	public String CheckNSPMessageLength(String headerMsg, String bodyMsg)
	{
		byte[] header = hexStringToByteArray(headerMsg);
		byte[] body = hexStringToByteArray(bodyMsg);

		header[5] = (byte) body.length;
		return ByteArrayTohexString(header, header.length).toUpperCase();
	}

	/**
	 * Method to calculate the XOR checksum from a given space seperated Hex
	 * string (10 20 30...)
	 * 
	 * @param msg
	 *            The message to calculate the checksum from
	 * @return checksum The calculated checksum byte value in string fromat
	 */
	public String CheckNSPMessageLengthCRC(String headerMsg, String bodyMsg)
	{
		byte[] header = hexStringToByteArray(headerMsg);
		byte[] body = hexStringToByteArray(bodyMsg);

		int length = body.length;
		header[9] = (byte) ((length & 0xFF00) >> 8);
		header[8] = (byte) ((length & 0x00FF) >> 0);

		// header[5] = (byte) body.length;
		return ByteArrayTohexString(header, header.length).toUpperCase();
	}

	/**
	 * Method to calculate the XOR checksum from a given space seperated Hex
	 * string (10 20 30...)
	 * 
	 * @param msg
	 *            The message to calculate the checksum from
	 * @return checksum The calculated checksum byte value in string fromat
	 */
	public String CheckNSPMessage(MessageData data)
	{
		String msg = null;

		if (data.protocolType == ProtocolType.NSP)
		{
			data.Header = data.Header.trim();

			if (data.Header.length() != 17)
			{
				statusLabel.setText("Invalid NSP format!");
				return "error";
			}

			data.Header = CheckNSPMessageLength(data.Header, data.Body).toUpperCase().trim();
			data.HeaderCS = GetNSPChecksumXOR(data.Header).toUpperCase().trim();

			if (data.Body.isEmpty())
			{
				msg = data.MsgName + ": " + data.Header + " " + data.HeaderCS;
			}
			else
			{
				data.BodyCS = GetNSPChecksumXOR(data.Body).toUpperCase().trim();

				msg = data.MsgName + ": " + data.Header + " " + data.HeaderCS + " " + data.Body + " " + data.BodyCS;
			}
		}
		if (data.protocolType == ProtocolType.NSP2)
		{
			data.Header = data.Header.trim();

			if (data.Header.length() != 29)
			{
				statusLabel.setText("Invalid NSP2 format!");
				return "error";
			}

			data.Header = CheckNSPMessageLengthCRC(data.Header, data.Body).toUpperCase().trim();
			data.HeaderCS = GetNSPChecksumCRC16(data.Header).toUpperCase().trim();

			if (data.Body.isEmpty())
			{
				msg = data.MsgName + ": " + data.Header + " " + data.HeaderCS;
			}
			else
			{
				data.BodyCS = GetNSPChecksumCRC16(data.Body).toUpperCase().trim();

				msg = data.MsgName + ": " + data.Header + " " + data.HeaderCS + " " + data.Body + " " + data.BodyCS;
			}
		}
		else if ((data.protocolType == ProtocolType.HEX) || (data.protocolType == ProtocolType.TEXT))
		{
			if (data.Body.isEmpty())
			{
				statusLabel.setText("Invalid HEX format!");
				return "error";
			}
			else
			{
				msg = data.MsgName + ": " + data.Body;
			}
		}
		return msg;
	}

	/**
	 * Method to calculate the XOR checksum from a given space seperated Hex
	 * string (10 20 30...)
	 * 
	 * @param msg
	 *            The message to calculate the checksum from
	 * @return checksum The calculated checksum byte value in string fromat
	 */
	public String GetNSPChecksumXOR(String msg)
	{
		byte[] tmp = hexStringToByteArray(msg);
		byte[] CS = new byte[1];

		for (int loop = 0; loop < tmp.length; loop++)
		{
			CS[0] ^= tmp[loop];
		}

		String checksum = ByteArrayTohexString(CS, 1);

		return checksum;
	}

	/**
	 * Method to calculate the CCITT CRC-16 checksum from a given space
	 * separated Hex string (10 20 30...)
	 * 
	 * @param msg
	 *            The message to calculate the checksum from
	 * @return checksum The calculated checksum byte value in string format
	 */
	public String GetNSPChecksumCRC16(String msg)
	{
		byte[] tmp = hexStringToByteArray(msg);
		int crc = CRC_PRESET;

		int i;
		byte j;

		for (i = 0; i < tmp.length; i++) /*
										 * cnt = number of protocol bytes
										 * without CRC
										 */
		{
			int value = (tmp[i] & 0xff);
			crc ^= value;

			for (j = 0; j < 8; j++)
			{
				if ((crc & 0x0001) == 0x0001)
					crc = (crc >>> 1) ^ CRC_POLYNOM;
				else
					crc = (crc >>> 1);
			}
		}

		String checksum = IntTohexString(crc, 2);

		return checksum;
	}

	/**
	 * Opening a connection to the specified Serial port, using the specified
	 * speed. After opening the port, messages can be sent using
	 * {@link #writeData(String)} and received data will be packed into packets
	 * (see {@link #divider}) and forwarded using
	 * {@link net.Network_iface#parseInput(int, int, int[])}.
	 * 
	 * @param portName
	 *            The name of the port the connection should be opened to (see
	 *            {@link #getPortList()}).
	 * @param baudrate
	 *            The desired speed of the connection in bps.
	 * @return <b>true</b> if the connection has been opened successfully,
	 *         <b>false</b> otherwise.
	 */
	public boolean connect(String portName, int baudrate, int parity, int databits, int stopbits, int flowcontrol, MessageData data)
	{
		boolean conn = false;
		try
		{
			protocolType = data.protocolType;
			interfaceType = data.interfaceType;
			appendNewLine = data.appendNewline;
			appendNewLineTimeout = data.appendNewLineTimeout;

			serialPort = SerialPort.getCommPort(portName);
			if (!serialPort.setComPortParameters(baudrate, databits, SerialSettings.stopBits(stopbits), SerialSettings.parity(parity)))
				throw new IllegalStateException("Serial-port parameters are not supported");
			if (!serialPort.setFlowControl(SerialSettings.flowControl(flowcontrol)))
				throw new IllegalStateException("Serial-port flow control is not supported");
			if (!serialPort.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0))
				throw new IllegalStateException("Serial-port timeout mode is not supported");

			if (!serialPort.openPort())
			{
				System.out.println("Error: Port could not be opened");
				if (statusLabel != null)
					statusLabel.setText("Port could not be opened (it may be in use)");
				serialPort = null;
			}
			else
			{
				inStream = serialPort.getInputStream();
				outStream = serialPort.getOutputStream();
				inStreamThread = (new Thread(new InputStreamReader(inStream, null, false), "UPMC serial reader"));
				threadEnd = false;
				inStreamThread.start();

				System.out.println("Connection on " + portName + " established");
				if (statusLabel != null)
					statusLabel.setText("Connection on " + portName + " established");

				propSupport.firePropertyChange("CommsManager", connection, CONNECTION_STARTED);
				connection = CONNECTION_STARTED;
				conn = true;
			}
		}
		catch (Exception e)
		{
			if (serialPort != null && serialPort.isOpen())
				serialPort.closePort();
			serialPort = null;
			if (statusLabel != null)
				statusLabel.setText("The connection could not be made: " + e.getMessage());
			e.printStackTrace();
		}
		return conn;
	}

	/**
	 * Opening a connection to the specified Tcp port, using the specified IP
	 * Address and host port. After opening the port, messages can be sent using
	 * {@link #writeData(String)} and received data will be packed into packets
	 * (see {@link #divider}) and forwarded using
	 * {@link net.Network_iface#parseInput(int, int, int[])}.
	 * 
	 * @param host
	 *            The host IP address to connect to (see {@link #getPortList()}
	 *            ).
	 * @param port
	 *            The desired port to use when connecting to the host
	 * @return <b>true</b> if the connection has been opened successfully,
	 *         <b>false</b> otherwise.
	 */
	public TcpServer.State connect(String host, int port, MessageData data)
	{
		protocolType = data.protocolType;
		interfaceType = data.interfaceType;
		tcpMode = data.tcpMode;
		appendNewLine = data.appendNewline;
		appendNewLineTimeout = data.appendNewLineTimeout;

		if (data.tcpMode == PortSettings.TCP_CLIENT)
		{
			try
			{
				tcpClientSocket = new Socket(host, port);
				inStream = new DataInputStream(tcpClientSocket.getInputStream());
				outStream = new DataOutputStream(tcpClientSocket.getOutputStream());
				inStreamThread = (new Thread(new InputStreamReader(inStream, null, true), "UPMC TCP client reader"));
				threadEnd = false;
				inStreamThread.start();

				// propSupport.firePropertyChange("State", "STOPPED",
				// "STARTED");
				propSupport.firePropertyChange("CommsManager", connection, CONNECTION_STARTED);
				connection = CONNECTION_STARTED;

				if (statusLabel != null)
					statusLabel.setText("Connection on " + host + " established");
				conn = TcpServer.State.STARTED;
			}
			catch (UnknownHostException e)
			{
				setStatus("Unknown host: " + host);
				conn = TcpServer.State.STOPPED;
			}
			catch (IOException e)
			{
				setStatus("No I/O to host " + host);
				conn = TcpServer.State.STOPPED;
			}
			catch (Exception e)
			{
				setStatus("Whoops! It didn't work (Client)!");
				conn = TcpServer.State.STOPPED;
			}
		}
		else if (data.tcpMode == PortSettings.TCP_SERVER)
		{
			try
			{
				tcpServer = new TcpServer(); // Create the server
				tcpServer.setPort(port); // Set the port
				tcpServer.setExecutor(null); // Accept sessions in arrival order
				TcpServer.setLoggingLevel(Level.SEVERE);

				tcpServer.addPropertyChangeListener("state", new PropertyChangeListener()
				{
					@Override
					public void propertyChange(PropertyChangeEvent evt)
					{
						final String prop = evt.getPropertyName();
						final Object oldVal = evt.getOldValue();
						final Object newVal = evt.getNewValue();

						if (TcpServer.STATE_PROP.equals(prop))
						{
							final TcpServer.State state = (TcpServer.State) newVal;
							SwingUtilities.invokeLater(new Runnable()
							{
								public void run()
								{
									switch (state)
									{
									case STARTING:
										setStatus("Starting");
										conn = TcpServer.State.STARTING;
										break;
									case STARTED:
										updateServerStatus();
										conn = TcpServer.State.STARTED;
										connectState = true;
										break;
									case STOPPING:
										setStatus("Stopping");
										conn = TcpServer.State.STOPPING;
										break;
									case STOPPED:
										setStatus("Stopped");
										conn = TcpServer.State.STOPPED;
										connectState = false;
										break;
										default:
											assert false : state;
											break;
									} // end switch
								} // end run
							});
						} // end if(TcpServer.STATE_PROP.equals(prop))

						if (TcpServer.STATE_PROP.equals(prop))
						{
							String newConnection = connection;
							TcpServer.State state = (TcpServer.State) newVal;
							if (state == TcpServer.State.STARTING)
								newConnection = CONNECTION_STARTING;
							else if (state == TcpServer.State.STARTED)
								newConnection = CONNECTION_STARTED;
							else if (state == TcpServer.State.STOPPING)
								newConnection = CONNECTION_STOPPING;
							else if (state == TcpServer.State.STOPPED)
								newConnection = CONNECTION_STOPPED;
							propSupport.firePropertyChange("CommsManager", connection, newConnection);
							connection = newConnection;
						}

						if (TcpServer.PORT_PROP.equals(evt.getPropertyName()))
						{
							SwingUtilities.invokeLater(new Runnable()
							{
								public void run()
								{
									// portField.setValue( newVal );
								} // end run
							}); // end swing utilities
						} // end if: port
					} // end prop change
				}); // end add property change

				tcpServer.addTcpServerListener(new TcpServer.Listener()
				{ // Add listener
							@Override
							public void socketReceived(TcpServer.Event evt)
							{ // New stream
								try
								{
									TcpClientSession session = new TcpClientSession(evt.getSocket());
									tcpServerClients.add(session);
									updateServerStatus();
									session.start();
								}
								catch (Exception e)
								{
									e.printStackTrace();
								}
							} // end socketReceived
						}); // end Listener
				tcpServer.start();
			}
			catch (Exception e)
			{
				setStatus("Whoops! It didn't work (Server)!");
				e.printStackTrace();
				conn = TcpServer.State.STOPPED;
			}
		}
		return conn;
	}

	/**
	 * A separate class to use as the {@link net.Network#reader}. It is run as a
	 * separate {@link Thread} and manages the incoming data, packaging them
	 * using {@link net.Network#divider} into arrays of <b>int</b>s and
	 * forwarding them using
	 * {@link net.Network_iface#parseInput(int, int, int[])}.
	 * 
	 */
	private class InputStreamReader implements Runnable
	{
		private final InputStream in;
		private final TcpClientSession session;
		private final boolean blockingInput;
		boolean msg_start = false;
		int bufLength = 0;
		private Timer timeout;
		private Timer newLineTimeout;
		private boolean rx_busy = false;
		private int data_len = 0;
		private boolean debug_rx = false;

		public InputStreamReader(InputStream in, TcpClientSession session, boolean blockingInput)
		{
			this.in = in;
			this.session = session;
			this.blockingInput = blockingInput;
		}

		public void run()
		{
			byte[] buffer = new byte[1038];
			byte[] rxBuf = new byte[1038];

			int len = -1;
			if (session == null)
				connectState = true;

			timeout = new Timer(250, new ActionListener()
			{
				public void actionPerformed(ActionEvent arg0)
				{
					if ((protocolType == ProtocolType.NSP) || (protocolType == ProtocolType.NSP2))
					{
						msg_start = false;
						rx_busy = false;
						bufLength = 0;
						timeout.stop();
						statusLabel.setText("Message Timeout!");
						if (debug_rx)
							rxBox.append("Message Timeout!\n");
					}
					else
					{
						timeout.stop();
						rxBox.append(("\n"));
						rxBox.setCaretPosition(rxBox.getText().length() - 1);
					}
				}
			});

			if (appendNewLine)
			{
				newLineTimeout = new Timer(appendNewLineTimeout, new ActionListener()
				{
					public void actionPerformed(ActionEvent arg0)
					{
						newLineTimeout.stop();
						rxBox.append(("\n"));
						rxBox.setCaretPosition(rxBox.getText().length() - 1);
						
						try
						{
							if(extraView)
							{
								extraViewerFrame.AppendMessage("\n");
							}
							
							if (logTofile)
							{
								logfile.append('\n');
								msg_start = false;
							}
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}
					}
				});
			}

			try
			{
				while (session != null ? session.open : !threadEnd)
				{
					if (blockingInput || (in.available()) > 0)
					{
						String rxStr = "";
						len = this.in.read(buffer);
						if (len < 0)
							break;
						if (len > 0)
						{
							// //////////////////////////////----NSP1----//////////////////////////////////////

							if (protocolType == ProtocolType.NSP)
							{
								timeout.restart();
								if ((buffer[0] == 0x10) && (!msg_start))
								{
									msg_start = true;
									for (int loop = 0; loop < len; loop++)
										rxBuf[loop] = buffer[loop];

									bufLength = len;
									data_len = 0;

									if (debug_rx)
									{
										rxStr = ByteArrayTohexString(rxBuf, bufLength).trim();
										txBox.append(("NSP1.1 " + rxStr + "\n").toUpperCase());
										txBox.setCaretPosition(txBox.getText().length());
									}
								}
								else if (msg_start)
								{
									for (int loop = 0; loop < len; loop++)
										rxBuf[bufLength + loop] = buffer[loop];

									bufLength += len;
									data_len = 0;

									if (debug_rx)
									{
										rxStr = ByteArrayTohexString(rxBuf, bufLength).trim();
										txBox.append(("NSP1.2 " + rxStr + "\n").toUpperCase());
										txBox.setCaretPosition(txBox.getText().length());
									}
								}

								if (bufLength >= NSP1_HEADER_LEN)
								{
									data_len = (rxBuf[5] & 0xFF); // Make
																	// unsigned
																	// byte

									if ((bufLength >= data_len + NSP1_HEADER_AND_BODY_CS_LEN) || (data_len == 0))
										rx_busy = true;
									else
										rx_busy = false;
								}

								while (rx_busy)
								{
									// When there is a current message being
									// received, and when the message has body
									if ((msg_start) && (data_len > 0) && (bufLength >= (NSP1_HEADER_AND_BODY_CS_LEN + data_len)))
									{
										rxStr = ByteArrayTohexString(rxBuf, NSP1_HEADER_AND_BODY_CS_LEN + data_len).trim();

										String calcXORHeader = GetNSPChecksumXOR(new String(rxStr).trim().substring(0, 18)).trim();
										String rxXORHeader = new String((rxStr).trim().substring(18, 20)).trim();

										String calcXORData = GetNSPChecksumXOR(new String(rxStr).trim().substring(21, rxStr.length() - 3)).trim();
										String rxXORData = new String((rxStr).trim().substring(rxStr.length() - 3, rxStr.trim().length())).trim();

										if (!calcXORHeader.equals(rxXORHeader))
										setStatus("NSP1 Header Checksum Error!");
									else if (!calcXORData.equals(rxXORData))
										setStatus("NSP1 Data Checksum Error!");
									else
									{
										setStatus("NSP1 Message Received");
											chkSimulation(rxStr);
										}

									appendReceived((rxStr + "\n").toUpperCase());

										// Subtract the handled message from the
										// buffer and check for a new start of
										// massage
										bufLength -= NSP1_HEADER_AND_BODY_CS_LEN + data_len;

										if (bufLength > NSP1_HEADER_LEN)
										{
											for (int loop = 0; loop < bufLength; loop++)
												rxBuf[loop] = rxBuf[loop + NSP1_HEADER_AND_BODY_CS_LEN + data_len];

											data_len = (rxBuf[5] & 0xFF);

											if ((bufLength >= data_len + NSP1_HEADER_AND_BODY_CS_LEN) || (data_len == 0))
												rx_busy = true;
											else
												rx_busy = false;

											if (debug_rx)
												txBox.append("(NSP1 Bdy>0) bufLength > HdrLen\n");
										}
										else if (bufLength == NSP1_HEADER_LEN)
										{
											for (int loop = 0; loop < bufLength; loop++)
												rxBuf[loop] = rxBuf[loop + NSP1_HEADER_AND_BODY_CS_LEN + data_len];

											data_len = (rxBuf[5] & 0xFF);

											if (data_len == 0)
												rx_busy = true;
											else
												rx_busy = false;

											if (debug_rx)
												txBox.append("(NSP1 Bdy>0) bufLength = HdrLen\n");
										}
										else if (bufLength > 0)
										{
											for (int loop = 0; loop < bufLength; loop++)
												rxBuf[loop] = rxBuf[loop + NSP1_HEADER_AND_BODY_CS_LEN + data_len];

											rx_busy = false;
											timeout.stop();

											if (debug_rx)
												txBox.append("(NSP1 Bdy>0) bufLength > 0\n");
										}
										else
										{
											msg_start = false;
											rx_busy = false;
											timeout.stop();

											if (debug_rx)
												txBox.append("(NSP1 Bdy>0) bufLength <= 0\n");
										}

										if (logTofile)
										{
											logfile.write(dateFormat.format(new Date()) + ": <-- " + rxStr + "\n");
										}
									}

									// When there is a current message being
									// received, and when the message has no
									// body
									if ((msg_start) && (data_len == 0) && (bufLength >= NSP1_HEADER_LEN))
									{
										rxStr = ByteArrayTohexString(rxBuf, NSP1_HEADER_LEN).trim();

										String calcCRC = GetNSPChecksumXOR(new String(rxStr).trim().substring(0, rxStr.length() - 3)).trim();
										String rxCRC = new String((rxStr).trim().substring(rxStr.length() - 3, rxStr.length())).trim();

										if (!calcCRC.equals(rxCRC))
										setStatus("NSP1 Header Checksum Error!");
									else
									{
										setStatus("NSP1 Message Received");
											chkSimulation(rxStr);
										}

									appendReceived((rxStr + "\n").toUpperCase());

										// Subtract the handled message from the
										// buffer and check for a new start of
										// massage
										bufLength -= NSP1_HEADER_LEN;

										if (bufLength > NSP1_HEADER_LEN)
										{
											for (int loop = 0; loop < bufLength; loop++)
												rxBuf[loop] = rxBuf[loop + NSP1_HEADER_LEN];

											data_len = (rxBuf[5] & 0xFF);

											if ((bufLength >= data_len + NSP1_HEADER_AND_BODY_CS_LEN) || (data_len == 0))
												rx_busy = true;
											else
												rx_busy = false;

											if (debug_rx)
												txBox.append("(NSP1 Bdy=0) bufLength > HdrLen\n");
										}
										else if (bufLength == NSP1_HEADER_LEN)
										{
											for (int loop = 0; loop < bufLength; loop++)
												rxBuf[loop] = rxBuf[loop + NSP1_HEADER_LEN];

											data_len = (rxBuf[5] & 0xFF);

											if (data_len == 0)
												rx_busy = true;
											else
												rx_busy = false;

											if (debug_rx)
												txBox.append("(NSP1 Bdy=0) bufLength = HdrLen\n");
										}
										else if (bufLength > 0)
										{
											for (int loop = 0; loop < bufLength; loop++)
												rxBuf[loop] = rxBuf[loop + NSP1_HEADER_LEN];

											rx_busy = false;
											timeout.stop();

											if (debug_rx)
												txBox.append("(NSP1 Bdy=0) bufLength > 0\n");
										}
										else
										{
											msg_start = false;
											rx_busy = false;
											timeout.stop();

											if (debug_rx)
												txBox.append("(NSP1 Bdy=0) bufLength <= 0\n");
										}

										if (logTofile)
										{
											logfile.write(dateFormat.format(new Date()) + ": <-- " + rxStr + "\n");
										}
									}
								}
							}

							// ////////////////////////////////----NSP2----//////////////////////////////////////

							if (protocolType == ProtocolType.NSP2)
							{
								timeout.restart();
								if ((buffer[0] == 0x20) && (!msg_start))
								{
									msg_start = true;
									for (int loop = 0; loop < len; loop++)
										rxBuf[loop] = buffer[loop];

									bufLength = len;
									data_len = 0;

									if (debug_rx)
									{
										rxStr = ByteArrayTohexString(rxBuf, bufLength).trim();
										txBox.append(("NSP2" + rxStr + "\n").toUpperCase());
										txBox.setCaretPosition(txBox.getText().length());
									}
								}
								else if (msg_start)
								{
									for (int loop = 0; loop < len; loop++)
										rxBuf[bufLength + loop] = buffer[loop];

									bufLength += len;
									data_len = 0;

									if (debug_rx)
									{
										rxStr = ByteArrayTohexString(rxBuf, bufLength).trim();
										txBox.append(("NSP2" + rxStr + "\n").toUpperCase());
										txBox.setCaretPosition(txBox.getText().length());
									}
								}

								if (bufLength >= NSP2_HEADER_LEN)
								{
									data_len = ((rxBuf[9] & 0xFF) << 8) + (rxBuf[8] & 0xFF);

									if ((bufLength >= data_len + NSP2_HEADER_AND_BODY_CS_LEN) || (data_len == 0))
										rx_busy = true;
									else
										rx_busy = false;
								}

								while (rx_busy)
								{
									// When there is a current message being
									// received, and when the message has body
									if ((msg_start) && (data_len > 0) && (bufLength >= (NSP2_HEADER_AND_BODY_CS_LEN + data_len)))
									{
										rxStr = ByteArrayTohexString(rxBuf, NSP2_HEADER_AND_BODY_CS_LEN + data_len).trim();

										String calcCRCHeader = GetNSPChecksumCRC16(new String(rxStr).trim().substring(0, 30)).trim();
										String rxCRCHeader = new String((rxStr).trim().substring(30, 36)).trim();

										String calcCRCData = GetNSPChecksumCRC16(new String(rxStr).trim().substring(36, rxStr.length() - 5)).trim();
										String rxCRCData = new String((rxStr).trim().substring(rxStr.length() - 5, rxStr.length())).trim();

										if (!calcCRCHeader.equals(rxCRCHeader))
										setStatus("NSP2 Header Checksum Error!");
									else if (!calcCRCData.equals(rxCRCData))
										setStatus("NSP2 Data Checksum Error!");
									else
									{
										setStatus("NSP2 Message Received");
											chkSimulation(rxStr);
										}

									appendReceived((rxStr + "\n").toUpperCase());

										// Subtract the handled message from the
										// buffer and check for a new start of
										// massage
										bufLength -= NSP2_HEADER_AND_BODY_CS_LEN + data_len;

										if (bufLength > NSP2_HEADER_LEN)
										{
											for (int loop = 0; loop < bufLength; loop++)
												rxBuf[loop] = rxBuf[loop + NSP2_HEADER_AND_BODY_CS_LEN + data_len];

											data_len = ((rxBuf[9] & 0xFF) << 8) + (rxBuf[8] & 0xFF);

											if ((bufLength >= data_len + NSP2_HEADER_AND_BODY_CS_LEN) || (data_len == 0))
												rx_busy = true;
											else
												rx_busy = false;

											if (debug_rx)
												txBox.append("(NSP2 Bdy>0) bufLength > HdrLen\n");
										}
										else if (bufLength == NSP2_HEADER_LEN)
										{
											for (int loop = 0; loop < bufLength; loop++)
												rxBuf[loop] = rxBuf[loop + NSP2_HEADER_AND_BODY_CS_LEN + data_len];

											data_len = ((rxBuf[9] & 0xFF) << 8) + (rxBuf[8] & 0xFF);

											if (data_len == 0)
												rx_busy = true;
											else
												rx_busy = false;

											if (debug_rx)
												txBox.append("(NSP2 Bdy>0) bufLength = HdrLen\n");
										}
										else if (bufLength > 0)
										{
											for (int loop = 0; loop < bufLength; loop++)
												rxBuf[loop] = rxBuf[loop + NSP2_HEADER_AND_BODY_CS_LEN + data_len];

											rx_busy = false;
											timeout.stop();

											if (debug_rx)
												txBox.append("(NSP2 Bdy>0) bufLength > 0\n");
										}
										else
										{
											msg_start = false;
											rx_busy = false;
											timeout.stop();

											if (debug_rx)
												txBox.append("(NSP2 Bdy>0) bufLength <= 0\n");
										}

										if (logTofile)
										{
											logfile.write(dateFormat.format(new Date()) + ": <-- " + rxStr + "\n");
										}
									}

									// When there is a current message being
									// received, and when the message has no
									// body
									if ((msg_start) && (data_len == 0) && (bufLength >= NSP2_HEADER_LEN))
									{
										rxStr = ByteArrayTohexString(rxBuf, NSP2_HEADER_LEN).trim();

										String calcCRC = GetNSPChecksumCRC16(new String(rxStr).trim().substring(0, rxStr.length() - 5)).trim();
										String rxCRC = new String((rxStr).trim().substring(rxStr.length() - 5, rxStr.length())).trim();

										if (!calcCRC.equals(rxCRC))
										setStatus("NSP2 Header Checksum Error!");
									else
									{
										setStatus("NSP2 Message Received");
											chkSimulation(rxStr);
										}

									appendReceived((rxStr + "\n").toUpperCase());

										// Subtract the handled message from the
										// buffer and check for a new start of
										// massage
										bufLength -= NSP2_HEADER_LEN;

										if (bufLength > NSP2_HEADER_LEN)
										{
											for (int loop = 0; loop < bufLength; loop++)
												rxBuf[loop] = rxBuf[loop + NSP2_HEADER_LEN];

											data_len = ((rxBuf[9] & 0xFF) << 8) + (rxBuf[8] & 0xFF);

											if ((bufLength >= data_len + NSP2_HEADER_AND_BODY_CS_LEN) || (data_len == 0))
												rx_busy = true;
											else
												rx_busy = false;

											if (debug_rx)
												txBox.append("(NSP2 Bdy=0) bufLength > HdrLen\n");
										}
										else if (bufLength == NSP2_HEADER_LEN)
										{
											for (int loop = 0; loop < bufLength; loop++)
												rxBuf[loop] = rxBuf[loop + NSP2_HEADER_LEN];

											data_len = ((rxBuf[9] & 0xFF) << 8) + (rxBuf[8] & 0xFF);

											if (data_len == 0)
												rx_busy = true;
											else
												rx_busy = false;

											if (debug_rx)
												txBox.append("(NSP2 Bdy=0) bufLength = HdrLen\n");
										}
										else if (bufLength > 0)
										{
											for (int loop = 0; loop < bufLength; loop++)
												rxBuf[loop] = rxBuf[loop + NSP2_HEADER_LEN];

											rx_busy = false;
											timeout.stop();

											if (debug_rx)
												txBox.append("(NSP2 Bdy=0) bufLength > 0\n");
										}
										else
										{
											msg_start = false;
											rx_busy = false;
											timeout.stop();

											if (debug_rx)
												txBox.append("(NSP2 Bdy=0) bufLength <= 0\n");
										}

										if (logTofile)
										{
											logfile.write(dateFormat.format(new Date()) + ": <-- " + rxStr + "\n");
										}
									}
								}
							}

							// //////////////////////////////----HEX----//////////////////////////////////////

							else if (protocolType == ProtocolType.HEX)
							{
								if (appendNewLine)
									newLineTimeout.restart(); // Restart the
																// newline timer

								if (!msg_start)
								{
									msg_start = true;
									if (logTofile)
									{
										logfile.write(dateFormat.format(new Date()) + ": <-- ");
									}
								}

								for (int loop = 0; loop < len; loop++)
									rxBuf[bufLength + loop] = buffer[loop];
								bufLength += len;

								rxStr = ByteArrayTohexString(rxBuf, bufLength);
								
								appendReceived(rxStr.toUpperCase());
								
								if(extraView)
								{
									String s = new String(rxBuf, 0, bufLength);
									//System.out.println(s);
									extraViewerFrame.AppendMessage(s);
								}
								
								bufLength = 0;
								if (logTofile)
								{
									logfile.write(rxStr.toUpperCase());
								}
							}

							// //////////////////////////////----TEXT----//////////////////////////////////////

							if (protocolType == ProtocolType.TEXT)
							{
								if (appendNewLine)
									newLineTimeout.restart(); // Restart the
																// newline timer

								if (!msg_start)
								{
									msg_start = true;
									if (logTofile)
									{
										logfile.write(dateFormat.format(new Date()) + ": <-- ");
									}
								}

								rxStr = new String(buffer, 0, len);
								appendReceived(rxStr);
								if (logTofile)
								{
									logfile.write(rxStr);
								}
							}
						}
					}
					else
					{
						try
						{
							Thread.sleep(10);
						}
						catch (InterruptedException e)
						{
							// We've been interrupted: no more messages.
							return;
						}
					}
				}
			}
			catch (IOException e)
			{
				if (session == null && !threadEnd)
					handleConnectionInterrupted();
			}
			finally
			{
				if (timeout != null)
					timeout.stop();
				if (newLineTimeout != null)
					newLineTimeout.stop();
				if (session != null)
					removeServerClient(session);
				else if (blockingInput && !threadEnd && connectState)
					handleConnectionInterrupted();
			}
		}

		/**
		 * @param rxStr
		 */
		private void chkSimulation(String rxStr)
		{
			if (simulateReceiveMsg != null)
			{
				for (int loop = 0; loop < noOfSimulateReceiveMsg; loop++)
				{
					if (rxStr.trim().toUpperCase().equals(simulateReceiveMsg[loop].toUpperCase()))
					{
						if (session != null)
							writeDataToSession(session, simulateTransmitMsg[loop]);
						else
							writeData(simulateTransmitMsg[loop]);
						return;
					}
				}
			}
		}
	}

	private void handleConnectionInterrupted()
	{
		connectState = false;
		setStatus("Connection has been interrupted");
		propSupport.firePropertyChange("CommsManager", connection, CONNECTION_INTERRUPT);
		connection = CONNECTION_INTERRUPT;
	}

	private void removeServerClient(TcpClientSession session)
	{
		session.close();
		if (tcpServerClients.remove(session))
			updateServerStatus();
	}

	private void updateServerStatus()
	{
		int count = tcpServerClients.size();
		setStatus("TCP Server - " + count + (count == 1 ? " client connected" : " clients connected"));
	}

	private void setStatus(final String text)
	{
		if (statusLabel == null)
			return;
		if (SwingUtilities.isEventDispatchThread())
			statusLabel.setText(text);
		else
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					statusLabel.setText(text);
				}
			});
	}

	private void appendReceived(final String text)
	{
		if (rxBox == null)
			return;
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				checkRxBoxMaxLines();
				rxBox.append(text);
				rxBox.setCaretPosition(rxBox.getText().length());
			}
		});
	}

	int getTcpServerClientCount()
	{
		return tcpServerClients.size();
	}

	private boolean writeDataToSession(TcpClientSession session, String msg)
	{
		try
		{
			byte[] data = hexStringToByteArray(msg);
			session.write(data);
			recordTransmission(data);
			return true;
		}
		catch (IOException e)
		{
			removeServerClient(session);
			return false;
		}
	}

	private void recordTransmission(final byte[] data)
	{
		final String message = ByteArrayTohexString(data, data.length).toUpperCase();
		if (txBox != null)
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					checkTxBoxMaxLines();
					txBox.append(message + "\n");
					txBox.setCaretPosition(txBox.getText().length());
				}
			});
		}
		if (logTofile)
		{
			try
			{
				logfile.write(dateFormat.format(new Date()) + ": --> " + message + "\n");
			}
			catch (IOException e)
			{
				LOGGER.log(Level.WARNING, "Could not write TCP simulator response to the log", e);
			}
		}
	}

	private boolean writeBytes(byte[] buffer)
	{
		if (interfaceType == INTERFACE_TCP && tcpMode == PortSettings.TCP_SERVER)
		{
			boolean success = false;
			for (TcpClientSession session : tcpServerClients)
			{
				try
				{
					session.write(buffer);
					success = true;
				}
				catch (IOException e)
				{
					removeServerClient(session);
				}
			}
			return success;
		}

		if (outStream == null)
			return false;
		try
		{
			outStream.write(buffer);
			outStream.flush();
			return true;
		}
		catch (IOException e)
		{
			return false;
		}
	}

	/**
	 * Simple function closing the connection held by this instance of
	 * {@link net.Network}. It also ends the Thread {@link net.Network#reader}.
	 * 
	 * @return <b>true</b> if the connection could be closed, <b>false</b>
	 *         otherwise.
	 */
	public boolean disconnect()
	{
		boolean disconn = true;

		threadEnd = true;
		connectState = false;
		// try
		// {
		// inStreamThread.join(200);
		// }
		// catch (InterruptedException e1)
		// {
		// e1.printStackTrace();
		// disconn = false;
		// }

		try
		{
			if (inStream != null)
				inStream.close();
			inStream = null;
			if (outStream != null)
				outStream.close();
			outStream = null;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			disconn = false;
		}

		if (serialPort != null)
		{
			if (!serialPort.closePort())
				disconn = false;
			serialPort = null;
		}

		try
		{
			if (tcpClientSocket != null)
			{
				tcpClientSocket.close();
				tcpClientSocket = null;
			}
		}
		catch (IOException e)
		{
			tcpClientSocket = null;
		}

		for (TcpClientSession session : tcpServerClients)
			session.close();
		tcpServerClients.clear();

		if (tcpServer != null)
		{
			tcpServer.stop();
			tcpServer = null;
		}
		tcpMode = 0;

		System.out.println("Connection disconnected");
		setStatus("Connection disconnected");

		propSupport.firePropertyChange("CommsManager", connection, CONNECTION_STOPPED);
		connection = CONNECTION_STOPPED;

		return disconn;
	}

	/**
	 * @return Whether this instance of {@link net.Network} has currently an
	 *         open connection of not.
	 */
	public boolean isConnected()
	{
		return connectState;
	}

	// public boolean writeSerial(String header, String headerChecksum, String
	// body, String bodyChecksum) {
	public void writeData(String msg)
	{
		checkTxBoxMaxLines();

		boolean success = false;
		if (isConnected())
		{
			try
			{
				byte[] buffer = hexStringToByteArray(msg);

				if (!writeBytes(buffer))
				{
					if (tcpMode == PortSettings.TCP_SERVER)
						setStatus("TCP Server - no clients connected");
					else
						disconnect();
					return;
				}

				if (txBox != null)
				{
					String message = ByteArrayTohexString(buffer, buffer.length).toUpperCase();
					txBox.setText(txBox.getText() + message + "\n");
					txBox.setCaretPosition(txBox.getText().length());
					if (logTofile)
					{
						logfile.write(dateFormat.format(new Date()) + ": --> " + message + "\n");
					}
				}
			}
			catch (IOException e)
			{
				disconnect();
			}
		}
	}

	/**
	 * This method is included as a legacy. Depending on the other side of the
	 * Serial port, it might be easier to send using a String. Note: this method
	 * does not add the {@link #divider} to the end.
	 * 
	 * If a connection is open, a {@link String} can be sent over the Serial
	 * port using this function. If no connection is available, <b>false</b> is
	 * returned and a message is sent using
	 * {@link net.Network_iface#writeLog(int, String)}.
	 * 
	 * @param message
	 *            The {@link String} to be sent over the Serial connection.
	 * @return <b>true</b> if the message could be sent, <b>false</b> otherwise.
	 */
	// public boolean writeSerial(String header, String headerChecksum, String
	// body, String bodyChecksum) {
	public boolean writeData(MessageData data)
	{

		boolean success = false;
		if (isConnected())
		{
			checkTxBoxMaxLines();

			try
			{
				if ((data.protocolType == ProtocolType.NSP) || (data.protocolType == ProtocolType.NSP2))
				{
					byte[] buffer = hexStringToByteArray(data.Header + data.HeaderCS + data.Body + data.BodyCS);

					if (!writeBytes(buffer))
						throw new IOException("No writable connection");

					if (txBox != null)
					{
						String message = ByteArrayTohexString(buffer, buffer.length).toUpperCase();
						txBox.setText(txBox.getText() + message + "\n");
						if (logTofile)
						{
							logfile.write(dateFormat.format(new Date()) + ": --> " + message + "\n");
						}
					}
					success = true;
				}
				else if (data.protocolType == ProtocolType.HEX)
				{
					byte[] buffer = hexStringToByteArray(data.Body);

					if (!writeBytes(buffer))
						throw new IOException("No writable connection");

					if (txBox != null)
					{
						String message = ByteArrayTohexString(buffer, buffer.length).toUpperCase();
						txBox.setText(txBox.getText() + message + "\n");
						if (logTofile)
						{
							logfile.write(dateFormat.format(new Date()) + ": --> " + message + "\n");
						}
					}
					success = true;
				}
				else if (data.protocolType == ProtocolType.TEXT)
				{
					if (!writeBytes((data.Body + "\n").getBytes()))
						throw new IOException("No writable connection");

					if (txBox != null)
					{
						txBox.append(data.Body + "\n");
						if (logTofile)
						{
							logfile.write(dateFormat.format(new Date()) + ": --> " + data.Body + "\n");
						}
					}
					success = true;
				}
				else
				{
					setStatus("Protocol not supported, please rectify the protocol and try again");
					success = true;
				}
			}
			catch (IOException e)
			{
				if (tcpMode == PortSettings.TCP_SERVER)
					setStatus("TCP Server - no clients connected");
				else
					disconnect();
			}
		}
		else
		{
			System.out.println("No port is connected.");
			setStatus("No port is connected.");
		}
		return success;
	}

	private void checkTxBoxMaxLines()
	{
		if (txBox != null)
		{
			try
			{
				if (txBox.getLineCount() > 500)
					txBox.replaceRange(null, txBox.getLineStartOffset(0), txBox.getLineEndOffset(0));
			}
			catch (BadLocationException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void checkRxBoxMaxLines()
	{
		if (rxBox != null)
		{
			try
			{
				if (rxBox.getLineCount() > 500)
					rxBox.replaceRange(null, rxBox.getLineStartOffset(0), rxBox.getLineEndOffset(0));
			}
			catch (BadLocationException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public String ByteArrayTohexString(byte[] data, int len)
	{
		String result = "";
		for (int i = 0; i < len; i++)
		{
			result += Integer.toString((data[i] & 0xFF) + 0x100, 16).substring(1) + " ";
		}
		return result;
	}
	
	public String ByteArrayToAsciiString(byte[] data, int len)
	{
		String result = "";
		for (int i = 0; i < len; i++)
		{
			result += Integer.toString((data[i] & 0xFF) + 0x100, 0).substring(1);
		}
		return result;
	}

	public String IntTohexString(int data, int len)
	{
		String result = "";
		for (int i = 0; i < len; i++)
		{
			result += Integer.toString(((data & (0xFF << (8 * i))) >> (8 * i)) + 0x100, 16).substring(1) + " ";
		}
		return result;
	}

	public byte[] hexStringToByteArray(String s)
	{
		s = s.replace(" ", "");
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2)
		{
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
		}
		return data;
	}
}
