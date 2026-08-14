package org.zstack.header.vm;

import org.zstack.header.errorcode.ErrorCode;

import java.util.List;

public interface SshKeyPairAssociateExtensionPoint {
    ErrorCode associateSshKeyPair(String vmUuid, List<String> keyPairUuids);

    List<String> fetchAssociatedSshKeyPairs(String vmUuid);

    void cloneSshKeyPairsToVm(String originVmUuid, String destVmUuid);
}
