package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.*;
import org.zstack.header.vm.*;
import org.zstack.network.l3.L3NetworkManager;
import org.zstack.utils.network.IPv6Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmAllocateNicForStartingVmFlow implements Flow {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected L3NetworkManager l3nm;
    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public void run(final FlowTrigger trigger, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        final VmInstanceInventory vm = spec.getVmInventory();

        if (!VmInstanceConstant.USER_VM_TYPE.equals(vm.getType())) {
            trigger.next();
            return;
        }

        final List<VmNicInventory> nicsNeedNewIp = new ArrayList<VmNicInventory>(vm.getVmNics().size());
        final List<VmNicInventory> usedIpNics = new ArrayList<VmNicInventory>();
        for (VmNicInventory nic : vm.getVmNics()) {
            L3NetworkVO l3VO = dbf.findByUuid(nic.getL3NetworkUuid(), L3NetworkVO.class);
            if (!l3VO.enableIpAllocation()) {
                continue;
            }
            if (nic.getUsedIpUuid() == null) {
                nic.getUsedIps().clear();
                nicsNeedNewIp.add(nic);
            } else {
                usedIpNics.add(nic);
            }
        }

        // normally, the usedIpUuid of VmNicVO will be set to NULL after an IP range is deleted.
        // however, ill database foreign keys (developers' fault) may cause usedIpUuid not to
        // be cleaned; so in addition to check NULL usedIpUuid, we double check if IP range for every
        // nic is still alive.
        // another case, there is new ipv4(or ipv6) range attached th3 of the nic, when vm stop/start,
        // allocate a ip address for the nic
        if (!usedIpNics.isEmpty()) {
            nicsNeedNewIp.addAll(findNicsNeedNewIps(usedIpNics));
        }

        if (nicsNeedNewIp.isEmpty()) {
            trigger.next();
            return;
        }

        final Map<String, List<String>> vmStaticIps = new StaticIpOperator().getStaticIpByVmUuid(vm.getUuid());
        List<AllocateIpMsg> amsgs = new ArrayList<>();
        for (VmNicInventory nic : nicsNeedNewIp) {
            L3NetworkInventory l3Inv = L3NetworkInventory.valueOf(dbf.findByUuid(nic.getL3NetworkUuid(), L3NetworkVO.class));
            List<Integer> ipVersions = l3Inv.getIpVersions();
            Map<Integer, String> nicStaticIpMap = new StaticIpOperator().getNicStaticIpMap(vmStaticIps.get(nic.getL3NetworkUuid()));
            for (int ipversion: ipVersions) {
                boolean existed = false;
                for (UsedIpInventory ip : nic.getUsedIps()) {
                    if (ip.getIpVersion() == ipversion) {
                        existed = true;
                        break;
                    }
                }
                if (existed) {
                    continue;
                }

                AllocateIpMsg msg = new AllocateIpMsg();
                msg.setL3NetworkUuid(nic.getL3NetworkUuid());
                msg.setAllocateStrategy(spec.getIpAllocatorStrategy());

                String staticIp = nicStaticIpMap.get(ipversion);
                if (staticIp != null) {
                    msg.setRequiredIp(staticIp);
                } else {
                    if (ipversion == IPv6Constants.IPv6) {
                        l3nm.updateIpAllocationMsg(msg, nic.getMac());
                    }
                }
                msg.setIpVersion(ipversion);
                bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, nic.getL3NetworkUuid());
                amsgs.add(msg);
            }
        }


        final List<UsedIpInventory> allocatedIPs = new ArrayList<UsedIpInventory>();
        data.put(VmAllocateNicForStartingVmFlow.class, allocatedIPs);

        List<ErrorCode> errs = new ArrayList<>();
        new While<>(amsgs).each((msg, wcompl) -> {
            bus.send(msg, new CloudBusCallBack(wcompl) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        errs.add(reply.getError());
                        wcompl.allDone();
                    }

                    final AllocateIpReply ar = reply.castReply();
                    final UsedIpInventory ip = ar.getIpInventory();
                    allocatedIPs.add(ip);

                    VmNicInventory nic = nicsNeedNewIp.stream()
                            .filter(n -> n.getL3NetworkUuid().equals(ip.getL3NetworkUuid()))
                            .findFirst().orElse(new VmNicInventory());
                    nicsNeedNewIp.remove(nic);

                    for (VmNicExtensionPoint ext : pluginRgty.getExtensionList(VmNicExtensionPoint.class)) {
                        ext.afterAddIpAddress(nic.getUuid(), ip.getUuid());
                    }
                    wcompl.done();
                }
            });
        }).run(new WhileDoneCompletion(trigger) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errs.size() > 0) {
                    trigger.fail(errs.get(0));
                } else {
                    VmInstanceVO vmvo = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);
                    spec.setVmInventory(VmInstanceInventory.valueOf(vmvo));
                    spec.setDestNics(spec.getVmInventory().getVmNics());
                    trigger.next();
                }
            }
        });
    }

    @Transactional(readOnly = true)
    private List<VmNicInventory> findNicsNeedNewIps(List<VmNicInventory> nics) {
        /* for vmnic, its network upgrade from ipv4 or ipv6 only to dual stack network  */
        List<VmNicInventory> needIps = new ArrayList<VmNicInventory>();

        for (VmNicInventory nic : nics) {
            L3NetworkVO l3Vo = dbf.findByUuid(nic.getL3NetworkUuid(), L3NetworkVO.class);
            if (nic.getUsedIps().size() < l3Vo.getIpVersions().size()) {
                needIps.add(nic);
            }

            /* this case should not happend */
            for (UsedIpInventory ip : nic.getUsedIps()) {
                if (!Q.New(NormalIpRangeVO.class).eq(NormalIpRangeVO_.uuid, ip.getIpRangeUuid()).isExists()) {
                    needIps.add(nic);
                }
            }
        }
        return needIps;
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        List<UsedIpInventory> allocatedIps = (List<UsedIpInventory>) data.get(VmAllocateNicForStartingVmFlow.class);
        if (allocatedIps == null || allocatedIps.isEmpty()) {
            trigger.rollback();
            return;
        }

        new While<>(allocatedIps).all((ip, cmpl) -> {
            ReturnIpMsg rmsg = new ReturnIpMsg();
            rmsg.setL3NetworkUuid(ip.getL3NetworkUuid());
            rmsg.setUsedIpUuid(ip.getUuid());
            bus.makeTargetServiceIdByResourceUuid(rmsg, L3NetworkConstant.SERVICE_ID, ip.getL3NetworkUuid());
            bus.send(rmsg, new CloudBusCallBack(cmpl) {
                @Override
                public void run(MessageReply reply) {
                    for (VmNicExtensionPoint ext : pluginRgty.getExtensionList(VmNicExtensionPoint.class)) {
                        ext.afterDelIpAddress(ip.getVmNicUuid(), ip.getUuid());
                    }
                    cmpl.done();
                }
            });
        }).run(new WhileDoneCompletion(trigger) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                trigger.rollback();
            }
        });
    }
}
