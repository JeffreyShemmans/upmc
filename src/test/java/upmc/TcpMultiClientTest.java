package upmc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.function.BooleanSupplier;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TcpMultiClientTest
{
    private CommsManager manager;
    private Socket firstClient;
    private Socket secondClient;
    private JLabel serverStatus;

    @AfterEach
    void cleanUp() throws IOException
    {
        if (manager != null)
            manager.disconnect();
        if (firstClient != null)
            firstClient.close();
        if (secondClient != null)
            secondClient.close();
    }

    @Test
    void serverReceivesFromBothClientsBroadcastsAndKeepsRemainingClientAlive() throws Exception
    {
        JTextArea received = new JTextArea();
        manager = startServer(CommsManager.ProtocolType.HEX, received);

        firstClient = connectClient();
        secondClient = connectClient();
        await(() -> manager.getTcpServerClientCount() == 2);
        await(() -> serverStatus.getText().contains("2 clients connected"));

        firstClient.getOutputStream().write(0x11);
        secondClient.getOutputStream().write(0x22);
        await(() -> received.getText().contains("11 ") && received.getText().contains("22 "));

        UPMC_UI.MessageData message = serverSettings(CommsManager.ProtocolType.HEX);
        message.Body = "AA";
        assertTrue(manager.writeData(message));
        assertEquals(0xAA, readByte(firstClient));
        assertEquals(0xAA, readByte(secondClient));

        firstClient.close();
        await(() -> manager.getTcpServerClientCount() == 1);
        await(() -> serverStatus.getText().contains("1 client connected"));
        assertTrue(manager.isConnected());

        message.Body = "BB";
        assertTrue(manager.writeData(message));
        assertEquals(0xBB, readByte(secondClient));
    }

    @Test
    void simulatorReplyReturnsOnlyToOriginatingClient() throws Exception
    {
        manager = startServer(CommsManager.ProtocolType.NSP, new JTextArea());
        JTable simulation = new JTable(new DefaultTableModel(
                new Object[][] { { "Receive: 10 00 00 00 00 00 10", "Transmit: AA" } },
                new Object[] { "Receive", "Transmit" }));
        manager.setDataSet(simulation, true);

        firstClient = connectClient();
        secondClient = connectClient();
        await(() -> manager.getTcpServerClientCount() == 2);

        firstClient.getOutputStream().write(new byte[] { 0x10, 0, 0, 0, 0, 0, 0x10 });
        assertEquals(0xAA, readByte(firstClient));

        secondClient.setSoTimeout(300);
        assertThrows(SocketTimeoutException.class, () -> secondClient.getInputStream().read());
    }

    @Test
    void stoppingServerClosesEveryClient() throws Exception
    {
        manager = startServer(CommsManager.ProtocolType.HEX, new JTextArea());
        firstClient = connectClient();
        secondClient = connectClient();
        await(() -> manager.getTcpServerClientCount() == 2);

        assertTrue(manager.disconnect());
        await(() -> manager.getTcpServerClientCount() == 0);
        assertFalse(manager.isConnected());
        assertEquals(-1, readByte(firstClient));
        assertEquals(-1, readByte(secondClient));
        manager = null;
    }

    private CommsManager startServer(CommsManager.ProtocolType protocol, JTextArea received) throws Exception
    {
        int port = availablePort();
        serverStatus = new JLabel();
        CommsManager result = new CommsManager(new JTextArea(), received, null, serverStatus);
        UPMC_UI.MessageData settings = serverSettings(protocol);
        result.connect("127.0.0.1", port, settings);
        serverPort = port;
        await(result::isConnected);
        return result;
    }

    private int serverPort;

    private Socket connectClient() throws IOException
    {
        Socket socket = new Socket("127.0.0.1", serverPort);
        socket.setSoTimeout(2000);
        return socket;
    }

    private static UPMC_UI.MessageData serverSettings(CommsManager.ProtocolType protocol)
    {
        UPMC_UI.MessageData settings = new UPMC_UI.MessageData();
        settings.interfaceType = CommsManager.INTERFACE_TCP;
        settings.tcpMode = PortSettings.TCP_SERVER;
        settings.protocolType = protocol;
        settings.appendNewline = false;
        return settings;
    }

    private static int availablePort() throws IOException
    {
        try (ServerSocket socket = new ServerSocket(0))
        {
            return socket.getLocalPort();
        }
    }

    private static int readByte(Socket socket) throws IOException
    {
        return socket.getInputStream().read();
    }

    private static void await(BooleanSupplier condition) throws InterruptedException
    {
        long deadline = System.currentTimeMillis() + 3000;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline)
            Thread.sleep(10);
        assertTrue(condition.getAsBoolean(), "condition did not become true before timeout");
    }
}
