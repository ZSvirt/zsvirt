package org.zstack.appliancevm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.vm.VmInstanceManager;
import org.zstack.compute.vm.VmNicManager;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowException;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.*;
import org.zstack.header.vm.*;
import org.zstack.network.l3.L3NetworkManager;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.NetworkUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 11:38 PM
 * To change this template use File | Settings | File Templates.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ApplianceVmAllocateNicFlow implements Flow {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private L3NetworkManager l3nm;
    @Autowired
    private VmNicManager nicManager;
    @Autowired
    protected VmInstanceManager vmMgr;

    private UsedIpInventory acquireIp(String l3NetworkUuid, String mac, Integer version, String staticIp, String stratgey, boolean allowDuplicatedAddress) {
        AllocateIpMsg msg = new AllocateIpMsg();
        msg.setL3NetworkUuid(l3NetworkUuid);
        if (staticIp != null) {
            msg.setRequiredIp(staticIp);
        }
        msg.setIpVersion(version);
        msg.setDuplicatedIpAllowed(allowDuplicatedAddress);
        if (version == IPv6Constants.IPv6) {
            l3nm.updateIpAllocationMsg(msg, mac);
        }
        bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, l3NetworkUuid);
        msg.setAllocateStrategy(stratgey);
        MessageReply reply = bus.call(msg);
        if (!reply.isSuccess()) {
            throw new FlowException(reply.getError());
        }

        AllocateIpReply areply = (AllocateIpReply) reply;
        return areply.getIpInventory();
    }

    private VmNicInventory makeNicInventory(VmInstanceSpec vmSpec, ApplianceVmNicSpec nicSpec, int[] deviceId) {
        VmNicInventory inv = new VmNicInventory();
        inv.setUuid(Platform.getUuid());
        inv.setL3NetworkUuid(nicSpec.getL3NetworkUuid());
        inv.setVmInstanceUuid(vmSpec.getVmInventory().getUuid());
        inv.setDeviceId(deviceId[0]);
        inv.setMetaData(nicSpec.getMetaData());
        inv.setInternalName(VmNicVO.generateNicInternalName(vmSpec.getVmInventory().getInternalId(), inv.getDeviceId()));
        inv.setMac(NetworkUtils.generateMacWithDeviceId((short) inv.getDeviceId()));
        inv.setHypervisorType(vmSpec.getVmInventory().getHypervisorType());
        inv.setDriverType(ImagePlatform.valueOf(vmSpec.getVmInventory().getPlatform()).isParaVirtualization() ?
                nicManager.getDefaultPVNicDriver() : nicManager.getDefaultNicDriver());
        inv.setState(VmNicState.enable.toString());

        L3NetworkVO l3NetworkVO = dbf.findByUuid(nicSpec.getL3NetworkUuid(), L3NetworkVO.class);
        VmNicParam nicParam = VmNicSpec.getVmNicParamsOfSpec(vmSpec.getL3Networks())
                .stream().filter(nic -> nicSpec.getL3NetworkUuid().equals(nic.getL3NetworkUuid()))
                .findFirst().orElse(new VmNicParam());

        VmNicType vmNicType = nicManager.getVmNicType(vmSpec.getVmInventory().getUuid(), L3NetworkInventory.valueOf(l3NetworkVO), nicParam.isSriovEnabled());
        if (vmNicType == null) {
            throw new OperationFailureException(Platform.operr("there is no available nicType on L3 network [%s]", l3NetworkVO.getUuid()));
        }
        inv.setType(vmNicType.toString());
        inv.setUsedIps(new ArrayList<>());

        if (nicSpec.getIp() == null) {
            /* for vpc router, code comes here */
            List<Integer> ipVersions = l3NetworkVO.getIpVersions();
            for (Integer version : ipVersions) {
                String strategy = nicSpec.getAllocatorStrategy();
                UsedIpInventory ip = acquireIp(nicSpec.getL3NetworkUuid(), inv.getMac(), version, nicSpec.getStaticIp().get(version), strategy, nicSpec.isAllowDuplicatedAddress());
                /* save first ip to nic */
                if (inv.getGateway() == null) {
                    inv.setGateway(ip.getGateway());
                    inv.setIp(ip.getIp());
                    inv.setNetmask(ip.getNetmask());
                    inv.setUsedIpUuid(ip.getUuid());
                    inv.setIpVersion(ip.getIpVersion());
                }
                inv.getUsedIps().add(ip);
            }
        } else {
            /* for virtual router, code comes here */
            inv.setGateway(nicSpec.getGateway());
            inv.setIp(nicSpec.getIp());
            inv.setNetmask(nicSpec.getNetmask());
            inv.setUsedIpUuid(null);
            if (NetworkUtils.isIpv4Address(nicSpec.getIp())) {
                inv.setIpVersion(IPv6Constants.IPv4);
            } else {
                inv.setIpVersion(IPv6Constants.IPv6);
            }
            if (nicSpec.getMac() != null) {
                inv.setMac(nicSpec.getMac());
            }
        }

        deviceId[0] ++;
        return inv;
    }

    private void removeNicFromDb(List<VmNicInventory> nics) {
        for (VmNicInventory vmNic : nics) {
            VmNicType type = VmNicType.valueOf(vmNic.getType());
            VmInstanceNicFactory vnicFactory = vmMgr.getVmInstanceNicFactory(type);
            vnicFactory.releaseVmNic(vmNic);
        }

        SQL.New(VmNicVO.class).in(VmNicVO_.uuid, nics.stream().map(VmNicInventory::getUuid).collect(Collectors.toList())).delete();
    }

    @Override
    public void run(FlowTrigger chain, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        ApplianceVmSpec aspec = spec.getExtensionData(ApplianceVmConstant.Params.applianceVmSpec.toString(), ApplianceVmSpec.class);
        int[] deviceId = {0};

        List<VmNicInventory> nics = new ArrayList();
        VmNicInventory mgmtNic = makeNicInventory(spec, aspec.getManagementNic(), deviceId);
        nics.add(mgmtNic);

        for (ApplianceVmNicSpec nicSpec : aspec.getAdditionalNics()) {
            nics.add(makeNicInventory(spec, nicSpec, deviceId));
        }

        new SQLBatch() {
            @Override
            protected void scripts() {
                Set<UsedIpVO> ipVOS = new HashSet<>();
                nics.forEach(nic -> {
                    VmNicType vmNicType = VmNicType.valueOf(nic.getType());
                    VmInstanceNicFactory vnicFactory = vmMgr.getVmInstanceNicFactory(vmNicType);
                    vnicFactory.createVmNic(nic, spec);
                    List<UsedIpInventory> ipInvList = new ArrayList<>();
                    if (nic.getUsedIps() != null) {
                        for (UsedIpInventory ip : nic.getUsedIps()) {
                            UsedIpVO ipVO = dbf.findByUuid(ip.getUuid(), UsedIpVO.class);
                            ipVO.setVmNicUuid(nic.getUuid());
                            ipVOS.add(ipVO);
                            ipInvList.add(UsedIpInventory.valueOf(ipVO));
                        }
                    }
                    nic.setUsedIps(ipInvList);
                    spec.getDestNics().removeIf(inv -> nic.getUuid().equals(inv.getUuid()));
                    spec.getDestNics().add(nic);
                });
                dbf.updateCollection(ipVOS);
            }
        }.execute();
        chain.next();
    }

    @Override
    public void rollback(FlowRollback chain, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        List<VmNicInventory> nics = spec.getDestNics();
        if (nics.isEmpty()) {
            chain.rollback();
            return;
        }

        List<ReturnIpMsg> rmsgs = new ArrayList<>();
        for (VmNicInventory nic : nics) {
            if (nic.getUsedIps() == null || nic.getUsedIps().isEmpty()) {
                continue;
            }

            for (UsedIpInventory ip : nic.getUsedIps()) {
                ReturnIpMsg msg = new ReturnIpMsg();
                msg.setL3NetworkUuid(nic.getL3NetworkUuid());
                msg.setUsedIpUuid(ip.getUuid());
                bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, nic.getL3NetworkUuid());
                rmsgs.add(msg);
            }
        }

        if (rmsgs.isEmpty()) {
            removeNicFromDb(nics);
            chain.rollback();
            return;
        }

        new While<>(rmsgs).each((msg, compl) -> {
            bus.send(msg, new CloudBusCallBack(compl) {
                @Override
                public void run(MessageReply reply) {
                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(chain) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                removeNicFromDb(nics);
                chain.rollback();
            }
        });
    }
}
