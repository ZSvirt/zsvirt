package org.zstack.header.vm;

public interface VmInstanceFactory {
    VmInstanceType getType();

    VmInstanceVO createVmInstance(VmInstanceVO vo, CreateVmInstanceMsg msg);

    VmInstance getVmInstance(VmInstanceVO vo);
}
