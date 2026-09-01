package upmc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fazecast.jSerialComm.SerialPort;
import org.junit.jupiter.api.Test;

class SerialSettingsTest
{
    @Test
    void mapsLegacyStopBits()
    {
        assertEquals(SerialPort.ONE_STOP_BIT, SerialSettings.stopBits(1));
        assertEquals(SerialPort.TWO_STOP_BITS, SerialSettings.stopBits(2));
        assertEquals(SerialPort.ONE_POINT_FIVE_STOP_BITS, SerialSettings.stopBits(3));
    }

    @Test
    void mapsLegacyParity()
    {
        assertEquals(SerialPort.NO_PARITY, SerialSettings.parity(0));
        assertEquals(SerialPort.ODD_PARITY, SerialSettings.parity(1));
        assertEquals(SerialPort.EVEN_PARITY, SerialSettings.parity(2));
    }

    @Test
    void mapsLegacyFlowControl()
    {
        assertEquals(SerialPort.FLOW_CONTROL_DISABLED, SerialSettings.flowControl(0));
        assertEquals(SerialPort.FLOW_CONTROL_RTS_ENABLED | SerialPort.FLOW_CONTROL_CTS_ENABLED,
                SerialSettings.flowControl(1));
        assertEquals(SerialPort.FLOW_CONTROL_XONXOFF_IN_ENABLED | SerialPort.FLOW_CONTROL_XONXOFF_OUT_ENABLED,
                SerialSettings.flowControl(2));
    }

    @Test
    void rejectsUnknownValues()
    {
        assertThrows(IllegalArgumentException.class, () -> SerialSettings.stopBits(0));
        assertThrows(IllegalArgumentException.class, () -> SerialSettings.parity(3));
        assertThrows(IllegalArgumentException.class, () -> SerialSettings.flowControl(3));
    }
}
