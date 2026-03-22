package com.heroicrobot.dropbit.config.serial;

import com.fazecast.jSerialComm.SerialPort;
import com.heroicrobot.dropbit.config.ConfigModel;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SerialDetect {

    private static SerialPort getSerialPort(DetectedPusher dp) {
        SerialPort s = SerialPort.getCommPort(dp.port);
        s.setBaudRate(115200);
        return s;
    }

    public void eraseConfig(DetectedPusher dp) {
        SerialPort s = getSerialPort(dp);

        try (OutputStream outputStream = s.getOutputStream()) {
            outputStream.write("\r\nConfig".getBytes());
            outputStream.write(255);
            outputStream.write(255);
            outputStream.write(255);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            s.closePort();
        }
    }

    public void reboot(DetectedPusher dp) {
        SerialPort s = getSerialPort(dp);

        try (OutputStream outputStream = s.getOutputStream()) {
            outputStream.write("\r\nReboot".getBytes());
            outputStream.write(255);
            outputStream.write(255);
            outputStream.write(255);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            s.closePort();
        }
    }

    public void writeConfig(DetectedPusher dp, ConfigModel configModel) {
        SerialPort s = getSerialPort(dp);
        int usbHoldoff = 1020; // 0 - 1020
        try (OutputStream outputStream = s.getOutputStream()) {
            outputStream.write(("\r\nHoldoff" + Integer.toString(usbHoldoff / 4, 16)).getBytes());
            if (configModel != null) {

                outputStream.write("\r\nConfig".getBytes());
                BufferedReader reader = new BufferedReader(new StringReader(configModel.toString()));
                for (int i = 0; i < 1023; i++) {
                    int c = reader.read();
                    if (c == -1) {
                        outputStream.write(255);
                        break;
                    }
                    outputStream.write(c);
                }

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            s.closePort();
        }
    }

    public void enterReflash(DetectedPusher dp) {
        SerialPort s = SerialPort.getCommPort(dp.port);
        try (OutputStream outputStream = s.getOutputStream()) {
            outputStream.write("\r\nOrange".getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            s.closePort();
        }
    }

    public DetectedPusher detectPusher() {

        Pattern vrx = Pattern.compile("PixelPusher (.*) ready and waiting");
        String pusherPort = null;
        String pusherVersion = null;

        // List all the available serial ports:
        for (SerialPort pn : SerialPort.getCommPorts()) {
            try {
                System.out.println("Attempting to detect PixelPusher on port " + pn);
                OutputStream s = pn.getOutputStream();
                InputStream is = pn.getInputStream();
                s.write("\r\nReboot".getBytes());
                s.flush();
                Thread.sleep(2000);
                while (is.available() > 0) {
                    String r = new String(is.readAllBytes());
                    Matcher m = vrx.matcher(r);
                    if (m.find()) {
                        pusherVersion = m.group(1);
                        pusherPort = pn.getPortLocation();
                    }
                }
                s.close();
            } catch (Exception ex) {
                System.out.println("Couldn't open port " + pn);
            }
        }
        if (pusherPort != null) {
            System.out.println("PixelPusher found on " + pusherPort + ". Version: " + pusherVersion);
            return new DetectedPusher(pusherPort, pusherVersion);
        }

        System.out.println("No PixelPusher found. Unplug, wait 5 seconds, and plug back in and try again");
        return null;
    }

}
