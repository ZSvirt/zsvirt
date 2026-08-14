package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.*;
import org.zstack.header.vm.*;
import org.zstack.network.l3.L3NetworkManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;

import java.util.*;
import java.util.stream.Collectors;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmAllocateNicIpFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(VmAllocateNicIpFlow.class);
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected L3NetworkManager l3nm;
    @Autowired
    private VmNicManager nicManager;
    @Autowired
    protected VmInstanceManager vmMgr;

    @Override
    public String name() {
        return "allocate-nics-ip";
    }

    @Override
    public void run(final FlowTrigger trigger, final Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        final List<VmNicInventory> nics = (List<VmNicInventory>) data.get(VmInstanceConstant.Params.VmAllocateNicFlow_nics.toString());
        Boolean allowDuplicatedAddress = (Boolean) data.get(VmInstanceConstant.Params.VmAllocateNicFlow_allowDuplicatedAddress.toString());
        Map<String, List<String>> vmStaticIps = new StaticIpOperator().getStaticIpByVmUuid(spec.getVmInventory().getUuid());
        List<ErrorCode> errs = new ArrayList<>();
        Set<UsedIpVO> ipVOS = new HashSet<>();
        List<VmNicVO> nicsWithIp = new ArrayList<>();
        List<UsedIpInventory> ips = new ArrayList<>();
        data.put(VmInstanceConstant.Params.VmAllocateNicFlow_nics.toString(), nicsWithIp);
        data.put(VmInstanceConstant.Params.VmAllocateNicFlow_ips.toString(), ips);

        final Map<String, LinkedList<VmNicInventory>> nicsL3s = new HashMap<>();
        nics.forEach(nic -> {
            nicsL3s.computeIfAbsent(nic.getL3NetworkUuid(), k -> new LinkedList<>()).add(nic);
        });
        if (spec.isSkipIpAllocation()) {
            trigger.next();
            return;
        }
        List<VmNicSpec> firstL3s = VmNicSpec.getFirstL3NetworkInventoryOfSpec(spec.getL3Networks())
                .stream()
                .filter(v -> {
                    if (v.getL3Invs().get(0).enableIpAllocation()) {
                        return true;
                    }

                    /* if dhcp is disabled, will not allocate ip address for user vm
                     *  allocated ip to appliance vm based on the following conditions */
                    if (spec.getVmInventory().getType().equals(VmInstanceConstant.USER_VM_TYPE)) {
                        return false;
                    } else {
                        L3NetworkInventory nw = v.getL3Invs().get(0);
                        VmNicInventory nic = nicsL3s.get(nw.getUuid()).stream().findFirst().orElse(null);
                        if (nic == null) {  
                            return false;
                        }
                        VmNicVO nicVO = dbf.findByUuid(nic.getUuid(), VmNicVO.class);
                        if (nicVO == null) {
                            return false;
                        }

                        /* if there is no ip range, or creates ip in VmAllocateNicFlow from systemTags  */
                        return nicVO.getUsedIps().isEmpty() && !nw.getIpRanges().isEmpty();
                    }
                })
                .peek(v -> {
                    if (!Q.New(NormalIpRangeVO.class)
                            .eq(NormalIpRangeVO_.l3NetworkUuid, v.getL3Invs().get(0).getUuid())
                            .isExists()) {
                        throw new OperationFailureException(Platform.operr("there is no available ipRange on L3 network [%s]", v.getL3Invs().get(0).getUuid()));
                    }
                })
                .collect(Collectors.toList());

        new While<>(firstL3s).each((nicSpec, wcomp) -> {
            L3NetworkInventory nw = nicSpec.getL3Invs().get(0);
            VmNicInventory nicUuid = nicsL3s.getOrDefault(nw.getUuid(), new LinkedList<>()).poll();
            if (nicUuid == null) {
                wcomp.done();
                logger.warn(String.format("no nic for l3Network[uuid:%s] to allocate ip", nw.getUuid()));
                return;
            }

            VmNicVO nic = dbf.findByUuid(nicUuid.getUuid(), VmNicVO.class);
            List<Integer> ipVersions = nw.getIpVersions();
            Map<Integer, String> nicStaticIpMap = new StaticIpOperator().getNicStaticIpMap(vmStaticIps.get(nw.getUuid()));
            List<AllocateIpMsg> msgs = new ArrayList<>();
            for (int ipversion : ipVersions) {
                AllocateIpMsg msg = new AllocateIpMsg();
                msg.setL3NetworkUuid(nw.getUuid());
                msg.setAllocateStrategy(spec.getIpAllocatorStrategy());
                String staticIp = nicStaticIpMap.get(ipversion);
                if (staticIp != null) {
                    msg.setRequiredIp(staticIp);
                } else {
                    if (ipversion == IPv6Constants.IPv6) {
                        l3nm.updateIpAllocationMsg(msg, nic.getMac());
                    }
                }
                if (allowDuplicatedAddress != null) {
                    msg.setDuplicatedIpAllowed(allowDuplicatedAddress);
                }
                msg.setIpVersion(ipversion);
                bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, nw.getUuid());
                msgs.add(msg);
            }
            List<ErrorCode> ipErrs = new ArrayList<>();
            List<UsedIpInventory> nicIps = new ArrayList<>();
            new While<>(msgs).each((msg, wcompl) -> {
                bus.send(msg, new CloudBusCallBack(wcompl) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            AllocateIpReply areply = reply.castReply();
                            ips.add(areply.getIpInventory());
                            nicIps.add(areply.getIpInventory());
                            wcompl.done();
                        } else {
                            ipErrs.add(reply.getError());
                            wcompl.allDone();
                        }
                    }
                });
            }).run(new WhileDoneCompletion(wcomp) {
                @Override
                public void done(ErrorCodeList errorCodeList) {
                    if (ipErrs.size() > 0) {
                        errs.add(ipErrs.get(0));
                        wcomp.allDone();
                    } else {
                        List<UsedIpInventory> sortedNicsIps = nicIps.stream()
                                .sorted(Comparator.comparingInt(UsedIpInventory::getIpVersion))
                                .collect(Collectors.toList());
                        UsedIpInventory ip = sortedNicsIps.get(0);
                        nic.setIp(ip.getIp());
                        nic.setIpVersion(ip.getIpVersion());
                        nic.setUsedIpUuid(ip.getUuid());
                        nic.setNetmask(ip.getNetmask());
                        nic.setGateway(ip.getGateway());
                        for (UsedIpInventory usedIp : sortedNicsIps) {
                            /* update usedIpVo */
                            UsedIpVO ipVO = Q.New(UsedIpVO.class).eq(UsedIpVO_.uuid, usedIp.getUuid()).find();
                            ipVO.setVmNicUuid(nic.getUuid());
                            ipVOS.add(ipVO);
                        }
                        nic.setUsedIps(ipVOS);
                        nicsWithIp.add(nic);
                        spec.getDestNics().removeIf(inv -> nic.getUuid().equals(inv.getUuid()));
                        spec.getDestNics().add(VmNicInventory.valueOf(nic));
                        wcomp.done();
                    }
                }
            });
        }).run(new WhileDoneCompletion(trigger) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errs.size() > 0) {
                    trigger.fail(errs.get(0));
                } else {
                    dbf.updateCollection(nicsWithIp);
                    dbf.updateCollection(ipVOS);
                    trigger.next();
                }
            }
        });
    }

    @Override
    public void rollback(final FlowRollback chain, Map data) {
        List<UsedIpInventory> ips = (List<UsedIpInventory>) data.get(VmInstanceConstant.Params.VmAllocateNicFlow_ips.toString());
        List<ReturnIpMsg> msgs = new ArrayList<>();
        for (UsedIpInventory ip : ips) {
            ReturnIpMsg msg = new ReturnIpMsg();
            msg.setL3NetworkUuid(ip.getL3NetworkUuid());
            msg.setUsedIpUuid(ip.getUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, ip.getL3NetworkUuid());
            msgs.add(msg);
        }
        if (msgs.isEmpty()) {
            chain.rollback();
            return;
        }
        bus.send(msgs, 1, new CloudBusListCallBack(chain) {
            @Override
            public void run(List<MessageReply> replies) {
                chain.rollback();
            }
        });
    }
}
