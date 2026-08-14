package org.zstack.mevoco;

import org.zstack.header.host.HostInventory;
import org.zstack.kvm.KVMHostDeployArguments;
import org.zstack.tag.SystemTagCreator;

import java.util.Collections;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * @author Xingwei Yu
 * @date 2024/7/4 17:14
 */
public class CgroupDeviceAclConfigItem implements QemuConfigItemOperator {
    @Override
    public String getStatus(HostInventory host) {
        return MevocoSystemTags.KVM_HOST_CGROUP_DEVICE_ACL_STATUS.getTokenByResourceUuid(
                host.getUuid(), MevocoSystemTags.KVM_HOST_CGROUP_DEVICE_ACL_STATUS_TOKEN
        );
    }

    @Override
    public boolean isEnabled() {
        return MevocoGlobalConfig.ENABLE_CGROUP_DEVICE_ACL.value(Integer.class) > 0;
    }

    @Override
    public void applyConfig(KVMHostDeployArguments args) {
        args.setEnableCgroupDeviceAcl(String.valueOf(isEnabled()));
    }

    @Override
    public void createOrUpdateTag(String uuid) {
        String status = MevocoSystemTags.KVM_HOST_CGROUP_DEVICE_ACL_STATUS.getTokenByResourceUuid(uuid, MevocoSystemTags.KVM_HOST_CGROUP_DEVICE_ACL_STATUS_TOKEN);
        boolean isEnabled = isEnabled();

        if (status == null) {
            SystemTagCreator creator = MevocoSystemTags.KVM_HOST_CGROUP_DEVICE_ACL_STATUS.newSystemTagCreator(uuid);
            creator.setTagByTokens(Collections.singletonMap(MevocoSystemTags.KVM_HOST_CGROUP_DEVICE_ACL_STATUS_TOKEN, String.valueOf(isEnabled)));
            creator.inherent = false;
            creator.recreate = true;
            creator.create();
        } else if (Boolean.parseBoolean(status) != isEnabled) {
            MevocoSystemTags.KVM_HOST_CGROUP_DEVICE_ACL_STATUS.update(uuid,
                    MevocoSystemTags.KVM_HOST_CGROUP_DEVICE_ACL_STATUS.instantiateTag(
                            map(e(MevocoSystemTags.KVM_HOST_CGROUP_DEVICE_ACL_STATUS_TOKEN, String.valueOf(isEnabled)))
                    )
            );
        }
    }

    @Override
    public void rollbackTag(String uuid) {
        boolean rollbackEnableCgroupDeviceAcl = !isEnabled();
        MevocoSystemTags.KVM_HOST_CGROUP_DEVICE_ACL_STATUS.update(uuid,
                MevocoSystemTags.KVM_HOST_CGROUP_DEVICE_ACL_STATUS.instantiateTag(
                        map(e(MevocoSystemTags.KVM_HOST_CGROUP_DEVICE_ACL_STATUS_TOKEN, String.valueOf(rollbackEnableCgroupDeviceAcl)))
                )
        );
    }
}
