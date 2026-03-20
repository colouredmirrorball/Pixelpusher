package com.heroicrobot.dropbit.devices.pixelpusher;

import java.util.*;

import com.heroicrobot.dropbit.registry.DeviceRegistry;
import java.util.concurrent.Semaphore;

public class SceneThread extends Thread implements Observer {

  private static int PUSHER_PORT = 9897;

  private final Map<String, PixelPusher> pusherMap;
  private final Map<String, CardThread> cardThreadMap;
  private int extraDelay = 0;
  private boolean autoThrottle=false;

  private boolean drain;

  private boolean running;
  private final Semaphore listSemaphore;

  public SceneThread() {
    this.pusherMap = new HashMap<>();
    this.cardThreadMap = new HashMap<>();
    this.drain = false;
    this.running = false;
    this.listSemaphore = new Semaphore(1);
  }
  
  public void setAutoThrottle(boolean autothrottle) {
    autoThrottle = autothrottle;
    for (PixelPusher pusher : pusherMap.values()) {
      pusher.setAutoThrottle(autothrottle);
    }
  }

  public long getTotalBandwidth() {
    long totalBandwidth=0;
    for (CardThread thread : cardThreadMap.values()) {
      totalBandwidth += thread.getBandwidthEstimate();
    }
    return totalBandwidth;
  }
  
  public void setExtraDelay(int msec) {
    extraDelay = msec;
    for (CardThread thread : cardThreadMap.values()) {
      thread.setExtraDelay(msec);
    }
  }
  
  public void removePusherThread(PixelPusher card) {
    for (CardThread th : cardThreadMap.values()) {
        if (th.controls(card)) {
          th.shutDown();
          th.cancel();
       }
     }
    cardThreadMap.remove(card.getMacAddress());
   }
  
  @Override
  public void update(Observable observable, Object update) {
    if (!drain) {
      listSemaphore.acquireUninterruptibly();
      
      Map<String, PixelPusher> incomingPusherMap = ((DeviceRegistry) observable)
          .getPusherMap(); // all observed pushers
      Map<String, PixelPusher> newPusherMap = new HashMap<>(
          incomingPusherMap);
      Map<String, PixelPusher> deadPusherMap = new HashMap<>(
          pusherMap);
      Map<String, PixelPusher> currentPusherMap = new HashMap<>(
          pusherMap);
      
      try {
        for (String key : incomingPusherMap.keySet()) {
          if (currentPusherMap.containsKey(key)) { // if we already know about it
            newPusherMap.remove(key); // remove it from the new pusher map (is
                                      // old)
          }
        }
      } catch (ConcurrentModificationException cme) {
        System.err.println("Concurrent modification exception attempting to generate the new pusher map.");
        listSemaphore.release();
        return;
      }
      try {
        for (String key : currentPusherMap.keySet()) {
          if (incomingPusherMap.containsKey(key)) { // if it's in the new pusher map
            deadPusherMap.remove(key); // it can't be dead
          }
        }
      } catch (ConcurrentModificationException cme) {
        System.err.println("Concurrent modification exception attempting to generate the dead pusher map.");
        listSemaphore.release();
        return;
      }

      for (String key : newPusherMap.keySet()) {
        CardThread newCardThread = new CardThread(newPusherMap.get(key),
            PUSHER_PORT, ((DeviceRegistry) observable));
        if (running) {
          newCardThread.start();
          newCardThread.setExtraDelay(extraDelay);
          newPusherMap.get(key).setAutoThrottle(autoThrottle);
        }
        pusherMap.put(key, newPusherMap.get(key));
        cardThreadMap.put(key, newCardThread);
      }
      listSemaphore.release();
    }
  }

  public boolean isRunning() {
    return this.running;
  }

  @Override
  public void run() {
    this.running = true;
    this.drain = false;
    for (CardThread thread : cardThreadMap.values()) {
      thread.start();
    }
  }

  public void cancel() {
    this.drain = true;
    for (String key : cardThreadMap.keySet()) {
      cardThreadMap.get(key).cancel();
      cardThreadMap.remove(key);
    }
  }
}
