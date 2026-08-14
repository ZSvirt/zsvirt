package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.vm.*;

public class UserVmFactory implements VmInstanceFactory {
    private static final VmInstanceType type = new VmInstanceType(VmInstanceConstant.USER_VM_TYPE);

    static {
        type.setSriovSupported(true);
    }

    @Autowired
    private DatabaseFacade dbf;
    
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

    @Override
    public VmInstance getVmInstance(VmInstanceVO vo) {
        return new VmInstanceBase(vo);
    }

}
