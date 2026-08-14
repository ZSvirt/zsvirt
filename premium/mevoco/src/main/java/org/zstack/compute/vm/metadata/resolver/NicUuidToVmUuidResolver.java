package org.zstack.compute.vm.metadata.resolver;

import org.zstack.core.db.Q;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.header.vm.metadata.VmUuidFromApiResolver;

/**
 * nicUuid → VmNicVO.vmInstanceUuid.
 */
public class NicUuidToVmUuidResolver implements VmUuidFromApiResolver {

    @Override
    public String resolveVmUuid(String fieldValue) {
        if (fieldValue == null) {
            return null;
        }
        return Q.New(VmNicVO.class).eq(VmNicVO_.uuid, fieldValue).select(VmNicVO_.vmInstanceUuid).findValue();
    }
}
