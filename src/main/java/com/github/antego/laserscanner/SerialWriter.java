package com.github.antego.laserscanner;

import javafx.application.Platform;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

import java.util.logging.Level;
import java.util.logging.Logger;

public class SerialWriter {
    private final static Logger logger = Logger.getLogger(SerialWriter.class.toString());

    private SerialPort serialPort;
    private Controller controller;

    public SerialWriter(String port, Controller controller) throws SerialPortException {
        this.controller = controller;
        serialPort = new SerialPort(port);
        logger.info("Port is opened: " + serialPort.openPort());
        logger.info("Parameters has been set: " + serialPort.setParams(9600, 8, 1, 0, true, false));
        int mask = SerialPort.MASK_RXCHAR;
        serialPort.setEventsMask(mask);
        serialPort.addEventListener(new SerialPortReader());
    }

    public boolean disconnect() {
        try {
            serialPort.removeEventListener();
        } catch (SerialPortException ex) {
            logger.log(Level.INFO, "", ex);
        }
        try {
            return serialPort.closePort();
        } catch (SerialPortException ex) {
            logger.log(Level.SEVERE, "", ex);
            return false;
        }
    }

    public boolean rotate(int steps) {
        try {
            return serialPort.writeString("r" + steps + "\r\n");
        } catch (SerialPortException ex) {
            logger.log(Level.SEVERE, "", ex);
            return false;
        }
    }

    class SerialPortReader implements SerialPortEventListener {
        StringBuilder message = new StringBuilder();

        public void serialEvent(SerialPortEvent event) {
            if (event.isRXCHAR() && event.getEventValue() > 0) {
                try {
                    byte buffer[] = serialPort.readBytes();
                    for (byte b : buffer) {
                        if ((b == '\r' || b == '\n') && message.length() > 0) {
                            if (message.toString().contentEquals("r:Done")) {
                                Thread.sleep(1000);
                                Platform.runLater(() -> controller.setTakeShoot(true));
                            }
                            logger.log(Level.INFO, message.toString());
                            message.setLength(0);
                        } else {
                            if (b != '\n' && b != '\r') message.append((char) b);
                        }
                    }
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "", ex);
                }
            }
        }
    }
}
