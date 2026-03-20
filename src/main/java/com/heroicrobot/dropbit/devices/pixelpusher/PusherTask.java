package com.heroicrobot.dropbit.devices.pixelpusher;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import com.heroicrobot.dropbit.registry.DeviceRegistry;

public class PusherTask extends TimerTask {

    public static final int PUSHER_PORT = 9897;
    byte[] packet;
    int packetLength;
    private Map<String, PixelPusher> pusherMap;
    private Semaphore semaphore;
    private DatagramSocket udp;

    public PusherTask() {
        this.pusherMap = new HashMap<>();
        this.packet = new byte[1460];
        this.packetLength = 0;
        this.semaphore = new Semaphore(1);
        try {
            this.udp = new DatagramSocket();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    public void update(Observable observable, Object update) {
        this.semaphore.acquireUninterruptibly();
        this.pusherMap = ((DeviceRegistry) observable).getPusherMap();
        this.semaphore.release();

    }

    @Override
    public void run() {
        this.semaphore.acquireUninterruptibly();
        if (pusherMap.isEmpty())
            return;
        for (PixelPusher pusher : pusherMap.values()) {
            int stripPerPacket = pusher.getMaxStripsPerPacket();
            List<Strip> remainingStrips = new ArrayList<>(pusher.getStrips());
            while (!remainingStrips.isEmpty()) {
                for (int i = 0; i < stripPerPacket; i++) {
                    if (remainingStrips.isEmpty()) {
                        break;
                    }
                    Strip strip = remainingStrips.remove(0);
                    byte[] stripPacket = strip.serialize();
                    this.packet[packetLength++] = (byte) strip.getStripNumber();
                    for (int j = 0; j < stripPacket.length; j++) {
                        this.packet[packetLength + j] = stripPacket[j];
                    }
                    this.packetLength += stripPacket.length;
                }
                try {
                    DatagramPacket datagramPacket = new DatagramPacket(this.packet, this.packetLength);
                    udp.bind(new InetSocketAddress(pusher.getIp(), PUSHER_PORT));
                    udp.send(datagramPacket);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                this.packetLength = 0;
            }
        }
        this.semaphore.release();

    }

}
