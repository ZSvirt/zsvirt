package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.vm.VmEncryptionOperator;

public class VmEncryptionOperatorImpl implements VmEncryptionOperator {
    @Autowired
    private VmSensitiveTagEncryptor encryptor;

    @Override
    public void mirrorEncryptionState(String srcVmUuid, String dstVmUuid) {
        encryptor.mirrorVmEncryptionFromSource(srcVmUuid, dstVmUuid);
    }
}
