package org.zstack.simulator.storage.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.simulator.storage.primary.APIAddSimulatorPrimaryStorageMsg;
import org.zstack.header.simulator.storage.primary.SimulatorPrimaryStorageConstant;
import org.zstack.header.storage.primary.*;
import org.zstack.storage.primary.PrimaryStorageCapacityUpdater;
import org.zstack.utils.path.PathUtil;

public class SimulatorPrimaryStorageFactory implements PrimaryStorageFactory {
    private static final PrimaryStorageType type = new PrimaryStorageType(SimulatorPrimaryStorageConstant.SIMULATOR_PRIMARY_STORAGE_TYPE, CoreGlobalProperty.EXPOSE_SIMULATOR_TYPE);

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public PrimaryStorageType getPrimaryStorageType() {
        return type;
    }

    @Override
    public PrimaryStorageInventory createPrimaryStorage(PrimaryStorageVO vo, APIAddPrimaryStorageMsg msg) {
        String mountPath = PathUtil.join("/primarystoragesimulator", vo.getUuid());
        vo.setMountPath(mountPath);
        vo = dbf.persistAndRefresh(vo);

        APIAddSimulatorPrimaryStorageMsg smsg = (APIAddSimulatorPrimaryStorageMsg) msg;
        PrimaryStorageCapacityVO cvo = new PrimaryStorageCapacityVO();
        cvo.setUuid(vo.getUuid());
        cvo.setAvailableCapacity(smsg.getAvailableCapacity());
        cvo.setTotalCapacity(smsg.getTotalCapacity());
        cvo.setAvailablePhysicalCapacity(smsg.getAvailablePhysicalCapacity());
        cvo.setTotalPhysicalCapacity(smsg.getTotalPhysicalCapacity());
        dbf.persist(cvo);
        return PrimaryStorageInventory.valueOf(vo);
    }

    @Override
    public PrimaryStorageInventory getInventory(String uuid) {
        PrimaryStorageVO vo = dbf.findByUuid(uuid, PrimaryStorageVO.class);
        return PrimaryStorageInventory.valueOf(vo);
    }

    @Override
    public void validateStorageProtocol(String protocol) {

    }

    @Override
    public PrimaryStorage getPrimaryStorage(PrimaryStorageVO vo) {
        return new SimulatorPrimaryStorage(vo);
    }

}
