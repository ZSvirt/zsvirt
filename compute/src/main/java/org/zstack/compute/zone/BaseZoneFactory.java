package org.zstack.compute.zone;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.zone.*;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class BaseZoneFactory implements ZoneFactory {
    static final ZoneType type = new ZoneType("zstack");
    
    @Autowired
    private DatabaseFacade dbf;
    
    @Override
    public ZoneType getType() {
        return type;
    }

    @Override
    public ZoneVO createZone(ZoneVO vo, APICreateZoneMsg msg) {
        vo.setType(type.toString());
        return vo;
    }

    @Override
    public Zone getZone(ZoneVO vo) {
        return new ZoneBase(vo);
    }

}
