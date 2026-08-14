package org.zstack.compute.host;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.allocator.HostAllocatorError;
import org.zstack.header.allocator.HostCandidate;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.*;
import org.zstack.header.message.MessageReply;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMGlobalConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.i18m;
import static org.zstack.utils.CollectionUtils.toMap;

/**
 * Created by LiangHanYu on 2021/8/13 13:48
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class HostCpuFunctionFilterFlow extends AbstractHostAllocatorFlow {
    private final CLogger logger = Utils.getLogger(HostCpuFunctionFilterFlow.class);
    @Autowired
    private CloudBus bus;
    @Autowired
    private ResourceConfigFacade rcf;
    @Autowired
    protected DatabaseFacade dbf;

    private boolean isEnableCpuFunctionCheck() {
        return KVMGlobalConfig.ENABLE_VM_MIGRATION_HOST_CPU_FUNCTION_CHECK.value(Boolean.class);
    }

    private boolean isVmCpuModeHostPassThrough() {
        return KVMConstant.CPU_MODE_HOST_PASSTHROUGH.equals(rcf.getResourceConfigValue(KVMGlobalConfig.NESTED_VIRTUALIZATION, spec.getVmInstance().getUuid(), String.class));
    }

    private boolean isKvmHypervisorType() {
        return spec.getVmInstance() != null && KVMConstant.KVM_HYPERVISOR_TYPE.equals(spec.getVmInstance().getHypervisorType());
    }

    public void allocate() {
        if (!isEnableCpuFunctionCheck() || !isVmCpuModeHostPassThrough() || !isKvmHypervisorType()) {
            next();
            return;
        }

        Map<String, HostCandidate> uuidCandidateMap = new ConcurrentHashMap<>(
                toMap(candidates, HostCandidate::getUuid, Function.identity()));
        String srcHostUuid = spec.getVmInstance().getHostUuid();

        //If the host's cpu features information exists in the db, directly query and filter the candidates
        // TODO query statement need improvement
        new ArrayList<>(uuidCandidateMap.keySet()).forEach(uuid -> {
            if (Q.New(CpuFeaturesHistoryVO.class)
                    .eq(CpuFeaturesHistoryVO_.srcHostUuid, srcHostUuid)
                    .eq(CpuFeaturesHistoryVO_.dstHostUuid, uuid)
                    .eq(CpuFeaturesHistoryVO_.supportLiveMigration, false).isExists()) {
                reject(uuidCandidateMap.remove(uuid), i18m(
                            "does not support live migration from host[%s] to this host", srcHostUuid));
            }
        });

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("migrate-vm-check-host-cpu-function");
        chain.then(new ShareFlow() {
            String cpuXml;
            String cpuModelName;
            List<String> needToCompareDstHostUuidCandidates = uuidCandidateMap.keySet().stream()
                    .filter(dstHostUuid -> !Q.New(CpuFeaturesHistoryVO.class)
                            .eq(CpuFeaturesHistoryVO_.srcHostUuid, srcHostUuid)
                            .eq(CpuFeaturesHistoryVO_.dstHostUuid, dstHostUuid).isExists())
                    .collect(Collectors.toList());
            Map<String, Boolean> dstHostSupportResult = new HashMap<>();

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "get-cpu-xml-on-src-host";

                    @Override
                    public boolean skip(Map data) {
                        return needToCompareDstHostUuidCandidates.isEmpty();
                    }

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        GetCpuFunctionXmlOnHostMsg msg = new GetCpuFunctionXmlOnHostMsg();
                        msg.setHostUuid(spec.getVmInstance().getHostUuid());
                        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
                        bus.send(msg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                GetCpuFunctionXmlOnHostReply ret = reply.castReply();
                                cpuXml = ret.getCpuXml();
                                cpuModelName = ret.getCpuModelName();
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "compare-cpu-on-dst-host";

                    @Override
                    public boolean skip(Map data) {
                        return needToCompareDstHostUuidCandidates.isEmpty();
                    }

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        new While<>(needToCompareDstHostUuidCandidates).step((dstHostUuid, compl) -> {
                            CompareCpuFunctionOnHostMsg msg = new CompareCpuFunctionOnHostMsg();
                            msg.setDstHostUuid(dstHostUuid);
                            msg.setSrcHostUuid(spec.getVmInstance().getHostUuid());
                            msg.setCpuXml(cpuXml);
                            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
                            bus.send(msg, new CloudBusCallBack(compl) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        reject(uuidCandidateMap.remove(dstHostUuid),
                                                i18m("failed to compare cpu function between this host and host %s",
                                                        spec.getVmInstance().getHostUuid()));
                                    }
                                    dstHostSupportResult.put(dstHostUuid, reply.isSuccess() ? true : false);
                                    compl.done();
                                }
                            });
                        }, 5).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                trigger.next();
                            }
                        });

                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "update-src-cpu-info-to-db";

                    @Override
                    public boolean skip(Map data) {
                        return needToCompareDstHostUuidCandidates.isEmpty();
                    }

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        new While<>(dstHostSupportResult.entrySet()).step((result, comp) -> {
                            CreateCpuFeaturesHistoryMsg msg = new CreateCpuFeaturesHistoryMsg();
                            msg.setSrcHostUuid(spec.getVmInstance().getHostUuid());
                            msg.setDstHostUuid(result.getKey());
                            msg.setSupportLiveMigration(result.getValue());
                            msg.setSrcCpuModelName(cpuModelName);
                            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
                            bus.send(msg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        logger.warn(String.format("create cpu features history[src:%s , dst:%s] fail, because: %s", msg.getHostUuid(), msg.getDstHostUuid(), reply.getError().getDetails()));
                                    }
                                    comp.done();
                                }
                            });
                        }, 10).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                trigger.next();
                            }
                        });
                    }
                });
            }
        }).done(new FlowDoneHandler(null) {
            @Override
            public void handle(Map data) {
                // TODO: bug. if call allocatorTriggerFail, the next pagination loop will be skipped
                // but if we call fail() here, because the current thread is different from
                // the thread that originally called the allocate() method,
                // The error thrown here can not caught by upper layer(HostAllocatorChain.runFlow())
                if (uuidCandidateMap.isEmpty()) {
                    allocatorTriggerFail(err(HostAllocatorError.NO_AVAILABLE_HOST, "HostCpuFunctionFilterFlow return zero candidate host"));
                    return;
                }
                next();
            }
        }).error(new FlowErrorHandler(null) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                allocatorTriggerFail(errCode);
            }
        }).start();
    }
}
