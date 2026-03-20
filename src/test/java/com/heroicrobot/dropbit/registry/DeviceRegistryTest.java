package com.heroicrobot.dropbit.registry;

import com.heroicrobot.dropbit.devices.pixelpusher.Pixel;
import com.heroicrobot.dropbit.devices.pixelpusher.Strip;
import org.junit.jupiter.api.Test;

import java.util.Observable;
import java.util.Observer;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class DeviceRegistryTest {
    private static final Logger LOGGER = Logger
            .getLogger(DeviceRegistryTest.class.getName());

    private Random random = new Random();

    int[][] colors = { { 127, 0, 0 }, { 0, 127, 0 }, { 0, 0, 127 } };

    public Pixel generateRandomPixel() {
        int[] color = colors[random.nextInt(colors.length)];
        return new Pixel((byte) color[0], (byte) color[1], (byte) color[2]);
    }

    @Test
    void testDiscovery() {
        LOGGER.setLevel(Level.FINEST);
        LOGGER.info("Beginning discovery");
        DeviceRegistry registry = new DeviceRegistry();
        class TestObserver implements Observer {

            public boolean hasStrips = false;

            @Override
            public void update(Observable registry, Object updatedDevice) {
                LOGGER.info("Registry changed!");
                if (updatedDevice != null) {
                    LOGGER.info("Device change: " + updatedDevice);
                }
                this.hasStrips = true;
            }

        }
        TestObserver testObserver = new TestObserver();
        registry.addObserver(testObserver);
        while (true) {
            Thread.yield();
            if (testObserver.hasStrips) {
                registry.startPushing();
                for (Strip strip : registry.getStrips()) {
                    for (int i = 0; i < strip.getLength(); i++)
                        strip.setPixel(generateRandomPixel(), i);
                }
            }
        }
    }


}