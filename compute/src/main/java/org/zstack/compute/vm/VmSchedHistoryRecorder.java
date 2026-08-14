package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.vm.*;
import org.zstack.identity.AccountManager;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.data.Pair;

import javax.persistence.Tuple;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmSchedHistoryRecorder {
    public static final String TYPE_HA = "VMHA";
    public static final String TYPE_HOST_MAINTENANCE = "HMT";

    private final String vmInstanceUuid;
    private final String schedType;
    private final String hostUuid;
    private final String zoneUuid;
    private String schedReason;
    private String failReason;
    private VmSchedHistoryVO vo;

    @Autowired
    private AccountManager accountManager;
    @Autowired
    private DatabaseFacade dbf;

    public static VmSchedHistoryRecorder ofHostMaintenance(String vmUuid) {
        return new VmSchedHistoryRecorder(TYPE_HOST_MAINTENANCE, vmUuid);
    }

    public static VmSchedHistoryRecorder ofHA(String vmUuid) {
        return new VmSchedHistoryRecorder(TYPE_HA, vmUuid);
    }

    public static VmSchedHistoryRecorder ofHA(VmInstanceInventory vmInv) {
        return new VmSchedHistoryRecorder(TYPE_HA, vmInv);
    }

    private VmSchedHistoryRecorder(String schedType, String vmUuid) {
        this.schedType = schedType;
        this.vmInstanceUuid = vmUuid;
        final Pair<String, String> p = new SQLBatchWithReturn<Pair<String, String>>() {
            @Override
            protected Pair<String, String> scripts() {
                Tuple t = q(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmInstanceUuid)
                        .select(VmInstanceVO_.hostUuid, VmInstanceVO_.lastHostUuid, VmInstanceVO_.zoneUuid)
                        .findTuple();
                String huuid = t.get(0, String.class);
                if (huuid == null) {
                    huuid = t.get(1, String.class);
                }
                return new Pair<>(huuid, t.get(2, String.class));
            }
        }.execute();
        this.hostUuid = p.first();
        this.zoneUuid = p.second();
    }

    private VmSchedHistoryRecorder(String schedType, VmInstanceInventory vmInv) {
        this.schedType = schedType;
        this.vmInstanceUuid = vmInv.getUuid();
        this.hostUuid = vmInv.getHostUuid() == null ? vmInv.getLastHostUuid() : vmInv.getHostUuid();
        this.zoneUuid = vmInv.getZoneUuid();
    }

    public VmSchedHistoryRecorder begin() {
        String acntUuid = accountManager.getOwnerAccountUuidOfResource(vmInstanceUuid);
        vo = new VmSchedHistoryVO();
        vo.setVmInstanceUuid(vmInstanceUuid);
        vo.setSchedType(schedType);
        vo.setAccountUuid(acntUuid);
        vo.setZoneUuid(zoneUuid);
        vo.setLastHostUuid(hostUuid);
        vo.setSchedReason(schedReason);
        vo = dbf.persistAndRefresh(vo);
        return this;
    }

    public VmSchedHistoryRecorder withSchedReason(String schedReason) {
        this.schedReason = schedReason;
        return this;
    }

    public VmSchedHistoryRecorder withFailReason(String failReason) {
        this.failReason = failReason;
        return this;
    }

    public int end(String currentHostUuid) {
        DebugUtils.Assert(this.vo != null, "Recorder not started.");

        return SQL.New(VmSchedHistoryVO.class)
                .eq(VmSchedHistoryVO_.id, vo.getId())
                .eq(VmSchedHistoryVO_.vmInstanceUuid, vmInstanceUuid)
                .set(VmSchedHistoryVO_.destHostUuid, currentHostUuid)
                .set(VmSchedHistoryVO_.success, currentHostUuid != null)
                .set(VmSchedHistoryVO_.failReason, failReason)
                .update();
    }
}
