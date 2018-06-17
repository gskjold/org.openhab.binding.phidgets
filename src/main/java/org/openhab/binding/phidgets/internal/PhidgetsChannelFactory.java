package org.openhab.binding.phidgets.internal;

import com.phidget22.*;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.openhab.binding.phidgets.PhidgetsBindingConstants.*;

public class PhidgetsChannelFactory {
    private static final Logger logger = LoggerFactory.getLogger(PhidgetsChannelFactory.class);

    private final ScheduledExecutorService backgroundScheduler = Executors.newSingleThreadScheduledExecutor();

    private static final Map<String, Phidget> phidgets = new HashMap<>();

    @Nullable
    private static Phidget getFromMap(int serialNumber, String channelTypeId, int channelIdx) {
        logger.debug("[{}] Getting from map {} {}", serialNumber, channelTypeId, channelIdx);
        return phidgets.get(String.format("%d_%s_%d", serialNumber, channelTypeId, channelIdx));
    }

    private static void addToMap(int serialNumber, String channelTypeId, int channelIdx, Phidget phidget) {
        phidgets.put(String.format("%d_%s_%d", serialNumber, channelTypeId, channelIdx), phidget);
    }

    private static void removeFromMap(int serialNumber, String channelTypeId, int channelIdx) {
        phidgets.remove(String.format("%d_%s_%d", serialNumber, channelTypeId, channelIdx));
    }

    public void getPhidgetChannel(PhidgetChannelRequest request) {
        backgroundScheduler.schedule(() -> {
            logger.debug("[{}] Processing request for phidget {} {}", request.getSerialNumber(), request.getChannelTypeId(), request.getChannel());
            Phidget phidget = getFromMap(request.getSerialNumber(), request.getChannelTypeId(), request.getChannel());
            if (phidget == null) {
                logger.debug("[{}] Creating phidget for request {} {}", request.getSerialNumber(), request.getChannelTypeId(), request.getChannel());
                try {
                    switch (request.getChannelTypeId()) {
                        case CHANNEL_VOLTAGE_INPUT_ID:
                            logger.debug("[{}] Phidget is voltage input channel {}", request.getSerialNumber(), request.getChannel());
                            VoltageInput vin = new VoltageInput();
                            vin.setDeviceSerialNumber(request.getSerialNumber());
                            if (request.getChannel() != null) {
                                vin.setChannel(request.getChannel());
                            }
                            phidget = vin;
                            break;
                        case CHANNEL_VOLTAGE_RATIO_INPUT_ID:
                            logger.debug("[{}] Phidget is voltage ratio input channel {}", request.getSerialNumber(), request.getChannel());
                            VoltageRatioInput vri = new VoltageRatioInput();
                            vri.setDeviceSerialNumber(request.getSerialNumber());
                            if (request.getChannel() != null) {
                                vri.setChannel(request.getChannel());
                            }
                            phidget = vri;
                            break;
                        case CHANNEL_DIGITAL_INPUT_ID:
                            logger.debug("[{}] Phidget is digital input channel {}", request.getSerialNumber(), request.getChannel());
                            DigitalInput in = new DigitalInput();
                            in.setDeviceSerialNumber(request.getSerialNumber());
                            if (request.getChannel() != null) {
                                in.setChannel(request.getChannel());
                            }
                            phidget = in;
                            break;
                        case CHANNEL_DIGITAL_OUTPUT_ID:
                            logger.debug("[{}] Phidget is digital output channel {}", request.getSerialNumber(), request.getChannel());
                            DigitalOutput out = new DigitalOutput();
                            out.setDeviceSerialNumber(request.getSerialNumber());
                            if (request.getChannel() != null) {
                                out.setChannel(request.getChannel());
                            }
                            phidget = out;
                            break;
                    }
                    if (phidget != null) {
                        addToMap(request.getSerialNumber(), request.getChannelTypeId(), request.getChannel(), phidget);
                    } else {
                        logger.debug("[{}] No phidget for channel {} {}", request.getSerialNumber(), request.getChannelTypeId(), request.getChannel());
                    }
                } catch (PhidgetException e) {
                    logger.error("[{}] Unable to create phidget with type {} for channel {}", request.getSerialNumber(),
                            request.getChannelTypeId(), request.getChannel(), e);
                }
            } else {
                logger.debug("[{}] Existing phidget found {} {}", request.getSerialNumber(), request.getChannelTypeId(), request.getChannel());
            }
            request.setPhidget(phidget);
        }, 0, TimeUnit.SECONDS);
    }

    public void disposePhidgetChannel(int serialNumber, String channelTypeId, int channelNumber) {
        Phidget phidget = getFromMap(serialNumber, channelTypeId, channelNumber);
        if (phidget != null) {
            try {
                phidget.close();
                removeFromMap(serialNumber, channelTypeId, channelNumber);
            } catch (PhidgetException e) {
                logger.warn("Could not close phidget", e);
            }
        }
    }

}
