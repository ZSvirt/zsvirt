package org.zstack.header.zone;

public interface ZoneDeleteExtensionPoint {
    void preDeleteZone(ZoneInventory inventory) throws ZoneException;

    void beforeDeleteZone(ZoneInventory inventory);

    void afterDeleteZone(ZoneInventory inventory);
}
