package com.heroicrobot.dropbit.config;

public class ConfigModel {

    public int group;
    public int controller;
    public boolean turbo_mode; // 1 - 24
    public int turbo_speed;
    public int strips_attached;
    public int pixelsperstrip;
    public boolean blank_on_idle;
    public StripType[] stripType;
    public boolean[] swap;
    public ColorOrder[] order;
    public int[] blank;
    public boolean dhcp;
    public int retries;
    public int artnet_universe;
    public int artnet_channel;
    public String ether;
    public String netmask;
    public String gateway;
    ConfigModel() {
        stripType = new StripType[8];
        swap = new boolean[8];

        blank = new int[8];
        order = new ColorOrder[8];

        order[0] = order[1] = order[2] = order[3] = order[4] = order[5] = order[6] = order[7] = ColorOrder.RGB;

        dhcp = true;
        retries = 5;

        ether = "192.168.1.137";
        netmask = "255.255.255.0";
        gateway = "192.168.1.1";

        blank_on_idle = false;
        pixelsperstrip = 240;
        strips_attached = 8;
    }

    @Override
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
            if (stripType[i] != StripType.LPD8806 && stripType[i] != null)
                s.append("strip").append(i + 1).append("=").append(stripType[i].getValue())
                        .append("\n");

        for (int i = 0; i < 8; i++)
            if (order[i] != ColorOrder.RGB && order[i] != null)
                s.append("strip").append(i + 1).append("=").append(order[i].getValue()).append("\n");

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

    public enum StripType {
        LPD8806("lpd8806"),
        LPD6803("lpd6803"),
        WS2801("ws2801"),
        WS2811("ws2811"),
        TLC59711("tlc59711"),
        P9813("p9813"),
        SD600A("sd600a");

        private final String value;

        StripType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum ColorOrder {
        RGB("rgb"),
        RBG("rbg"),
        GRB("grb"),
        GBR("gbr"),
        BRG("brg"),
        BGR("bgr");

        private final String value;

        ColorOrder(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }


}
