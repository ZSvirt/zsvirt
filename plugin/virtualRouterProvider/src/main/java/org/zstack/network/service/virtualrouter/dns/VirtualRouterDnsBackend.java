package org.zstack.network.service.virtualrouter.dns;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.appliancevm.ApplianceVmStatus;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.*;
import org.zstack.header.network.service.*;
import org.zstack.header.vm.*;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.*;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;

import java.util.*;
import javax.persistence.Tuple;

/**
 */
public class VirtualRouterDnsBackend extends AbstractVirtualRouterBackend implements NetworkServiceDnsBackend,
        VirtualRouterBeforeDetachNicExtensionPoint, VirtualRouterAfterAttachNicExtensionPoint {
    private final CLogger logger = Utils.getLogger(VirtualRouterDnsBackend.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ApiTimeoutManager apiTimeoutManager;
    @Autowired
    protected DatabaseFacade dbf;

    @Override
    public NetworkServiceProviderType getProviderType() {
        return VirtualRouterConstant.PROVIDER_TYPE;
    }

    public List<VirtualRouterCommands.DnsInfo> getDnsInfoOfVr(String vrUuid, String execludeL3NetworkUuid) {
        /* for vpc network, there are multiple network, so there may have multiple dns for a virtual router,
         * for duplicated dns address, choose the dns with minimum index which is added first */
        if (execludeL3NetworkUuid == null) {
            execludeL3NetworkUuid = "";
        }
        String sql = "select l3.dns, nic.mac from L3NetworkDnsVO l3, VmNicVO nic where l3.l3NetworkUuid = nic.l3NetworkUuid " +
                "and l3.l3NetworkUuid != :execludeL3NetworkUuid and nic.vmInstanceUuid = :vrUuid and nic.metaData in (:guestNic) order by l3.id";
        List<Tuple> tuples = SQL.New(sql, Tuple.class).param("execludeL3NetworkUuid", execludeL3NetworkUuid).param("vrUuid", vrUuid)
                .param("guestNic", VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST).list();

        Map<String, List<String>> dnsMap = new HashMap<>();
        if (tuples != null && !tuples.isEmpty()) {
            tuples.forEach(tuple -> {
                List<String> lst = dnsMap.computeIfAbsent(tuple.get(0, String.class), k -> new ArrayList<>());
                lst.add(tuple.get(1, String.class));
            });
        }

        List<VirtualRouterCommands.DnsInfo> dnsInfos = new ArrayList<>();
        if (tuples != null && !tuples.isEmpty()) {
            for (Tuple tuple: tuples) {
                String dns = tuple.get(0, String.class);
                List<String> macList = dnsMap.get(dns);
                if (macList != null && !macList.isEmpty()) {
                    for (String mac: macList) {
                        VirtualRouterCommands.DnsInfo info = new VirtualRouterCommands.DnsInfo();
                        info.setDnsAddress(dns);
                        info.setNicMac(mac);
                        dnsInfos.add(info);
                    }
                }

                dnsMap.remove(dns);
                if (dnsMap.isEmpty()) {
                    break;
                }
            }
        }

        return dnsInfos;
    }

    @Override
    public void addDns(L3NetworkInventory l3, List<String> dns, final Completion completion) {
        VirtualRouterVmInventory vr = vrMgr.getVirtualRouterVm(l3);
        if (vr == null || !VmInstanceState.Running.toString().equals(vr.getState()) || !ApplianceVmStatus.Connected.toString().equals(vr.getStatus())) {
            completion.success();
            return;
        }

        SetDnsCmd cmd = new SetDnsCmd();
        cmd.setDns(getDnsInfoOfVr(vr.getUuid(), null));

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setVmInstanceUuid(vr.getUuid());
        msg.setPath(VirtualRouterConstant.VR_SET_DNS_PATH);
        msg.setCommand(cmd);
        msg.setCheckStatus(true);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                VirtualRouterAsyncHttpCallReply r = reply.castReply();
                SetDnsRsp rsp = r.toResponse(SetDnsRsp.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("operation error, because:%s", rsp.getError()));
                    return;
                }

                completion.success();
            }
        });
    }

    @Override
    public void removeDns(L3NetworkInventory l3, List<String> dns, final Completion completion) {
        VirtualRouterVmInventory vr = vrMgr.getVirtualRouterVm(l3);
        if (vr == null || !VmInstanceState.Running.toString().equals(vr.getState()) || !ApplianceVmStatus.Connected.toString().equals(vr.getStatus())) {
            completion.success();
            return;
        }

        RemoveDnsCmd cmd = new RemoveDnsCmd();

        List<VirtualRouterCommands.DnsInfo> vrDns = getDnsInfoOfVr(vr.getUuid(), null);
        cmd.setDns(vrDns);

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setVmInstanceUuid(vr.getUuid());
        msg.setPath(VirtualRouterConstant.VR_SET_DNS_PATH);
        msg.setCommand(cmd);
        msg.setCheckStatus(true);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                VirtualRouterAsyncHttpCallReply r = reply.castReply();
                RemoveDnsRsp rsp = r.toResponse(RemoveDnsRsp.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("operation error, because:%s", rsp.getError()));
                    return;
                }

                completion.success();
            }
        });
    }

    private void applyDns(final Iterator<DnsStruct> it, final VmInstanceSpec spec, final Completion completion) {
        if (!it.hasNext()) {
            completion.success();
            return;
        }

        final DnsStruct struct = it.next();
        final L3NetworkInventory l3 = struct.getL3Network();

        VirtualRouterStruct s = new VirtualRouterStruct();
        s.setL3Network(l3);

        acquireVirtualRouterVm(s, new ReturnValueCompletion<VirtualRouterVmInventory>(completion) {
            @Override
            public void success(final VirtualRouterVmInventory vr) {
                VirtualRouterCommands.SetDnsCmd cmd = new VirtualRouterCommands.SetDnsCmd();
                cmd.setDns(getDnsInfoOfVr(vr.getUuid(), null));

                VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
                msg.setVmInstanceUuid(vr.getUuid());
                msg.setPath(VirtualRouterConstant.VR_SET_DNS_PATH);
                msg.setCommand(cmd);
                msg.setCheckStatus(true);
                bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
                bus.send(msg, new CloudBusCallBack(completion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            completion.fail(reply.getError());
                            return;
                        }

                        VirtualRouterAsyncHttpCallReply re = reply.castReply();
                        SetDnsRsp ret = re.toResponse(SetDnsRsp.class);
                        if (ret.isSuccess()) {
                            new VirtualRouterRoleManager().makeDnsRole(vr.getUuid());
                            logger.debug(String.format("successfully add dns entry[%s] to virtual router vm[uuid:%s, ip:%s]", struct, vr.getUuid(), vr.getManagementNic()
                                    .getIp()));
                            applyDns(it, spec, completion);
                        } else {
                            ErrorCode err = operr("virtual router[uuid:%s, ip:%s] failed to configure dns%s for L3Network[uuid:%s, name:%s], %s",
                                    vr.getUuid(), vr.getManagementNic().getIp(), struct, l3.getUuid(), l3.getName(), ret.getError());
                            completion.fail(err);
                        }
                    }
                });
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void applyDnsService(List<DnsStruct> dnsStructList, VmInstanceSpec spec, Completion completion) {
        completion.success();
    }


    private void releaseDns(final Iterator<DnsStruct> it, final VmInstanceSpec spec, final NoErrorCompletion completion) {
        if (!it.hasNext()) {
            completion.done();
            return;
        }

        DnsStruct struct = it.next();
        if (!vrMgr.isVirtualRouterRunningForL3Network(struct.getL3Network().getUuid())) {
            logger.debug(String.format("virtual router for l3Network[uuid:%s] is not running, skip releasing DNS", struct.getL3Network().getUuid()));
            releaseDns(it, spec, completion);
            return;
        }

        final VirtualRouterVmInventory vr = vrMgr.getVirtualRouterVm(struct.getL3Network());

        final List<VirtualRouterCommands.DnsInfo> info = new ArrayList<VirtualRouterCommands.DnsInfo>();
        for (String dns : struct.getDns()) {
            VirtualRouterCommands.DnsInfo i = new VirtualRouterCommands.DnsInfo();
            i.setDnsAddress(dns);
            i.setNicMac(vr.getGuestNicByL3NetworkUuid(struct.getL3Network().getUuid()).getMac());
            info.add(i);
        }

        VirtualRouterCommands.RemoveDnsCmd cmd = new VirtualRouterCommands.RemoveDnsCmd();
        cmd.setDns(info);

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setCheckStatus(true);
        msg.setPath(VirtualRouterConstant.VR_REMOVE_DNS_PATH);
        msg.setCommand(cmd);
        msg.setVmInstanceUuid(vr.getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.debug(String.format("virtual router[name: %s, uuid: %s] failed to remove dns%s, because %s",
                            vr.getName(), vr.getUuid(), JSONObjectUtil.toJsonString(info), reply.getError()));
                    // TODO GC
                } else {
                    VirtualRouterAsyncHttpCallReply re = reply.castReply();
                    RemoveDnsRsp ret = re.toResponse(RemoveDnsRsp.class);
                    if (ret.isSuccess()) {
                        logger.debug(String.format("virtual router[name: %s, uuid: %s] successfully removed dns%s",
                                vr.getName(), vr.getUuid(), JSONObjectUtil.toJsonString(info)));
                    } else {
                        logger.debug(String.format("virtual router[name: %s, uuid: %s] failed to remove dns%s, because %s",
                                vr.getName(), vr.getUuid(), JSONObjectUtil.toJsonString(info), ret.getError()));
                        //TODO GC
                    }
                }

                releaseDns(it, spec, completion);
            }
        });
    }

    @Override
    public void releaseDnsService(List<DnsStruct> dnsStructList, VmInstanceSpec spec, NoErrorCompletion completion) {
        completion.done();
    }

    private void ApplyDnsForVirtualRouter(String vrUuid, String execludeL3NetworkUuid, Completion completion){
        VirtualRouterCommands.SetDnsCmd cmd = new VirtualRouterCommands.SetDnsCmd();
        cmd.setDns(getDnsInfoOfVr(vrUuid, execludeL3NetworkUuid));

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setCheckStatus(true);
        msg.setPath(VirtualRouterConstant.VR_SET_DNS_PATH);
        msg.setCommand(cmd);
        msg.setVmInstanceUuid(vrUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vrUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.debug(String.format("virtual router[uuid: %s] failed to apply dns, because %s",
                            vrUuid, reply.getError()));
                    completion.fail(reply.getError());
                } else {
                    VirtualRouterAsyncHttpCallReply re = reply.castReply();
                    RemoveDnsRsp ret = re.toResponse(RemoveDnsRsp.class);
                    if (ret.isSuccess()) {
                        logger.debug(String.format("virtual router[uuid: %s] successfully apply dns",
                                vrUuid));
                        completion.success();
                    } else {
                        logger.debug(String.format("virtual router[uuid: %s] failed to apply dns, because %s",
                                vrUuid, ret.getError()));
                        completion.fail(reply.getError());
                    }
                }
            }
        });
    }

    @Override
    public void beforeDetachNic(VmNicInventory nic, Completion completion) {
        if (!isVirtualRouterDnsBackend(nic)) {
            completion.success();
            return;
        }

        ApplyDnsForVirtualRouter(nic.getVmInstanceUuid(), nic.getL3NetworkUuid(), completion);
    }

    @Override
    public void beforeDetachNicRollback(VmNicInventory nic, NoErrorCompletion completion) {
        if (!isVirtualRouterDnsBackend(nic)) {
            completion.done();
            return;
        }

        ApplyDnsForVirtualRouter(nic.getVmInstanceUuid(), null, new Completion(completion) {
            @Override
            public void success() {
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.done();
            }
        });
    }

    @Override
    public void afterAttachNic(VmNicInventory nic, Completion completion) {
        if (!isVirtualRouterDnsBackend(nic)) {
            completion.success();
            return;
        }

        ApplyDnsForVirtualRouter(nic.getVmInstanceUuid(), null, completion);
    }

    @Override
    public void afterAttachNicRollback(VmNicInventory nic, NoErrorCompletion completion) {
        if (!isVirtualRouterDnsBackend(nic)) {
            completion.done();
            return;
        }

        ApplyDnsForVirtualRouter(nic.getVmInstanceUuid(), nic.getL3NetworkUuid(), new Completion(completion) {
            @Override
            public void success() {
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.done();
            }
        });
    }

    private boolean isVirtualRouterDnsBackend(VmNicInventory nic) {
        if (!VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST.contains(nic.getMetaData())) {
            return false;
        }

        if (!Q.New(NetworkServiceL3NetworkRefVO.class).eq(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, nic.getL3NetworkUuid())
                .eq(NetworkServiceL3NetworkRefVO_.networkServiceType, NetworkServiceType.DNS.toString()).isExists()) {
            return false;
        }

        L3NetworkVO l3Vo = dbf.findByUuid(nic.getL3NetworkUuid(), L3NetworkVO.class);
        if (!L3NetworkConstant.L3_BASIC_NETWORK_TYPE.equals(l3Vo.getType())) {
            return false;
        }

        return true;
    }
}
