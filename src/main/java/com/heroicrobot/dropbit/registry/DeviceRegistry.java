package com.heroicrobot.dropbit.registry;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import com.heroicrobot.dropbit.devices.pixelpusher.Pixel;
import com.heroicrobot.dropbit.devices.pixelpusher.PixelPusher;
import com.heroicrobot.dropbit.devices.pixelpusher.PusherGroup;
import com.heroicrobot.dropbit.devices.pixelpusher.SceneThread;
import com.heroicrobot.dropbit.devices.pixelpusher.Strip;
import com.heroicrobot.dropbit.discovery.DeviceHeader;
import com.heroicrobot.dropbit.discovery.DeviceType;

public class DeviceRegistry extends Observable {

    private static final Logger LOGGER = Logger.getLogger(DeviceRegistry.class
            .getName());
    public static final int DISCOVERY_PORT = 7331;
    public static final int MAX_DISCONNECT_SECONDS = 10;
    public static final long EXPIRY_TIMER_MSEC = 1000L;
    private static long totalPower = 0;
    private static long totalPowerLimit = -1;
    private static double powerScale = 1.0;
    private static boolean autoThrottle = false;
    private final DatagramSocket udp;
    private final Map<String, PixelPusher> pusherMap;
    private final Map<String, LocalDateTime> pusherLastSeenMap;

    private final SceneThread sceneThread;

    private final TreeMap<Integer, PusherGroup> groupMap;

    private final TreeSet<PixelPusher> sortedPushers;

