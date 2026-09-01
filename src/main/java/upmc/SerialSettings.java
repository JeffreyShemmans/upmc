package upmc;

import com.fazecast.jSerialComm.SerialPort;

/** Translates UPMC's persisted serial settings to jSerialComm constants. */
final class SerialSettings
{
    private SerialSettings()
    {
    }

    static int stopBits(int legacyStopBits)
    {
        switch (legacyStopBits)
        {
            case 1:
                return SerialPort.ONE_STOP_BIT;
            case 2:
                return SerialPort.TWO_STOP_BITS;
            case 3:
                return SerialPort.ONE_POINT_FIVE_STOP_BITS;
            default:
                throw new IllegalArgumentException("Unsupported stop bits value: " + legacyStopBits);
        }
    }

    static int parity(int legacyParity)
    {
        switch (legacyParity)
        {
            case 0:
                return SerialPort.NO_PARITY;
            case 1:
                return SerialPort.ODD_PARITY;
            case 2:
                return SerialPort.EVEN_PARITY;
            default:
                throw new IllegalArgumentException("Unsupported parity value: " + legacyParity);
        }
    }

    static int flowControl(int legacyFlowControl)
    {
        switch (legacyFlowControl)
        {
            case 0:
                return SerialPort.FLOW_CONTROL_DISABLED;
            case 1:
                return SerialPort.FLOW_CONTROL_RTS_ENABLED | SerialPort.FLOW_CONTROL_CTS_ENABLED;
            case 2:
                return SerialPort.FLOW_CONTROL_XONXOFF_IN_ENABLED | SerialPort.FLOW_CONTROL_XONXOFF_OUT_ENABLED;
            default:
                throw new IllegalArgumentException("Unsupported flow control value: " + legacyFlowControl);
        }
    }
}
