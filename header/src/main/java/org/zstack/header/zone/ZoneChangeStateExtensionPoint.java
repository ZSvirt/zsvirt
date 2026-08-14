package org.zstack.header.zone;


public interface ZoneChangeStateExtensionPoint {
    void preChangeZoneState(ZoneInventory inventory, ZoneStateEvent event, ZoneState nextState) throws ZoneException;

    void beforeChangeZoneState(ZoneInventory inventory, ZoneStateEvent event, ZoneState nextState);

    void afterChangeZoneState(ZoneInventory inventory, ZoneStateEvent event, ZoneState previousState);
}
