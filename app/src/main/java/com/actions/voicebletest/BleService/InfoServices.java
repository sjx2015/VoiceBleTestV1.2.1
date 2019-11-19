package com.actions.voicebletest.BleService;

import java.util.HashMap;

public class InfoServices {

    private static HashMap<String, InfoService> SERVICES = new HashMap<String, InfoService>();

    static {
        final GapService gapService = new GapService();
        final ActionsBeaconService actionsBeaconService = new ActionsBeaconService();
        final ActionsTransmissionService actionsTransmissionService = new ActionsTransmissionService();

        SERVICES.put(gapService.getUUID(), gapService);
        SERVICES.put(actionsBeaconService.getUUID(), actionsBeaconService);
        SERVICES.put(actionsTransmissionService.getUUID(), actionsTransmissionService);
    }

    public static InfoService getService(String uuid) {
        return SERVICES.get(uuid);
    }

    public static boolean isActionsService(String uuid) {
        return uuid.equals(ActionsBeaconService.UUID_SERVICE) || uuid.equals(ActionsTransmissionService.UUID_SERVICE);
    }
} 