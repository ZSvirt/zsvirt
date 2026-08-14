package org.zstack.xdragon;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.zstack.compute.host.HostPriorityCaculator;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.*;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeFormat;
import org.zstack.kvm.KVMHostContext;
import org.zstack.kvm.KVMHostFactory;

import static org.zstack.core.Platform.operr;

public class XDragonHostFactory implements HypervisorFactory, HostPriorityCaculator {
    @Autowired
    private DatabaseFacade dbf;

    @Autowired
    @Qualifier("KVMHostFactory")
    private KVMHostFactory factory;

    static private final HypervisorType hypervisorType = new HypervisorType(XDragonConstant.HYPERVISOR_TYPE);
    static private final VolumeFormat QCOW2_FORMAT = new VolumeFormat(VolumeConstant.VOLUME_FORMAT_QCOW2, KVMHostFactory.hypervisorType, hypervisorType);
    static private final VolumeFormat RAW_FORMAT = new VolumeFormat(VolumeConstant.VOLUME_FORMAT_RAW, KVMHostFactory.hypervisorType, hypervisorType);

    static {
        RAW_FORMAT.newFormatInputOutputMapping(hypervisorType, QCOW2_FORMAT.toString());
    }

    public HostVO createHost(HostVO vo, AddHostMessage msg) {
        if (!(msg instanceof AddXDragonHostMessage)) {
            throw new OperationFailureException(operr("cluster[uuid:%s] hypervisorType is not %s", msg.getClusterUuid(), XDragonConstant.HYPERVISOR_TYPE));
        }

        AddXDragonHostMessage amsg = (AddXDragonHostMessage) msg;
        XDragonHostVO xvo = new XDragonHostVO(vo);
        xvo.setUsername(amsg.getUsername());
        xvo.setPassword(amsg.getPassword());
        xvo.setPort(amsg.getSshPort());
        xvo.setCpuNum(amsg.getCpuNum());
        xvo.setCpuSockets(amsg.getCpuSockets());
        xvo.setTotalPhysicalMemory(amsg.getTotalPhysicalMemory());
        xvo = dbf.persistAndRefresh(xvo);
        return xvo;
    }

    public Host getHost(HostVO vo) {
        XDragonHostVO xvo = dbf.findByUuid(vo.getUuid(), XDragonHostVO.class);
        KVMHostContext context = factory.getHostContext(vo.getUuid());
        if (context == null) {
            context = factory.createHostContext(xvo);
        }
        return new XDragonHost(xvo, context);
    }

    public HypervisorType getHypervisorType() {
        return hypervisorType;
    }

    public HostInventory getHostInventory(HostVO vo) {
        XDragonHostVO vvo = vo instanceof XDragonHostVO ? (XDragonHostVO) vo : dbf.findByUuid(vo.getUuid(), XDragonHostVO.class);
        return XDragonHostInventory.valueOf(vvo);
    }

    public HostInventory getHostInventory(String uuid) {
        XDragonHostVO vo = dbf.findByUuid(uuid, XDragonHostVO.class);
        return vo == null ? null : XDragonHostInventory.valueOf(vo);
    }

    @Override
    public boolean supportGetHostOs() {
        return true;
    }

    @Override
    public HostOperationSystem getHostOS(String uuid) {
        XDragonHostVO vo = dbf.findByUuid(uuid, XDragonHostVO.class);
        return HostOperationSystem.of(vo.getOsDistribution(), vo.getOsRelease(), vo.getOsVersion());
    }

    @Override
    public int getHostConnectPriority(String hostUuid) {
        if (dbf.isExist(hostUuid, XDragonHostVO.class)) {
            return 10;
        }

        return 0;
    }
}
