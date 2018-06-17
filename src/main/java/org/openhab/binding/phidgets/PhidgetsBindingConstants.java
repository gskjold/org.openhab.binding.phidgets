/**
 * Copyright (c) 2014,2018 by the respective copyright holders.
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
package org.openhab.binding.phidgets;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * The {@link PhidgetsBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Gunnar Skjold - Initial contribution
 */
@NonNullByDefault
public class PhidgetsBindingConstants {
    private static final Logger logger = LoggerFactory.getLogger(PhidgetsBindingConstants.class);

    static {
        try {
            Class.forName("com.phidget22.PhidgetBase");
        } catch (ClassNotFoundException e) {
            logger.error("Unable to load phidgets library", e);
        }
    }

    private static final String BINDING_ID = "phidgets";

    public static final String BINDING_CONFIG_LOG_FILE = "logfile";

    public static final ThingTypeUID THING_PHIDGET = new ThingTypeUID(BINDING_ID, "phidget");
    public static final ThingTypeUID THING_PHIDGET_1010_1013_1018_1019 = new ThingTypeUID(BINDING_ID, "phidget_1010_1013_1018_1019");
    public static final ThingTypeUID THING_PHIDGET_1011 = new ThingTypeUID(BINDING_ID, "phidget_1011");
    public static final ThingTypeUID THING_PHIDGET_1012 = new ThingTypeUID(BINDING_ID, "phidget_1012");
    public static final ThingTypeUID THING_PHIDGET_1014 = new ThingTypeUID(BINDING_ID, "phidget_1014");
    public static final ThingTypeUID THING_PHIDGET_1017 = new ThingTypeUID(BINDING_ID, "phidget_1017");
    public static final ThingTypeUID THING_PHIDGET_1046 = new ThingTypeUID(BINDING_ID, "phidget_1046");
    public static final ThingTypeUID THING_PHIDGET_HUB0000 = new ThingTypeUID(BINDING_ID, "phidget_hub0000");

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = new HashSet<>(Arrays.asList(
            THING_PHIDGET, 
            THING_PHIDGET_1010_1013_1018_1019,
            THING_PHIDGET_1011,
            THING_PHIDGET_1012,
            THING_PHIDGET_1014,
            THING_PHIDGET_1017,
            THING_PHIDGET_1046
    ));

    public static final String THING_CONFIG_SERIAL_NUMBER = "serialNumber";

    public static final String CHANNEL_VOLTAGE_INPUT_ID = "voltage-input";
    public static final String CHANNEL_VOLTAGE_RATIO_INPUT_ID = "voltage-ratio-input";
    public static final String CHANNEL_DIGITAL_INPUT_ID = "digital-input";
    public static final String CHANNEL_DIGITAL_OUTPUT_ID = "digital-output";
    public static final String CHANNEL_ANALOG_INPUT_ID = "analog-input";
    public static final String CHANNEL_RELAY_OUTPUT_ID = "relay-output";
    public static final String CHANNEL_VINT_PORT = "vint-port";

    public static final ChannelTypeUID CHANNEL_VOLTAGE_INPUT = new ChannelTypeUID(BINDING_ID, CHANNEL_VOLTAGE_INPUT_ID);
    public static final ChannelTypeUID CHANNEL_VOLTAGE_RATIO_INPUT = new ChannelTypeUID(BINDING_ID, CHANNEL_VOLTAGE_RATIO_INPUT_ID);
    public static final ChannelTypeUID CHANNEL_DIGITAL_INPUT = new ChannelTypeUID(BINDING_ID, CHANNEL_DIGITAL_INPUT_ID);
    public static final ChannelTypeUID CHANNEL_DIGITAL_OUTPUT = new ChannelTypeUID(BINDING_ID, CHANNEL_DIGITAL_OUTPUT_ID);
    public static final ChannelTypeUID CHANNEL_ANALOG_INPUT = new ChannelTypeUID(BINDING_ID, CHANNEL_ANALOG_INPUT_ID);
    public static final ChannelTypeUID CHANNEL_RELAY_OUTPUT = new ChannelTypeUID(BINDING_ID, CHANNEL_RELAY_OUTPUT_ID);

    public static final String CHANNEL_PROPERTY_CHANNEL = "channel";
    public static final String CHANNEL_PROPERTY_PORT = "port";
    public static final String CHANNEL_CONFIG_POWER_SUPPLY = "power-supply";
    public static final String CHANNEL_CONFIG_INPUT_MODE = "input-mode";
    public static final String CHANNEL_CONFIG_DUTY_CYCLE = "duty-cycle";
    public static final String CHANNEL_CONFIG_LED_CURRENT_LIMIT = "led-current-limit";
    public static final String CHANNEL_CONFIG_LED_FORWARD_VOLTAGE = "led-forward-voltage";
    public static final String CHANNEL_CONFIG_SENSOR_TYPE = "sensor-type";
    public static final String CHANNEL_CONFIG_VOLTAGE_RANGE = "voltage-range";
    public static final String CHANNEL_CONFIG_BRIDGE_ENABLE = "bridge-enable";
    public static final String CHANNEL_CONFIG_BRIDGE_GAIN = "bridge-gain";
    public static final String CHANNEL_CONFIG_SENSITIVITY = "sensitivity";
    public static final String CHANNEL_CONFIG_PORT_MODE = "port-mode";
    public static final double CHANNEL_DEFAULT_SENSITIVITY = 0.01d;
}
