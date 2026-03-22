package com.heroicrobot.dropbit.config.serial;

public class DetectedPusher {
    public String port;
    public String ver;

    DetectedPusher(String port, String ver) {
        this.port = port;
        this.ver = ver;
    }
}
