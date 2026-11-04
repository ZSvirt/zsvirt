package org.zstack.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.*;

public class UserVmInstanceOfferingFactory implements InstanceOfferingFactory {
    static final InstanceOfferingType type = new InstanceOfferingType(ConfigurationConstant.USER_VM_INSTANCE_OFFERING_TYPE);

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public InstanceOfferingType getInstanceOfferingType() {
        return type;
    }

    @Override
    @Transactional
    public InstanceOfferingInventory createInstanceOffering(InstanceOfferingVO vo, APICreateInstanceOfferingMsg msg) {
        dbf.getEntityManager().persist(vo);
        dbf.getEntityManager().flush();
        dbf.getEntityManager().refresh(vo);
        return InstanceOfferingInventory.valueOf(vo);
    }

    @Override
    public InstanceOffering getInstanceOffering(InstanceOfferingVO vo) {
        return new InstanceOfferingBase(vo);
    }
}
