package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.DeadlockAutoRestart;
import org.zstack.core.db.UpdateQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceSpec.CdRomSpec;
import org.zstack.header.vm.cdrom.VmCdRomVO;
import org.zstack.header.vm.cdrom.VmCdRomVO_;
import org.zstack.identity.Account;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

/**
 * Create by lining at 2018/12/26
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmAllocateCdRomFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(VmAllocateCdRomFlow.class);
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected ErrorFacade errf;

    @Override
    public String name() {
        return "create-cd-roms";
    }

    @Override
    public void run(final FlowTrigger trigger, final Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        List<CdRomSpec> cdRomSpecs = spec.getCdRomSpecs();

        if (cdRomSpecs.isEmpty()) {
            trigger.next();
            return;
        }

        List<Integer> deviceIds = cdRomSpecs.stream().map(CdRomSpec::getDeviceId).distinct().collect(Collectors.toList());
        if (deviceIds.size() < cdRomSpecs.size()) {
            trigger.fail(operr("vm[uuid:%s] cdRom deviceId repetition",spec.getVmInventory().getUuid()));
            return;
        }

        List<VmCdRomVO> cdRomVOS = new ArrayList<>();
        for (CdRomSpec cdRomSpec : cdRomSpecs) {
            VmCdRomVO vmCdRomVO = new VmCdRomVO();
            String cdRomUuid = cdRomSpec.getUuid() != null ? cdRomSpec.getUuid() : Platform.getUuid();
            cdRomSpec.setUuid(cdRomUuid);
            String acntUuid = Account.getAccountUuidOfResource(spec.getVmInventory().getUuid());
            vmCdRomVO.setVmInstanceUuid(spec.getVmInventory().getUuid());
            vmCdRomVO.setUuid(cdRomUuid);
            vmCdRomVO.setDeviceId(cdRomSpec.getDeviceId());
            vmCdRomVO.setName(String.format("vm-%s-cdRom", spec.getVmInventory().getUuid()));
            vmCdRomVO.setAccountUuid(acntUuid);
            vmCdRomVO.setIsoUuid(cdRomSpec.getImageUuid());
            vmCdRomVO.setOccupant(vmCdRomVO.getIsoUuid() == null ? null : VmInstanceConstant.VM_CDROM_OCCUPANT_ISO);
            cdRomVOS.add(vmCdRomVO);
        }

        persistCdRomVOS(cdRomVOS);
        new IsoOperator().syncVmIsoSystemTag(spec.getVmInventory().getUuid());
        trigger.next();
    }

    @DeadlockAutoRestart
    private void persistCdRomVOS(List<VmCdRomVO> cdRomVOS) {
        dbf.persistCollection(cdRomVOS);
    }

    @Override
    public void rollback(final FlowRollback chain, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        List<CdRomSpec> cdRomSpecs = spec.getCdRomSpecs();

        if (!cdRomSpecs.isEmpty()) {
            UpdateQuery.New(VmCdRomVO.class)
                    .eq(VmCdRomVO_.vmInstanceUuid, spec.getVmInventory().getUuid())
                    .hardDelete();
        }

        chain.rollback();
    }
}
