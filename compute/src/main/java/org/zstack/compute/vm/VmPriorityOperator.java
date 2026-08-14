package org.zstack.compute.vm;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.managementnode.PrepareDbInitialValueExtensionPoint;
import org.zstack.header.vm.*;
import org.zstack.tag.SystemTagCreator;

import java.util.List;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * @ Author : yh.w
 * @ Date   : Created in 14:11 2019/9/18
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmPriorityOperator implements PrepareDbInitialValueExtensionPoint {
    @Autowired
    private DatabaseFacade dbf;

    public static List<VmInstanceState> allowEffectImmediatelyStates = Lists.newArrayList();

    static {
        allowEffectImmediatelyStates.add(VmInstanceState.Running);
        allowEffectImmediatelyStates.add(VmInstanceState.Paused);
        allowEffectImmediatelyStates.add(VmInstanceState.Pausing);
        allowEffectImmediatelyStates.add(VmInstanceState.Resuming);
    }

    @Override
    public void prepareDbInitialValue() {
        for (VmPriorityLevel vmPriorityLevel : VmPriorityLevel.values()) {
            if (!Q.New(VmPriorityConfigVO.class).eq(VmPriorityConfigVO_.level, vmPriorityLevel).isExists()) {
                VmPriorityConfigVO vo = new VmPriorityConfigVO();
                vo.setUuid(Platform.getUuid());
                vo.setLevel(vmPriorityLevel);
                vo.setOomScoreAdj(vmPriorityLevel.getOomScoreAdj());
                vo.setCpuShares(vmPriorityLevel.getCpuShares());
                vo.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
                dbf.persist(vo);
            }
        }
    }

    public static class PriorityStruct {
        private Integer cpuShares;

        private Integer oomScoreAdj;

        public Integer getCpuShares() {
            return cpuShares;
        }

        public void setCpuShares(Integer cpuShares) {
            this.cpuShares = cpuShares;
        }

        public Integer getOomScoreAdj() {
            return oomScoreAdj;
        }

        public void setOomScoreAdj(Integer oomScoreAdj) {
            this.oomScoreAdj = oomScoreAdj;
        }
    }

    public VmPriorityLevel getVmPriority(String vmUuid) {
        String priority = VmSystemTags.VM_PRIORITY.getTokenByResourceUuid(vmUuid, VmSystemTags.VM_PRIORITY_TOKEN);
        if (StringUtils.isEmpty(priority)) {
            setVmPriority(vmUuid, VmPriorityLevel.Normal);
            return VmPriorityLevel.Normal;
        }

        return VmPriorityLevel.valueOf(priority);
    }

    public void setVmPriority(String vmUuid, VmPriorityLevel level) {
        SystemTagCreator creator = VmSystemTags.VM_PRIORITY.newSystemTagCreator(vmUuid);
        creator.setTagByTokens(map(
                e(VmSystemTags.VM_PRIORITY_TOKEN, level)
        ));
        creator.inherent = false;
        creator.recreate = true;
        creator.create();
    }

    public void batchSetVmPriority(List<String> vmUuids, VmPriorityLevel level) {
        for (String vmUuid : vmUuids) {
            setVmPriority(vmUuid, level);
        }
    }

    public VmPriorityConfigVO updatePriorityConfig(String uuid, PriorityStruct struct) {
        VmPriorityConfigVO vo = dbf.findByUuid(uuid, VmPriorityConfigVO.class);
        boolean update = false;
        if (struct.getCpuShares() != null && struct.getCpuShares() != vo.getCpuShares()) {
            vo.setCpuShares(struct.getCpuShares());
            update = true;
        }

        if (struct.getOomScoreAdj() != null && struct.getOomScoreAdj() != vo.getOomScoreAdj()) {
            vo.setOomScoreAdj(struct.getOomScoreAdj());
            update = true;
        }

        if (update) {
            return dbf.updateAndRefresh(vo);
        }
        return null;
    }

    public boolean needEffectImmediately(VmInstanceState state) {
        return allowEffectImmediatelyStates.contains(state);
    }
}