    public DeviceRegistry() {
        try {
            udp = new DatagramSocket(DISCOVERY_PORT);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        pusherMap = new TreeMap<>();
        groupMap = new TreeMap<>();
        sortedPushers = new TreeSet<>(new DefaultPusherComparator());
        pusherLastSeenMap = new HashMap<>();
        new Thread(new UdpListener()).start();
        Timer expiryTimer = new Timer();
        expiryTimer.scheduleAtFixedRate(new DeviceExpiryTask(this), 0L,
                EXPIRY_TIMER_MSEC);
        this.sceneThread = new SceneThread();
        this.addObserver(this.sceneThread);
    }

    public Map<String, PixelPusher> getPusherMap() {
        return pusherMap;
    }

    public void setExtraDelay(int msec) {
        sceneThread.setExtraDelay(msec);
    }

    public void setAutoThrottle(boolean autothrottle) {
        autoThrottle = autothrottle;
        sceneThread.setAutoThrottle(autothrottle);
    }

    public long getTotalBandwidth() {
        return sceneThread.getTotalBandwidth();
    }

    public long getTotalPower() {
        return totalPower;
    }

    public long getTotalPowerLimit() {
        return totalPowerLimit;
    }

    public void setTotalPowerLimit(long powerLimit) {
        totalPowerLimit = powerLimit;
    }

    public double getPowerScale() {
        return powerScale;
    }

    public List<Strip> getStrips() {
        List<Strip> strips = new ArrayList<>();
        for (PixelPusher p : this.sortedPushers) {
            strips.addAll(p.getStrips());
        }
        return strips;
    }

    public List<Strip> getStrips(int groupNumber) {
        if (this.groupMap.containsKey(groupNumber)) {
            return this.groupMap.get(groupNumber).getStrips();
        } else {
            return new ArrayList<>();
        }
    }

    public void expireDevice(String macAddr) {
        LOGGER.info("Device gone: " + macAddr);
        PixelPusher pusher = pusherMap.remove(macAddr);
        pusherLastSeenMap.remove(macAddr);
        sortedPushers.remove(pusher);
        this.groupMap.get(pusher.getGroupOrdinal()).removePusher(pusher);
        if (sceneThread.isRunning())
            sceneThread.removePusherThread(pusher);
        this.setChanged();
        this.notifyObservers();
    }

    public void setStripValues(String macAddress, int stripNumber, Pixel[] pixels) {
        this.pusherMap.get(macAddress).setStripValues(stripNumber, pixels);

    }

    public void startPushing() {
        if (!sceneThread.isRunning()) {
            sceneThread.start();
        }
    }

    public void stopPushing() {
        if (sceneThread.isRunning()) {
            sceneThread.cancel();
        }
    }

    public void receive(byte[] data) {
        // This is for the UDP callback, this should not be called directly
        DeviceHeader header = new DeviceHeader(data);
        String macAddr = header.GetMacAddressString();
        if (header.DeviceType != DeviceType.PIXELPUSHER) {
            LOGGER.fine("Ignoring non-PixelPusher discovery packet from "
                    + header.toString());
            return;
        }
        PixelPusher device = new PixelPusher(header.PacketRemainder, header);
        // Set the timestamp for the last time this device checked in
        pusherLastSeenMap.put(macAddr, LocalDateTime.now());
        if (!pusherMap.containsKey(macAddr)) {
            // We haven't seen this device before
            addNewPusher(macAddr, device);
        } else {
            if (!pusherMap.get(macAddr).equals(device)) { // we already saw it
                updatePusher(macAddr, device);
            } else {
                // The device is identical, nothing has changed
                LOGGER.fine("Device still present: " + macAddr);
                // if we dropped more than occasional packets, slow down a little
                if (device.getDeltaSequence() > 3)
                    pusherMap.get(macAddr).increaseExtraDelay(5);
                if (device.getDeltaSequence() < 1)
                    pusherMap.get(macAddr).decreaseExtraDelay(1);
                System.out.println(device);
            }
        }

        // update the power limit variables
        if (totalPowerLimit > 0) {
            totalPower = 0;
            for (PixelPusher pusher : sortedPushers) {
                totalPower += pusher.getPowerTotal();
            }
            if (totalPower > totalPowerLimit) {
                powerScale = totalPowerLimit / totalPower;
            } else {
                powerScale = 1.0;
            }
        }
    }

    private void updatePusher(String macAddr, PixelPusher device) {
        // We already knew about this device at the given MAC, but its details
        // have changed
        LOGGER.info("Device changed: " + macAddr);
        pusherMap.get(macAddr).copyHeader(device);

        this.setChanged();
        this.notifyObservers(device);
    }

    private void addNewPusher(String macAddr, PixelPusher pusher) {
        LOGGER.info("New device: " + macAddr + " has group ordinal " + pusher.getGroupOrdinal());
        pusherMap.put(macAddr, pusher);
        LOGGER.info("Adding to sorted list");
        sortedPushers.add(pusher);
        LOGGER.info("Adding to group map");
        if (groupMap.get(pusher.getGroupOrdinal()) != null) {
            LOGGER.info("Adding pusher to group " + pusher.getGroupOrdinal());
            groupMap.get(pusher.getGroupOrdinal()).addPusher(pusher);
        } else {
            // we need to create a PusherGroup since it doesn't exist yet.
            PusherGroup pg = new PusherGroup();
            LOGGER.info("Creating group and adding pusher to group " + pusher.getGroupOrdinal());
            pg.addPusher(pusher);
            groupMap.put(pusher.getGroupOrdinal(), pg);
        }
        pusher.setAutoThrottle(autoThrottle);
        LOGGER.info("Notifying observers");
        this.setChanged();
        this.notifyObservers(pusher);
    }

    private static class DefaultPusherComparator implements Comparator<PixelPusher> {

        @Override
        public int compare(PixelPusher arg0, PixelPusher arg1) {
            int group0 = arg0.getGroupOrdinal();
            int group1 = arg1.getGroupOrdinal();
            if (group0 != group1) {
                if (group0 < group1)
                    return -1;
                return 1;
            }

            int ord0 = arg0.getControllerOrdinal();
            int ord1 = arg1.getControllerOrdinal();
            if (ord0 != ord1) {
                if (ord0 < ord1)
                    return -1;
                return 1;
            }

            return arg0.getMacAddress().compareTo(arg1.getMacAddress());
        }

    }

    class DeviceExpiryTask extends TimerTask {

        private final DeviceRegistry registry;

        DeviceExpiryTask(DeviceRegistry registry) {
            this.registry = registry;
        }

        @Override
        public void run() {
            LOGGER.fine("Expiry and preening task running");

            // A little sleight of hand here.  We can't call registry.expireDevice()
            // directly from inside the loop, for the loop is an implicit iterator and
            // registry.expireDevice modifies the pusherMap.
            // Instead we create a list of the MAC addresses to kill, then loop over
            // them outside the iterator.  - jls
            List<String> toKill = new ArrayList<>();
            for (String deviceMac : pusherMap.keySet()) {
                long lastSeenSeconds = Duration.between(
                        pusherLastSeenMap.get(deviceMac), LocalDateTime.now()).getSeconds();
                if (lastSeenSeconds > MAX_DISCONNECT_SECONDS) {
                    toKill.add(deviceMac);
                }
            }
            for (String doomedIndividual : toKill) {
                registry.expireDevice(doomedIndividual);
            }
        }
    }

    public class UdpListener implements Runnable {
        @Override
        public void run() {

            byte[] buffer = new byte[1500];
            while (!udp.isClosed()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                try {
                    udp.receive(packet);
                    byte[] data = new byte[packet.getLength()];
                    System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
                    DeviceRegistry.this.receive(data);
                } catch (IOException e) {
                    if (!udp.isClosed()) {
                        throw new RuntimeException("UDP receive failed", e);
                    }
                    break;
                }

            }
        }
    }

}
