package com.heroicrobot.dropbit.discovery;

public enum DeviceType {
  ETHERDREAM, LUMIABRIDGE, PIXELPUSHER;

  public static DeviceType fromInteger(int x) {
      return switch (x) {
          case 0 -> ETHERDREAM;
          case 1 -> LUMIABRIDGE;
          case 2 -> PIXELPUSHER;
          default -> null;
      };
  }
}
