package org.zstack.header.vm;

import org.zstack.header.errorcode.ErrorCode;

public interface VmResourceInstantiationChain {
    void fail(ErrorCode err);

    void runNext(VmInstanceSpec spec);
}
