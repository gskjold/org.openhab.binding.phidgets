package org.openhab.binding.phidgets.internal;

import com.phidget22.Phidget;

public class PhidgetChannelRequest {
    private final int serialNumber;
    private final String channelTypeId;
    private final Integer channel;

    private final Object o = new Object();
    private Phidget phidget;

    public PhidgetChannelRequest(int serialNumber, String channelTypeId, Integer channel) {
        this.serialNumber = serialNumber;
        this.channelTypeId = channelTypeId;
        this.channel = channel;
    }

    public int getSerialNumber() {
        return serialNumber;
    }

    public String getChannelTypeId() {
        return channelTypeId;
    }

    public Integer getChannel() {
        return channel;
    }

    public void setPhidget(Phidget phidget) {
        synchronized (o) {
            this.phidget = phidget;
            o.notifyAll();
        }
    }

    public Phidget getPhidget() {
        if (phidget != null) {
            return phidget;
        }
        synchronized (o) {
            try {
                o.wait(4000);
                return phidget;
            } catch (InterruptedException e) {
                return null;
            }
        }
    }
}
