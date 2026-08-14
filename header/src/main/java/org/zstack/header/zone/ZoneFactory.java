package org.zstack.header.zone;

public interface ZoneFactory {
    ZoneType getType();

    ZoneVO createZone(ZoneVO vo, APICreateZoneMsg msg);

    Zone getZone(ZoneVO vo);
}
