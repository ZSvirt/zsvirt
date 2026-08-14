package org.zstack.guesttools.kvm;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.ansible.AnsibleRunner;
import org.zstack.core.ansible.SshFileMd5Checker;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.guesttools.*;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostInventory;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.cdrom.VmCdRomVO;
import org.zstack.header.vm.cdrom.VmCdRomVO_;
import org.zstack.kvm.*;
import org.zstack.mevoco.MevocoSystemTags;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.zwatch.ZWatchConstants;
import org.zstack.zwatch.ZWatchManager;
import org.zstack.zwatch.api.GetMetricDataMsg;
import org.zstack.zwatch.api.GetMetricDataReply;
import org.zstack.zwatch.datatype.Datapoint;
import org.zstack.zwatch.datatype.Label;
import org.zstack.zwatch.namespace.VmNamespace;

import javax.persistence.Tuple;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;
import static org.zstack.guesttools.GuestToolsConstant.*;
import static org.zstack.guesttools.header.GuestToolsErrors.*;
import static org.zstack.guesttools.kvm.GuestToolsKvmCommands.*;

/**
 * Created by Wenhao.Zhang on 21/07/18
 */
public abstract class GuestToolsOnKvmBackend implements GuestToolsHypervisorBackend {
    private static final CLogger logger = Utils.getLogger(GuestToolsOnKvmBackend.class);
    @Autowired
    protected CloudBus bus;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ZWatchManager zWatchManager;

