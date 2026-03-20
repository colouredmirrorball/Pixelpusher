package com.heroicrobot.dropbit.config;

public class ConfigModel {

    int group;
    int controller;
    boolean turbo_mode;
    int turbo_speed;
    int strips_attached;
    int pixelsperstrip;
    boolean blank_on_idle;
    int[] stripType;
    boolean[] swap;
    int[] order;
    int[] blank;
    boolean dhcp;
    int retries;
    int artnet_universe;
    int artnet_channel;
    String ether;
    String netmask;
    String gateway;

    String filename;

    private final String[] stripTypes = {"lpd8806", "lpd6803", "ws2801", "ws2811", "tlc59711", "p9813", "sd600a"};
    private final String[] orders = {"rgb", "rbg", "grb", "gbr", "brg", "bgr"};

    ConfigModel() {
        stripType = new int[8];
        swap = new boolean[8];

        blank = new int[8];
        stripType = new int[8];
        order = new int[8];

        order[0] = order[1] = order[2] = order[3] = order[4] = order[5] = order[6] = order[7] = 1;

        dhcp = true;
        retries = 5;

        ether = "192.168.1.137";
        netmask = "255.255.255.0";
        gateway = "192.168.1.1";

        blank_on_idle = false;
        pixelsperstrip = 240;
        strips_attached = 8;
    }

    public String toString() {
        StringBuilder s = new StringBuilder();
        if (group != 0)
            s.append("group=").append(group).append("\n");
        if (controller != 0)
            s.append("controller=").append(controller).append("\n");
        if (artnet_universe != 0)
            s.append("artnet_universe=").append(artnet_universe).append("\n");
        if (artnet_channel != 0)
            s.append("artnet_channel=").append(artnet_channel).append("\n");


        if (turbo_mode)
            s.append("turbo_mode=").append(turbo_speed).append("\n");
        if (strips_attached != 8)
            s.append("stripsattached=").append(strips_attached).append("\n");
        if (pixelsperstrip != 240)
            s.append("pixels=").append(pixelsperstrip).append("\n");
        for (int i = 0; i < 8; i++)
            if (stripType[i] != 0)
                s.append("strip").append(i + 1).append("=").append(stripTypes[stripType[i]])
                        .append("\n");

        for (int i = 0; i < 8; i++)
            if (order[i] != 1)
                s.append("strip").append(i + 1).append("=").append(orders[order[i]]).append("\n");

        for (int i = 0; i < 8; i++)
            if (blank[i] != 0)
                s.append("start").append(i + 1).append("=").append(Integer.toString(blank[i], 16))
                        .append("\n");

        if (blank_on_idle)
            s.append("blank_strips_on_idle=1\n");

        if (dhcp) {
            if (retries != 5)
                s.append("dhcp_timeout=").append(retries).append("\n");
        } else {
            s.append("ether=").append(ether).append("\n");
            s.append("netmask=").append(netmask).append("\n");
            s.append("gateway=").append(gateway).append("\n");
        }


        // this is ugly, but...
        boolean isSomeOutputSwapped = false;
        for (int i = 0; i < 8; i++) {
            if (swap[i]) {
                isSomeOutputSwapped = true;
                break;
            }
        }
        if (isSomeOutputSwapped) {
            s.append("swap=");
            for (int i = 1; i < 8; i++) {
                if (swap[i])
                    s.append(i + 1);
            }
            s.append("\n");
        }

        return s.toString();
    }


}
