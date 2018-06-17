/**
- * Copyright (c) 2014,2018 by the respective copyright holders.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.phidgets.handler;

import com.phidget22.*;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.*;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.phidgets.internal.PhidgetChannelRequest;
import org.openhab.binding.phidgets.internal.PhidgetsChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.openhab.binding.phidgets.PhidgetsBindingConstants.*;

/**
 * The {@link PhidgetsHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Gunnar Skjold - Initial contribution
 */
public class PhidgetsHandler extends BaseThingHandler {
    private static final Logger logger = LoggerFactory.getLogger(PhidgetsHandler.class);

    private int serialNumber;

    private PhidgetsChannelFactory phidgetsChannelFactory;

    private Map<ChannelUID, AttachListener> currentStateChanger;

    public PhidgetsHandler(Thing thing, PhidgetsChannelFactory phidgetsChannelFactory) {
        super(thing);
        String serialNumber = thing.getProperties().get(THING_CONFIG_SERIAL_NUMBER);
        if (serialNumber != null && !serialNumber.trim().isEmpty()) {
            this.serialNumber = Double.valueOf(serialNumber).intValue();
        } else {
            Number serialNumberNumber = (Number) thing.getConfiguration().get(THING_CONFIG_SERIAL_NUMBER);
            this.serialNumber = serialNumberNumber.intValue();
        }
        this.phidgetsChannelFactory = phidgetsChannelFactory;
        this.currentStateChanger = new HashMap<>();
    }

    private Phidget getPhidget(Channel channel) {
        Integer channelNumber;
        String channelNumberString = channel.getProperties().get(CHANNEL_PROPERTY_CHANNEL);
        if (channelNumberString != null && !channelNumberString.isEmpty()) {
            channelNumber = Double.valueOf(channelNumberString).intValue();
        } else {
            channelNumber = null;
        }
        String channelTypeId = getChannelTypeId(channel);
        PhidgetChannelRequest request = new PhidgetChannelRequest(serialNumber, channelTypeId, channelNumber);
        phidgetsChannelFactory.getPhidgetChannel(request);
        return request.getPhidget();
    }

    private void disposePhidget(Channel channel) {
        String channelNumberString = channel.getProperties().get(CHANNEL_PROPERTY_CHANNEL);
        int channelNumber = Integer.valueOf(channelNumberString);
        String channelTypeId = getChannelTypeId(channel);
        phidgetsChannelFactory.disposePhidgetChannel(serialNumber, channelTypeId, channelNumber);
    }

