package org.openhab.binding.phidgets.internal;

import com.phidget22.*;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.phidgets.PhidgetsBindingConstants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.openhab.binding.phidgets.PhidgetsBindingConstants.*;

@Component(immediate = true, service = DiscoveryService.class)
public class PhidgetsDiscoveryService extends AbstractDiscoveryService {
    private final Logger logger = LoggerFactory.getLogger(PhidgetsDiscoveryService.class);

    private Manager manager;

    public PhidgetsDiscoveryService() {
        super(SUPPORTED_THING_TYPES_UIDS, 30);
    }

    @Override
    @Activate
    protected void activate(@Nullable Map<@NonNull String, @Nullable Object> configProperties) {
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

        try {
            String logfile = (String) configProperties.get(BINDING_CONFIG_LOG_FILE);
            LogBase.enable(LogLevel.WARNING, logfile);
        } catch (PhidgetException e) {
            logger.warn("Could not enable phidgets logging");
        }
        super.activate(configProperties);
    }

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    protected void startScan() {
        try {
            final Set<Integer> visited = new HashSet<>();
            manager = new Manager();
            manager.addAttachListener(new ManagerAttachListener() {
                @Override
                public void onAttach(ManagerAttachEvent event) {
                    try {
                        Phidget phidget = event.getChannel();
                        int serial = phidget.getDeviceSerialNumber();
                        if (!visited.contains(serial)) {
                            visited.add(serial);

                            ThingTypeUID type;
                            switch (phidget.getDeviceID()) {
                                case PN_DICTIONARY:
                                    return;
                                case PN_1010_1013_1018_1019:
                                    type = PhidgetsBindingConstants.THING_PHIDGET_1010_1013_1018_1019;
                                    break;
                                case PN_1011:
                                    type = PhidgetsBindingConstants.THING_PHIDGET_1011;
                                    break;
                                case PN_1012:
                                    type = PhidgetsBindingConstants.THING_PHIDGET_1012;
                                    break;
                                case PN_1014:
                                    type = PhidgetsBindingConstants.THING_PHIDGET_1014;
                                    break;
                                case PN_1017:
                                    type = PhidgetsBindingConstants.THING_PHIDGET_1017;
                                    break;
                                case PN_1046:
                                    type = PhidgetsBindingConstants.THING_PHIDGET_1046;
                                    break;
                                case PN_HUB0000:
                                    type = PhidgetsBindingConstants.THING_PHIDGET_HUB0000;
                                    break;
                                default:
                                    type = PhidgetsBindingConstants.THING_PHIDGET;
                            }
                            String label = String.format("%s (serial: %d)", phidget.getDeviceName(), serial);
                            thingDiscovered(
                                    DiscoveryResultBuilder.create(new ThingUID(type, String.format("%d", serial)))
                                            .withLabel(label).withProperty(THING_CONFIG_SERIAL_NUMBER, serial).build());
                        }
                    } catch (PhidgetException e) {
                        logger.error("Error while handling discovered phidget", e);
                    }
                }
            });
            manager.open();
        } catch (PhidgetException e) {
            stopScan();
            throw new RuntimeException(e);
        }
    }

    @Override
    protected synchronized void stopScan() {
        super.stopScan();
        if (manager != null) {
            try {
                manager.close();
            } catch (PhidgetException e) {
            } finally {
                manager = null;
            }
        }
    }
}
