package com.heroicrobot.dropbit.config;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigModelTest {

    @Test
    void generateEmptyConfig() {
        ConfigModel config = new ConfigModel();
        assertTrue(StringUtils.isBlank(config.toString()),
                "Config file without any modified parameters should be empty");
    }

    @Test
    void generateMyConfig() {
        ConfigModel config = new ConfigModel();
        Arrays.fill(config.stripType, ConfigModel.StripType.WS2811);
        Arrays.fill(config.order, ConfigModel.ColorOrder.RGB);
        config.turbo_mode = true;
        config.turbo_speed = 24;
        config.pixelsperstrip = 240 * 4 / 3;
        System.out.println(config);
    }


}