    protected void getMetricDataList(String vmUuid, String metricName, ReturnValueCompletion<List<Double>> completion) {
        String namespace = String.format("ZStack/%s", VmNamespace.NAME);
        String label = String.format("%s%s%s", VmNamespace.LabelNames.VMUuid.toString(), Label.Operator.Equal.toString(), vmUuid);

        GetMetricDataMsg getMetricDataMsg = new GetMetricDataMsg();
        getMetricDataMsg.setNamespace(namespace);
        getMetricDataMsg.setMetricName(metricName);
        getMetricDataMsg.setOffsetAheadOfCurrentTime(TimeUnit.MINUTES.toSeconds(2));
        getMetricDataMsg.setLabels(Arrays.asList(label));

        if (CoreGlobalProperty.UNIT_TEST_ON) {
            /* make old case happy */
            bus.makeLocalServiceId(getMetricDataMsg, ZWatchConstants.SERVICE_ID);
            bus.send(getMetricDataMsg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply r) {
                    if (!r.isSuccess()) {
                        completion.fail(r.getError());
                        return;
                    }

                    GetMetricDataReply rly = (GetMetricDataReply) r;
                    List<Datapoint> datapoints = rly.getDatas();
                    if (datapoints == null || datapoints.isEmpty()) {
                        completion.success(Collections.emptyList());
                        return;
                    }

                    completion.success(datapoints.stream().map(Datapoint::getValue).collect(Collectors.toList()));
                }
            });
        } else {
            zWatchManager.getMetricData(getMetricDataMsg, new ReturnValueCompletion<List<Datapoint>>(completion) {
                @Override
                public void success(List<Datapoint> returnValue) {
                    completion.success(returnValue.stream().map(Datapoint::getValue).collect(Collectors.toList()));
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    completion.fail(errorCode);
                }
            });
        }
    }

    /**
     * @param completion
     *   return value:
     *   - If query Prometheus success, return the newest value;
     *   - If Prometheus return empty results, return defaultValue;
     *   - If query Prometheus fail, completion.fail() will be invoked.
     */
    protected void queryGuestToolsInfoFromPrometheus(String vmUuid, String metricName, Double defaultValue, ReturnValueCompletion<Double> completion) {
        getMetricDataList(vmUuid, metricName, new ReturnValueCompletion<List<Double>>(completion) {
            @Override
            public void success(List<Double> list) {
                if (list.isEmpty()) {
                    completion.success(defaultValue);
                } else {
                    completion.success(list.get(list.size() - 1));
                }
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    protected void getVmGuestToolsInfoFromHost(String vmUuid, String hostUuid, ReturnValueCompletion<GetVmGuestToolsInfoRsp> completion) {
        GetVmGuestToolsInfoCmd cmd = new GetVmGuestToolsInfoCmd();
        cmd.setVmInstanceUuid(vmUuid);
        cmd.setPlatform(this.getGuestToolsAgentType().toImagePlatform().name());

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(hostUuid);
        msg.setCommand(cmd);
        msg.setPath(GET_VM_GUEST_TOOLS_INFO_PATH);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.error(String.format("failed to get guest tools info from vm[uuid:%s]: %s", vmUuid, reply.getError()));
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply rly = reply.castReply();
                GetVmGuestToolsInfoRsp rsp = rly.toResponse(GetVmGuestToolsInfoRsp.class);
                if (!rsp.isSuccess()) {
                    logger.error(String.format("failed to get guest tools info from vm[uuid:%s]: %s", vmUuid, rsp.getError()));
                    completion.fail(err(FAILED_TO_GET_STATUS, "failed to get guest tools info from vm[uuid:%s], because:%s", vmUuid, rsp.getError()));
                    return;
                }

                logger.info(String.format("successfully get guest tools info from vm[uuid:%s]", vmUuid));
                completion.success(rsp);
            }
        });
    }

    protected QueryGuestToolsInfoContext createContext(String vmUuid) {
        return new QueryGuestToolsInfoContext(vmUuid, this.getGuestToolsAgentType().toImagePlatform(),
                this::queryGuestToolsInfoFromPrometheus);
    }
    
    protected void emitAfterQueryGuestToolsInfoExtension(QueryGuestToolsInfoContext context, NoErrorCompletion completion) {
        List<AfterQueryGuestToolsInfoExtensionPoint> extensionPoints =
                pluginRgty.getExtensionList(AfterQueryGuestToolsInfoExtensionPoint.class);
        afterQueryGuestToolsInfo(extensionPoints.iterator(), context, completion);
    }

    private void afterQueryGuestToolsInfo(
            final Iterator<AfterQueryGuestToolsInfoExtensionPoint> it,
            QueryGuestToolsInfoContext context,
            final NoErrorCompletion completion) {
        if (!it.hasNext()) {
            completion.done();
            return;
        }

        AfterQueryGuestToolsInfoExtensionPoint ext = it.next();
        ext.afterQueryGuestToolsInfo(context, new Completion(completion) {
            @Override
            public void success() {
                afterQueryGuestToolsInfo(it, context, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("fail to execute AfterQueryGuestToolsInfoExtensionPoint %s because %s",
                        ext.getClass().getSimpleName(), errorCode.getDetails()));
                afterQueryGuestToolsInfo(it, context, completion); // NO FAIL
            }
        });
    }

    @Override
    public void beforeVmMigrate(VmInstanceInventory vm, Completion completion) {
        // detach guest tools iso from vm before vm live migration
        DetachGuestToolsIsoFromVmMsg detachMsg = new DetachGuestToolsIsoFromVmMsg();
        detachMsg.setVmInstanceUuid(vm.getUuid());
        bus.makeTargetServiceIdByResourceUuid(detachMsg, GuestToolsConstant.SERVICE_ID, vm.getUuid());
        bus.send(detachMsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(err(DETACHED_ERROR,
                            "failed to detach guest tools in vm[uuid:%s]", vm.getUuid())
                            .withCause(reply.getError()));
                    return;
                }
                completion.success();
            }
        });
    }

    @Override
    public void getVmMetricsRoutingStatus(VmInstanceInventory vm, Set<String> checkItems, ReturnValueCompletion<Map<String, String>> completion) {
        GetVmMetricsRoutingStatusCmd cmd = new GetVmMetricsRoutingStatusCmd();
        cmd.setVmInstanceUuid(vm.getUuid());
        cmd.setItems(checkItems);

        new KvmCommandSender(vm.getHostUuid())
                .send(cmd, GET_VM_METRICS_ROUTING_STATUS_PATH,
                wrapper -> {
                    GetVmMetricsRoutingStatusRsp rsp = wrapper.getResponse(GetVmMetricsRoutingStatusRsp.class);
                    return rsp.isSuccess() ? null : operr("%s", rsp.getError());
                },
                new ReturnValueCompletion<KvmResponseWrapper>(completion) {
                    @Override
                    public void success(KvmResponseWrapper wrapper) {
                        GetVmMetricsRoutingStatusRsp rsp = wrapper.getResponse(GetVmMetricsRoutingStatusRsp.class);
                        completion.success(rsp.getValues());
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
    }

    @Override
    public void downloadGuestToolsIsoToHost(HostInventory host, String version, Completion completion) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            completion.success();
            return;
        }

        KVMHostVO kvm = dbf.findByUuid(host.getUuid(), KVMHostVO.class);
        if (kvm == null) {
            completion.fail(Platform.err(INSTALLATION_ERROR, "failed to download guest tools iso because no kvm host[uuid:%s] found", host.getUuid()));
            return;
        }

        String srcPath = this.getSrcGuestToolsIso(host.getArchitecture(), host.getHypervisorType(), version);
        String dstPath = this.getDstGuestToolsIso();

        SshFileMd5Checker checker = new SshFileMd5Checker();
        checker.setTargetIp(kvm.getManagementIp());
        checker.setUsername(kvm.getUsername());
        checker.setPassword(kvm.getPassword());
        checker.setSshPort(kvm.getPort());
        checker.addSrcDestPair(srcPath, dstPath);

        AnsibleRunner runner = new AnsibleRunner();
        runner.installChecker(checker);
        runner.setPlayBookName(GuestToolsConstant.ANSIBLE_PLAYBOOK_NAME);
        runner.setTargetIp(kvm.getManagementIp());
        runner.setUsername(kvm.getUsername());
        runner.setPassword(kvm.getPassword());
        runner.setSshPort(kvm.getPort());
        runner.putArgument("src_guest_tools_iso", srcPath);
        runner.putArgument("dst_guest_tools_iso", dstPath);

        runner.run(new ReturnValueCompletion<Boolean>(completion) {
            @Override
            public void success(Boolean downloaded) {
                logger.info(String.format("successfully downloaded %s to kvm host[uuid:%s]", srcPath, host.getUuid()));
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.error(String.format("failed to download %s to kvm host[uuid:%s]", srcPath, host.getUuid()));
                completion.fail(errorCode);
            }
        });
    }


    @Override
    public void attachGuestToolsIsoToVm(VmInstanceInventory vm, Completion completion) {
        List<Tuple> tuples = Q.New(VmCdRomVO.class)
                .eq(VmCdRomVO_.vmInstanceUuid, vm.getUuid())
                .select(VmCdRomVO_.deviceId, VmCdRomVO_.occupant)
                .orderBy(VmCdRomVO_.deviceId, SimpleQuery.Od.ASC)
                .listTuple();
        if (tuples.isEmpty()) {
            completion.fail(err(INSTALLATION_ERROR,
                    "no available cdrom device for vm[uuid:%s] to attach guest-tools", vm.getUuid()));
            return;
        }

        Integer deviceId = tuples.stream()
                .filter(t -> VmInstanceConstant.VM_CDROM_OCCUPANT_GUEST_TOOLS.equals(t.get(1)))
                .findFirst()
                .map(tuple -> tuple.get(0, Integer.class))
                .orElse(null);
        if (deviceId != null) {
            logger.debug(String.format(
                    "vm[uuid:%s] attach guest-tools on cdrom[deviceId:%d]: The guest tools is already attached",
                    vm.getUuid(), deviceId));
        }

        if (deviceId == null) {
            deviceId = tuples.stream()
                    .filter(t -> t.get(1) == null)
                    .findFirst()
                    .map(tuple -> tuple.get(0, Integer.class))
                    .orElse(null);
            if (deviceId != null) {
                logger.debug(String.format(
                        "vm[uuid:%s] attach guest-tools on cdrom[deviceId:%d]: The guest tools is attached to empty cdrom",
                        vm.getUuid(), deviceId));
            }
        }

        if (deviceId == null) {
            deviceId = 0;
            logger.debug(String.format(
                    "no available cdrom device for vm[uuid:%s] to attach guest-tools, use cdrom[deviceId:0]. The ISO on this cdrom will be detached",
                    vm.getUuid()));
        }

        GuestToolsKvmCommands.AttachGuestToolsIsoToVmCmd cmd = new GuestToolsKvmCommands.AttachGuestToolsIsoToVmCmd();
        ImagePlatform platform = this.getGuestToolsAgentType().toImagePlatform();
        cmd.setVmInstanceUuid(vm.getUuid());
        /* only windows vm need temp disk */
        if (platform == ImagePlatform.Windows || platform == ImagePlatform.WindowsVirtio) {
            cmd.setNeedTempDisk(!VmSystemTags.VIRTIO.hasTag(vm.getUuid()));
        }
        cmd.setPlatform(platform.name());
        cmd.setCdromDeviceId(deviceId);

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(vm.getHostUuid());
        msg.setCommand(cmd);
        msg.setPath(GuestToolsConstant.ATTACH_GUEST_TOOLS_ISO_TO_VM_PATH);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, vm.getHostUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.error(String.format("failed to attach guest tools iso to vm[uuid:%s]: %s", vm.getUuid(), reply.getError()));
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply rly = reply.castReply();
                GuestToolsKvmCommands.AttachGuestToolsIsoToVmRsp rsp = rly.toResponse(GuestToolsKvmCommands.AttachGuestToolsIsoToVmRsp.class);
                if (!rsp.isSuccess()) {
                    logger.error(String.format("failed to attach guest tools iso to vm[uuid:%s]: %s", vm.getUuid(), rsp.getError()));
                    completion.fail(operr("failed to attach guest tools iso to vm[uuid:%s], because:%s", vm.getUuid(), rsp.getError()));
                    return;
                }

                logger.info(String.format("successfully attached guest tools iso to vm[uuid:%s]", vm.getUuid()));
                markGuestToolsInstalled();
                completion.success();
            }

            void markGuestToolsInstalled() {
                final String vmUuid = vm.getUuid();
                SystemTagCreator creator = MevocoSystemTags.GUEST_TOOLS_HAS_ATTACHED.newSystemTagCreator(vmUuid);
                creator.inherent = false;
                creator.recreate = true;
                creator.create();

                SQL.New(VmCdRomVO.class)
                        .eq(VmCdRomVO_.vmInstanceUuid, vmUuid)
                        .eq(VmCdRomVO_.deviceId, cmd.getCdromDeviceId())
                        .set(VmCdRomVO_.occupant, VmInstanceConstant.VM_CDROM_OCCUPANT_GUEST_TOOLS)
                        .set(VmCdRomVO_.isoUuid, null)
                        .set(VmCdRomVO_.isoInstallPath, null)
                        .update();
            }
        });
    }

    @Override
    public void detachGuestToolsIsoFromVm(VmInstanceInventory vm, Completion completion) {
        GuestToolsKvmCommands.DetachGuestToolsIsoFromVmCmd cmd = new GuestToolsKvmCommands.DetachGuestToolsIsoFromVmCmd();
        cmd.setVmInstanceUuid(vm.getUuid());
        cmd.setPlatform(this.getGuestToolsAgentType().toImagePlatform().name());

        String hostUuid = vm.getHostUuid() != null ? vm.getHostUuid(): vm.getLastHostUuid();
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(hostUuid);
        msg.setCommand(cmd);
        msg.setPath(GuestToolsConstant.DETACH_GUEST_TOOLS_ISO_FROM_VM_PATH);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.error(String.format("failed to detach guest tools iso from vm[uuid:%s]: %s", vm.getUuid(), reply.getError()));
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply rly = reply.castReply();
                GuestToolsKvmCommands.DetachGuestToolsIsoFromVmRsp rsp = rly.toResponse(GuestToolsKvmCommands.DetachGuestToolsIsoFromVmRsp.class);
                if (!rsp.isSuccess()) {
                    logger.error(String.format("failed to detach guest tools iso from vm[uuid:%s]: %s", vm.getUuid(), rsp.getError()));
                    completion.fail(operr("failed to detach guest tools iso from vm[uuid:%s], because:%s", vm.getUuid(), rsp.getError()));
                    return;
                }

                SQL.New(VmCdRomVO.class)
                        .eq(VmCdRomVO_.vmInstanceUuid, vm.getUuid())
                        .eq(VmCdRomVO_.occupant, VmInstanceConstant.VM_CDROM_OCCUPANT_GUEST_TOOLS)
                        .set(VmCdRomVO_.occupant, null)
                        .set(VmCdRomVO_.isoUuid, null)
                        .set(VmCdRomVO_.isoInstallPath, null)
                        .update();

                logger.info(String.format("successfully detached guest tools iso from vm[uuid:%s]", vm.getUuid()));
                completion.success();
            }
        });
    }

    protected class CheckGuestToolsStatusFlowBuilder {
        QueryGuestToolsInfoContext context;
        String name;
        String metricName;
        Function<GuestToolsAgentStatus, Double> statusChecker;
        Function<GuestToolsAgentStatus, List<Double>> statusCheckerForList;
        Function<GuestToolsVersion, Double> parseVersionFunction;
        boolean skipIfMetricIsEmpty = false;
        boolean skipWhenQueryFail = false;

        public CheckGuestToolsStatusFlowBuilder context(QueryGuestToolsInfoContext context) {
            this.context = context;
            return this;
        }

        public CheckGuestToolsStatusFlowBuilder name(String name) {
            this.name = name;
            return this;
        }

        public CheckGuestToolsStatusFlowBuilder prometheusMetric(String metricName) {
            this.metricName = metricName;
            return this;
        }

        public CheckGuestToolsStatusFlowBuilder checkStatusByLastMetricValue(Function<GuestToolsAgentStatus, Double> aliveFunction) {
            this.statusChecker = aliveFunction;
            return this;
        }

        public CheckGuestToolsStatusFlowBuilder checkStatusByLastMetricValues(Function<GuestToolsAgentStatus, List<Double>> aliveFunction) {
            this.statusCheckerForList = aliveFunction;
            return this;
        }

        public CheckGuestToolsStatusFlowBuilder version(Function<GuestToolsVersion, Double> parseVersionFunction) {
            this.parseVersionFunction = parseVersionFunction;
            return this;
        }

        public CheckGuestToolsStatusFlowBuilder skipIfMetricIsEmpty() {
            this.skipIfMetricIsEmpty = true;
            return this;
        }

        public CheckGuestToolsStatusFlowBuilder skipWhenQueryFail() {
            this.skipWhenQueryFail = true;
            return this;
        }

        @SuppressWarnings("rawtypes")
        public NoRollbackFlow create() {
            if (metricName == null) {
                throw new CloudRuntimeException("fail to create CheckGuestToolsStatusFlow: metric name is not defined");
            }
            if (statusCheckerForList == null && statusChecker == null) {
                statusChecker = value -> null;
            }
            if (parseVersionFunction == null) {
                parseVersionFunction = value -> null;
            }

            return new NoRollbackFlow() {
                String __name__ = name;

                @Override
                public boolean skip(Map data) {
                    return context.getStatus() != null && context.getVersion() != null;
                }

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    getMetricDataList(context.getVmUuid(), metricName, new ReturnValueCompletion<List<Double>>(trigger) {
                        @Override
                        public void success(List<Double> values) {
                            if (statusCheckerForList != null) {
                                onReceiveValues(values, trigger);
                                return;
                            }
                            onReceiveValue(CollectionUtils.isEmpty(values) ? null : values.get(0), trigger);
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            if (skipWhenQueryFail) {
                                logger.debug(String.format("failed to get guest tools state from prometheus but skip anyway: [metric=%s, error=%s]",
                                        metricName, errorCode));
                                trigger.next();
                                return;
                            }

                            trigger.fail(operr("failed to get guest tools state from prometheus: [metric=%s]", metricName)
                                    .withCause(errorCode));
                        }
                    });
                }

                private void onReceiveValue(Double returnValue, FlowTrigger trigger) {
                    if (skipIfMetricIsEmpty && returnValue == null) {
                        trigger.next();
                        return;
                    }

                    if (context.getStatus() == null) {
                        context.setStatus(statusChecker.call(returnValue));
                    }
                    if (context.getVersion() == null) {
                        context.setVersion(parseVersionFunction.call(returnValue));
                    }
                    trigger.next();
                }

                private void onReceiveValues(List<Double> returnValues, FlowTrigger trigger) {
                    if (skipIfMetricIsEmpty && CollectionUtils.isEmpty(returnValues)) {
                        trigger.next();
                        return;
                    }

                    if (context.getStatus() == null) {
                        context.setStatus(statusCheckerForList.call(returnValues));
                    }
                    if (context.getVersion() == null && !CollectionUtils.isEmpty(returnValues)) {
                        context.setVersion(parseVersionFunction.call(returnValues.get(0)));
                    }
                    trigger.next();
                }
            };
        }
    }
}
