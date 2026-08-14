package org.zstack.kvm;

import org.zstack.compute.host.VolumeMigrationTargetHostFilter;
import org.zstack.core.db.Q;
import org.zstack.header.host.HostVO;

import java.util.List;

import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 * Created by Bryant on 2017/9/7.
 */
public class UsernameKVMHostFilter implements VolumeMigrationTargetHostFilter {
    @Override
    public List<HostVO> filter(List<HostVO> candidates) {
        List<String> toRemoveHuuids = Q.New(HostVO.class).notEq(KVMHostVO_.username, "root").select(KVMHostVO_.uuid).listValues();
        if (!toRemoveHuuids.isEmpty()){
            candidates = transformAndRemoveNull(candidates, arg -> toRemoveHuuids.contains(arg.getUuid()) ? null : arg);
        }
        return candidates;
    }
}
