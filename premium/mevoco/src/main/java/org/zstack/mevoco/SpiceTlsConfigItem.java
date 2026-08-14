package org.zstack.mevoco;

import org.zstack.header.host.HostInventory;
import org.zstack.kvm.KVMHostDeployArguments;
import org.zstack.tag.SystemTagCreator;

import java.util.Collections;

import static org.zstack.utils.CollectionDSL.*;

/**
 * @author Xingwei Yu
 * @date 2024/7/4 17:14
 */
public class SpiceTlsConfigItem implements QemuConfigItemOperator {
    @Override
    public String getStatus(HostInventory host) {
        return MevocoSystemTags.KVM_HOST_SPICE_TLS_STATUS.getTokenByResourceUuid(
                host.getUuid(), MevocoSystemTags.KVM_HOST_SPICE_TLS_STATUS_TOKEN
        );
    }

    @Override
    public boolean isEnabled() {
        return MevocoGlobalConfig.ENABLE_SPICE_CHANNEL_SUPPORT_TLS.value(Integer.class) > 0;
    }

    @Override
    public void applyConfig(KVMHostDeployArguments args) {
        args.setEnableSpiceTls(String.valueOf(isEnabled()));
    }

    @Override
    public void createOrUpdateTag(String uuid) {
        String status = MevocoSystemTags.KVM_HOST_SPICE_TLS_STATUS.getTokenByResourceUuid(uuid, MevocoSystemTags.KVM_HOST_SPICE_TLS_STATUS_TOKEN);
        boolean isEnabled = isEnabled();

        if (status == null) {
            SystemTagCreator creator = MevocoSystemTags.KVM_HOST_SPICE_TLS_STATUS.newSystemTagCreator(uuid);
            creator.setTagByTokens(Collections.singletonMap(MevocoSystemTags.KVM_HOST_SPICE_TLS_STATUS_TOKEN, String.valueOf(isEnabled)));
            creator.inherent = false;
            creator.recreate = true;
            creator.create();
        } else if (Boolean.parseBoolean(status) != isEnabled) {
            MevocoSystemTags.KVM_HOST_SPICE_TLS_STATUS.update(uuid,
                    MevocoSystemTags.KVM_HOST_SPICE_TLS_STATUS.instantiateTag(
                            map(e(MevocoSystemTags.KVM_HOST_SPICE_TLS_STATUS_TOKEN, String.valueOf(isEnabled)))
                    )
            );
        }
    }

    @Override
    public void rollbackTag(String uuid) {
        boolean rollbackEnableSpiceTls = !isEnabled();
        MevocoSystemTags.KVM_HOST_SPICE_TLS_STATUS.update(uuid,
                MevocoSystemTags.KVM_HOST_SPICE_TLS_STATUS.instantiateTag(
                        map(e(MevocoSystemTags.KVM_HOST_SPICE_TLS_STATUS_TOKEN, String.valueOf(rollbackEnableSpiceTls)))
                )
        );
    }
}
