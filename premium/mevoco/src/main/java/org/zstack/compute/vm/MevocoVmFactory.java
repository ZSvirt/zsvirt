package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.Component;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageDeletionMsg;
import org.zstack.header.image.ImageDeletionPolicyManager;
import org.zstack.header.vm.*;
import org.zstack.mevoco.MevocoConstants;
import org.zstack.mevoco.MevocoGlobalConfig;

/**
 * Created by mingjian.deng on 16/10/29.
 */
public class MevocoVmFactory implements VmInstanceFactory, Component {
    private static final VmInstanceType type = new VmInstanceType(MevocoConstants.MEVOCO_VM_TYPE);

    @Override
    public VmInstance getVmInstance(VmInstanceVO vo) {
        return new MevocoVmInstanceBase(vo);
    }

    @Autowired
    private DatabaseFacade dbf;

    @Autowired
    private AnsibleFacade asf;

    @Autowired
    private CloudBus bus;

    @Override
    public VmInstanceType getType() {
        return type;
    }

    @Override
    @Transactional
    public VmInstanceVO createVmInstance(VmInstanceVO vo, CreateVmInstanceMsg msg) {
        vo.setType(type.toString());
        dbf.getEntityManager().persist(vo);
        return vo;
    }

    private void deploySaltState() {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }
    }

    @Override
    public boolean start() {
        deploySaltState();
        return false;
    }

    @Override
    public boolean stop() {
        return false;
    }
}
