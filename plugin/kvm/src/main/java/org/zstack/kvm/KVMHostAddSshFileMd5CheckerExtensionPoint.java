package org.zstack.kvm;

import org.zstack.core.ansible.SshFileMd5Checker;

public interface KVMHostAddSshFileMd5CheckerExtensionPoint {
    SshFileMd5Checker getSshFileMd5Checker(KVMHostVO kvmHostVO);
}