    private String getChannelTypeId(Channel channel) {
        String channelTypeId = channel.getChannelTypeUID().getId();
        if (channelTypeId.equals(CHANNEL_VINT_PORT)) {
            Number hubPortModeId = (Number) channel.getConfiguration().get(CHANNEL_CONFIG_PORT_MODE);
            HubPortMode hubPortMode = HubPortMode.getEnum(hubPortModeId.intValue());
            if (hubPortMode != null) {
                switch (hubPortMode) {
                    case DIGITAL_INPUT:
                        channelTypeId = CHANNEL_DIGITAL_INPUT_ID;
                        break;
                    case DIGITAL_OUTPUT:
                        channelTypeId = CHANNEL_DIGITAL_OUTPUT_ID;
                        break;
                    case VOLTAGE_INPUT:
                        channelTypeId = CHANNEL_VOLTAGE_INPUT_ID;
                        break;
                    case VOLTAGE_RATIO_INPUT:
                        channelTypeId = CHANNEL_VOLTAGE_RATIO_INPUT_ID;
                        break;
                }
            }
        } else if (channelTypeId.equals(CHANNEL_ANALOG_INPUT_ID)) {
            Number sensorType = (Number) channel.getConfiguration().get(CHANNEL_CONFIG_SENSOR_TYPE);
            VoltageSensorType voltageSensorType = VoltageSensorType.getEnum(sensorType.intValue());
            if (voltageSensorType != null) {
                channelTypeId = CHANNEL_VOLTAGE_INPUT_ID;
            } else {
                if (sensorType.intValue() == 1) {
                    sensorType = 0;
                }
                VoltageRatioSensorType voltageRatioSensorType = VoltageRatioSensorType.getEnum(sensorType.intValue());
                if (voltageRatioSensorType != null) {
                    channelTypeId = CHANNEL_VOLTAGE_RATIO_INPUT_ID;
                }
            }
        } else if (channelTypeId.equals(CHANNEL_RELAY_OUTPUT_ID)) {
            channelTypeId = CHANNEL_DIGITAL_OUTPUT_ID;
        }
        return channelTypeId;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        try {
            Phidget phidget = getPhidget(getThing().getChannel(channelUID.getId()));
            if (phidget == null) {
                logger.warn("[{}] Phidget was not found for channel {} to handle command {}", serialNumber,
                        channelUID.getId(), command.toFullString());
                return;
            }
            if (command instanceof OnOffType) {
                if (phidget instanceof DigitalOutput) {
                    DigitalOutput out = (DigitalOutput) phidget;
                    Boolean newState = null;
                    switch ((OnOffType) command) {
                        case ON:
                            newState = true;
                            break;
                        case OFF:
                            newState = false;
                            break;
                    }
                    if (newState != null) {
                        if (phidget.getAttached()) {
                            logger.debug("[{}] Setting state for {} to {}", serialNumber, channelUID.getId(), newState);
                            out.setState(newState);
                            updateState(channelUID, newState ? OnOffType.ON : OnOffType.OFF);
                        } else {
                            updateStatus(ThingStatus.OFFLINE);
                            out.open();
                            logger.debug(
                                    "[{}] Phidget was not attached, adding AttachListener to set state for {} to {} when attached",
                                    serialNumber, channelUID.getId(), newState);
                            boolean nextState = newState;
                            AttachListener currentStateChanger = this.currentStateChanger.get(channelUID);
                            if (currentStateChanger != null) {
                                phidget.removeAttachListener(currentStateChanger);
                            }
                            currentStateChanger = new AttachListener() {
                                @Override
                                public void onAttach(AttachEvent arg0) {
                                    logger.debug("[{}] Executing delayed command for {} setting state {}", serialNumber,
                                            channelUID.getId(), nextState);
                                    handleCommand(channelUID, command);
                                    phidget.removeAttachListener(this);
                                }
                            };
                            this.currentStateChanger.put(channelUID, currentStateChanger);
                            phidget.addAttachListener(currentStateChanger);
                        }
                    }
                }
            } else if (command instanceof RefreshType) {
                if (phidget.getAttached()) {
                    logger.debug("[{}] Refreshing channel {}", serialNumber, channelUID.getId());
                    if (phidget instanceof VoltageInput) {
                        updateState(channelUID, new DecimalType(((VoltageInput) phidget).getVoltage()));
                    } else if (phidget instanceof VoltageRatioInput) {
                        updateState(channelUID, new DecimalType(((VoltageRatioInput) phidget).getVoltageRatio()));
                    } else if (phidget instanceof DigitalInput) {
                        updateState(channelUID, ((DigitalInput) phidget).getState() ? OnOffType.ON : OnOffType.OFF);
                    } else if (phidget instanceof DigitalOutput) {
                        updateState(channelUID, ((DigitalOutput) phidget).getState() ? OnOffType.ON : OnOffType.OFF);
                    }
                } else {
                    phidget.open();
                    logger.debug("[{}] Channel {} was not attached, will refresh state when attached", serialNumber,
                            channelUID.getId());
                }
            }
        } catch (PhidgetException e) {
            logger.error("[{}] Problem when handling command {} for channel {}", command.toFullString(),
                    channelUID.getId(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
        }
    }

    @Override
    public void initialize() {
        logger.debug("[{}] Initialize", serialNumber);

        try {
            NetBase.enableServerDiscovery(ServerType.DEVICE_REMOTE);
        } catch (PhidgetException e) {
            logger.warn("Unable to enable server discovery for Phidget22 Server"
                    + ", is libavahi-client (debian) or avahi-devel (rhel) missing?", e);
        }

        try {
            NetBase.enableServerDiscovery(ServerType.WWWREMOTE);
        } catch (PhidgetException e) {
            logger.warn("Unable to enable server discovery for Phidget22 Web server"
                    + ", is libavahi-client (debian) or avahi-devel (rhel) missing?", e);
        }
        try {
            NetBase.enableServerDiscovery(ServerType.SBC);
        } catch (PhidgetException e) {
            logger.warn("Unable to enable server discovery for Phidget SBC"
                    + ", is libavahi-client (debian) or avahi-devel (rhel) missing?", e);
        }

        for (Channel channel : getThing().getChannels()) {
            logger.debug("[{}] Setting up phidget for channel {}", serialNumber, channel.getUID().getId());

            Phidget phidget = getPhidget(channel);
            if (phidget == null) {
                logger.debug("[{}] No phidget, ignoring {}", serialNumber, channel.getUID().getId());
                continue;
            }
            phidget.addAttachListener((event) -> {
                logger.debug("[{}] Attached for channel {}", serialNumber, channel.getUID().getId());
                updateStatus(ThingStatus.ONLINE);
            });
            phidget.addDetachListener((event) -> {
                logger.debug("[{}] Detached for channel {}", serialNumber, channel.getUID().getId());
                updateStatus(ThingStatus.OFFLINE);
            });

            if (phidget instanceof Hub) {
                logger.debug("[{}] Is a hub, ignoring {}", serialNumber, channel.getUID().getId());
                continue;
            }

            Number confSens = (Number) channel.getConfiguration().get(CHANNEL_CONFIG_SENSITIVITY);
            Double sensitivity = confSens == null ? CHANNEL_DEFAULT_SENSITIVITY : confSens.doubleValue();

            String channelTypeId = channel.getChannelTypeUID().getId();
            boolean hubPortDevice = channelTypeId.equals(CHANNEL_VINT_PORT);
            if (hubPortDevice) {
                String portNumberString = channel.getProperties().get(CHANNEL_PROPERTY_PORT);
                int portNumber = Integer.valueOf(portNumberString);
                try {
                    phidget.setIsHubPortDevice(hubPortDevice);
                    phidget.setHubPort(portNumber);
                    phidget.setChannel(0);
                } catch (PhidgetException e) {
                    logger.error("[{}] Unable to configure hub port {}", serialNumber, channel.getUID().getId(), e);
                }
            }

            if (phidget instanceof VoltageInput) {
                logger.debug("[{}] Channel {} is a voltage input", serialNumber, channel.getUID().getId());
                VoltageInput vin = (VoltageInput) phidget;
                vin.addAttachListener((event) -> {
                    try {
                        vin.setSensorValueChangeTrigger(sensitivity);

                        Number sensorTypeId = (Number) channel.getConfiguration().get(CHANNEL_CONFIG_SENSOR_TYPE);
                        VoltageSensorType sensorType = VoltageSensorType.getEnum(sensorTypeId.intValue());
                        if (sensorType != null) {
                            vin.setSensorType(sensorType);
                        }

                        Number powerSupplyId = (Number) channel.getConfiguration().get(CHANNEL_CONFIG_POWER_SUPPLY);
                        PowerSupply powerSupply = PowerSupply.getEnum(powerSupplyId.intValue());
                        if (powerSupply != null) {
                            vin.setPowerSupply(powerSupply);
                        }

                        Number voltageRangeId = (Number) channel.getConfiguration().get(CHANNEL_CONFIG_VOLTAGE_RANGE);
                        VoltageRange voltageRange = VoltageRange.getEnum(voltageRangeId.intValue());
                        if (voltageRange != null) {
                            vin.setVoltageRange(voltageRange);
                        }
                    } catch (PhidgetException e) {
                        logger.error("[{}] Unable to configure phidget channel {} properly", serialNumber,
                                channel.getUID().getId(), e);
                    }
                });
                vin.addSensorChangeListener((event) -> {
                    logger.debug("[{}] Sensor changed for {} to {}", serialNumber, channel.getUID().getId(),
                            event.getSensorValue());
                    updateState(channel.getUID(), new DecimalType(event.getSensorValue()));
                });
            } else if (phidget instanceof VoltageRatioInput) {
                logger.debug("[{}] Channel {} is a voltage ratio input", serialNumber, channel.getUID().getId());
                VoltageRatioInput vri = (VoltageRatioInput) phidget;
                vri.addAttachListener((event) -> {
                    try {
                        vri.setSensorValueChangeTrigger(sensitivity);

                        Number sensorTypeId = (Number) channel.getConfiguration().get(CHANNEL_CONFIG_SENSOR_TYPE);
                        VoltageRatioSensorType sensorType = VoltageRatioSensorType.getEnum(sensorTypeId.intValue());
                        if (sensorType != null) {
                            vri.setSensorType(sensorType);
                        }

                        Boolean bridgeEnable = (Boolean) channel.getConfiguration().get(CHANNEL_CONFIG_BRIDGE_ENABLE);
                        if (bridgeEnable != null && bridgeEnable) {
                            vri.setBridgeEnabled(bridgeEnable);
                        }

                        Number bridgeGainId = (Number) channel.getConfiguration().get(CHANNEL_CONFIG_BRIDGE_GAIN);
                        BridgeGain bridgeGain = BridgeGain.getEnum(bridgeGainId.intValue());
                        if (bridgeGain != null) {
                            vri.setBridgeGain(bridgeGain);
                        }
                    } catch (PhidgetException e) {
                        logger.error("[{}] Unable to configure phidget channel {} properly", serialNumber,
                                channel.getUID().getId(), e);
                    }
                });
                vri.addSensorChangeListener((event) -> {
                    logger.debug("[{}] Sensor changed for {} to {}", serialNumber, channel.getUID().getId(),
                            event.getSensorValue());
                    updateState(channel.getUID(), new DecimalType(event.getSensorValue()));
                });
            } else if (phidget instanceof DigitalInput) {
                logger.debug("[{}] Channel {} is a digital input", serialNumber, channel.getUID().getId());
                DigitalInput in = (DigitalInput) phidget;
                in.addAttachListener((event) -> {
                    try {
                        Number powerSupplyId = (Number) channel.getConfiguration().get(CHANNEL_CONFIG_POWER_SUPPLY);
                        PowerSupply powerSupply = PowerSupply.getEnum(powerSupplyId.intValue());
                        if (powerSupply != null) {
                            in.setPowerSupply(powerSupply);
                        }

                        Number inputModeId = (Number) channel.getConfiguration().get(CHANNEL_CONFIG_INPUT_MODE);
                        InputMode inputMode = InputMode.getEnum(inputModeId.intValue());
                        if (inputMode != null) {
                            in.setInputMode(inputMode);
                        }
                    } catch (PhidgetException e) {
                        logger.error("[{}] Unable to configure phidget channel {} properly", serialNumber,
                                channel.getUID().getId(), e);
                    }
                });
                in.addStateChangeListener((event) -> {
                    logger.debug("[{}] Digital input changed for {} to {}", serialNumber, channel.getUID().getId(),
                            event.getState());
                    updateState(channel.getUID(), event.getState() ? OnOffType.ON : OnOffType.OFF);
                });
            } else if (phidget instanceof DigitalOutput) {
                logger.debug("[{}] Channel {} is a digital output", serialNumber, channel.getUID().getId());
                DigitalOutput out = (DigitalOutput) phidget;
                out.addAttachListener((event) -> {
                    try {
                        Number dutyCycle = (Number) channel.getConfiguration().get(CHANNEL_CONFIG_DUTY_CYCLE);
                        if (dutyCycle != null) {
                            out.setDutyCycle(dutyCycle.doubleValue());
                        }

                        Number ledCurrentLimit = (Number) channel.getConfiguration()
                                .get(CHANNEL_CONFIG_LED_CURRENT_LIMIT);
                        if (ledCurrentLimit != null) {
                            out.setLEDCurrentLimit(ledCurrentLimit.doubleValue());
                        }

                        Number ledForwardVoltageId = (Number) channel.getConfiguration()
                                .get(CHANNEL_CONFIG_LED_FORWARD_VOLTAGE);
                        LEDForwardVoltage ledForwardVoltage = LEDForwardVoltage.getEnum(ledForwardVoltageId.intValue());
                        if (ledForwardVoltage != null) {
                            out.setLEDForwardVoltage(ledForwardVoltage);
                        }
                    } catch (PhidgetException e) {
                        logger.error("[{}] Unable to configure phidget channel {} properly", serialNumber,
                                channel.getUID().getId(), e);
                    }
                });
            }
            try {
                phidget.open();
            } catch (PhidgetException e) {
                logger.error("[{}] Unable to open phidget channel {}", serialNumber, channel.getUID().getId(), e);
            }
        }
        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void dispose() {
        logger.debug("[{}] Dispose", serialNumber);
        currentStateChanger.clear();
        for (Channel channel : getThing().getChannels()) {
            logger.debug("[{}] Disposing phidget for channel {}", serialNumber, channel.getUID().getId());
            disposePhidget(channel);
        }
    }
}
