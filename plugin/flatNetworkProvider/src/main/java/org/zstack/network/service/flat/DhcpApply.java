package org.zstack.network.service.flat;

import com.google.common.collect.Lists;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.network.IPv6Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;

public class DhcpApply {
    CloudBus bus;

    class InternalWorker {
        String l3Uuid;
        List<FlatDhcpBackend.DhcpInfo> info;
        List<FlatDhcpBackend.DhcpInfo> info4;
        List<FlatDhcpBackend.DhcpInfo> info6;
        FlatDhcpAcquireDhcpServerIpReply.DhcpServerIpStruct dhcp4Server = null;
        FlatDhcpAcquireDhcpServerIpReply.DhcpServerIpStruct dhcp6Server = null;

        void acquireDhcpServerIp(Map.Entry<String, List<FlatDhcpBackend.DhcpInfo>> entry, Completion completion) {
            l3Uuid = entry.getKey();
            info = entry.getValue();
            info4 = info.stream().filter(i -> i.ipVersion == IPv6Constants.IPv4).collect(Collectors.toList());
            info6 = info.stream().filter(i -> i.ipVersion == IPv6Constants.IPv6).collect(Collectors.toList());
            DebugUtils.Assert(!info.isEmpty(), "how can info be empty???");

            FlatDhcpAcquireDhcpServerIpMsg msg = new FlatDhcpAcquireDhcpServerIpMsg();
            msg.setL3NetworkUuid(l3Uuid);
            bus.makeTargetServiceIdByResourceUuid(msg, FlatNetworkServiceConstant.SERVICE_ID, l3Uuid);
            bus.send(msg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        completion.fail(reply.getError());
                        return;
                    }
                    FlatDhcpAcquireDhcpServerIpReply r = reply.castReply();
                    List<FlatDhcpAcquireDhcpServerIpReply.DhcpServerIpStruct> dhcpServerIps = r.getDhcpServerList();
                    if (dhcpServerIps == null || dhcpServerIps.isEmpty()) {
                        completion.success();
                        return;
                    }

                    for (FlatDhcpAcquireDhcpServerIpReply.DhcpServerIpStruct struct : dhcpServerIps) {
                        if (struct.getIpVersion() == IPv6Constants.IPv4) {
                            dhcp4Server = struct;
                        } else if (struct.getIpVersion() == IPv6Constants.IPv6) {
                            dhcp6Server = struct;
                        }
                    }
                    if (!info4.isEmpty() && dhcp4Server == null) {
                        completion.fail(operr("could not get dhcp4 server ip for l3 network [uuid:%s]", msg.getL3NetworkUuid()));
                        return;
                    }
                    if (!info6.isEmpty() && dhcp6Server == null) {
                        completion.fail(operr("could not get dhcp6 server ip for l3 network [uuid:%s]", msg.getL3NetworkUuid()));
                        return;
                    }

                    completion.success();
                }
            });
        }

        FlatDhcpBackend.PrepareDhcpCmd getPrepareDhcpCmd() {
            if (dhcp4Server == null && dhcp6Server == null) {
                return null;
            }

            FlatDhcpBackend.DhcpInfo i = info.get(0);

            FlatDhcpBackend.PrepareDhcpCmd cmd = new FlatDhcpBackend.PrepareDhcpCmd();
            cmd.bridgeName = i.bridgeName;
            cmd.virtualNetworkId = i.virtualNetworkId;
            cmd.namespaceName = i.namespaceName;
            if (dhcp4Server != null) {
                cmd.dhcpServerIp = dhcp4Server.getIp();
                cmd.dhcpNetmask = dhcp4Server.getNetmask();
            }
            if (dhcp6Server != null) {
                cmd.dhcp6ServerIp = dhcp6Server.getIp();
                cmd.prefixLen = dhcp6Server.getIpr().getPrefixLen();
                cmd.addressMode = dhcp6Server.getIpr().getAddressMode();
            }
            if (dhcp4Server != null && dhcp6Server == null) {
                cmd.ipVersion = IPv6Constants.IPv4;
            } else if (dhcp4Server == null && dhcp6Server != null) {
                cmd.ipVersion = IPv6Constants.IPv6;
            } else {
                cmd.ipVersion = IPv6Constants.DUAL_STACK;
            }

            return cmd;
        }

        FlatDhcpBackend.ApplyDhcpCmd getApplyDhcpCmd(boolean rebuild) {
            FlatDhcpBackend.ApplyDhcpCmd cmd = new FlatDhcpBackend.ApplyDhcpCmd();
            cmd.dhcp = info;
            cmd.rebuild = rebuild;
            cmd.l3NetworkUuid = l3Uuid;

            return cmd;
        }
    }

    void apply(Map<String, List<FlatDhcpBackend.DhcpInfo>> e, String hostUuid, boolean rebuild, Completion completion) {
        if (e == null || e.isEmpty()) {
            completion.success();
            return;
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("flat-dhcp-provider-apply-dhcp-on-host-%s", hostUuid));
        chain.then(new ShareFlow() {
            List<InternalWorker> internalWorkers = new ArrayList<>();

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "batch-acquire-dhcp-ip";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new While<>(e.entrySet()).each((entry, c) -> {
                            InternalWorker internalWorker = new InternalWorker();
                            internalWorker.acquireDhcpServerIp(entry, new Completion(c) {
                                @Override
                                public void success() {
                                    internalWorkers.add(internalWorker);
                                    c.done();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    c.addError(errorCode);
                                    c.allDone();
                                }
                            });
                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (!errorCodeList.getCauses().isEmpty()) {
                                    trigger.fail(errorCodeList.getCauses().get(0));
                                    return;
                                }

                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "batch-prepare-distributed-dhcp-server-on-host";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<KVMHostAsyncHttpCallMsg> msgs = new ArrayList<>();
                        List<List<InternalWorker>> subSets = Lists.partition(internalWorkers, 50);
                        for (List<InternalWorker> internalWorkers : subSets) {
                            List<FlatDhcpBackend.PrepareDhcpCmd> dhcpCmds = internalWorkers
                                    .stream()
                                    .map(InternalWorker::getPrepareDhcpCmd)
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toList());

                            FlatDhcpBackend.BatchPrepareDhcpCmd cmd = new FlatDhcpBackend.BatchPrepareDhcpCmd();
                            cmd.dhcpInfos = dhcpCmds;

                            KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                            msg.setHostUuid(hostUuid);
                            msg.setNoStatusCheck(true);
                            msg.setCommand(cmd);
                            msg.setPath(FlatDhcpBackend.BATCH_PREPARE_DHCP_PATH);
                            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
                            msgs.add(msg);
                        }

                        new While<>(msgs).each((msg, c) -> bus.send(msg, new CloudBusCallBack(c) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    c.addError(reply.getError());
                                    c.allDone();
                                    return;
                                }

                                KVMHostAsyncHttpCallReply ar = reply.castReply();
                                FlatDhcpBackend.PrepareDhcpRsp rsp = ar.toResponse(FlatDhcpBackend.PrepareDhcpRsp.class);
                                if (!rsp.isSuccess()) {
                                    c.addError(operr("operation error, because:%s", rsp.getError()));
                                    c.allDone();
                                    return;
                                }

                                c.done();
                            }
                        })).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (!errorCodeList.getCauses().isEmpty()) {
                                    trigger.fail(errorCodeList.getCauses().get(0));
                                    return;
                                }

                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "batch-apply-dhcp";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<KVMHostAsyncHttpCallMsg> msgs = new ArrayList<>();
                        List<List<InternalWorker>> subSets = Lists.partition(internalWorkers, 50);
                        for (List<InternalWorker> internalWorkers : subSets) {
                            List<FlatDhcpBackend.ApplyDhcpCmd> dhcpCmds = internalWorkers
                                    .stream()
                                    .map(worker -> worker.getApplyDhcpCmd(rebuild))
                                    .collect(Collectors.toList());

                            FlatDhcpBackend.BatchApplyDhcpCmd cmd = new FlatDhcpBackend.BatchApplyDhcpCmd();
                            cmd.dhcpInfos = dhcpCmds;

                            KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                            msg.setHostUuid(hostUuid);
                            msg.setNoStatusCheck(true);
                            msg.setCommand(cmd);
                            msg.setPath(FlatDhcpBackend.BATCH_APPLY_DHCP_PATH);
                            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
                            msgs.add(msg);
                        }

                        new While<>(msgs).each((msg, c) -> bus.send(msg, new CloudBusCallBack(c) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    c.addError(reply.getError());
                                    c.allDone();
                                    return;
                                }

                                KVMHostAsyncHttpCallReply ar = reply.castReply();
                                FlatDhcpBackend.ApplyDhcpRsp rsp = ar.toResponse(FlatDhcpBackend.ApplyDhcpRsp.class);
                                if (!rsp.isSuccess()) {
                                    c.addError(operr("operation error, because:%s", rsp.getError()));
                                    c.allDone();
                                    return;
                                }

                                c.done();
                            }
                        })).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (!errorCodeList.getCauses().isEmpty()) {
                                    trigger.fail(errorCodeList.getCauses().get(0));
                                    return;
                                }

                                trigger.next();
                            }
                        });
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }
}
