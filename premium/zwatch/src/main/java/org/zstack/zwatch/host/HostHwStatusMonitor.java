package org.zstack.zwatch.host;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.host.HostGlobalConfig;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.cloudbus.*;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.db.UpdateQuery;
import org.zstack.core.thread.AsyncTimer;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.kvm.KVMConstant;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.zwatch.ZWatchManager;
import org.zstack.zwatch.api.GetMetricDataMsg;
import org.zstack.zwatch.datatype.Datapoint;
import org.zstack.zwatch.datatype.Label;
import org.zstack.zwatch.datatype.Namespace;
import org.zstack.zwatch.namespace.HostNamespace;

import javax.persistence.Tuple;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/5/28 11:49
 */
public class HostHwStatusMonitor implements ManagementNodeChangeListener, ManagementNodeReadyExtensionPoint,
        Component {
    private final static CLogger logger = Utils.getLogger(HostHwStatusMonitor.class);
    private Map<String, Tracker> trackers = new ConcurrentHashMap<>();

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ResourceDestinationMaker destMaker;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ZWatchManager zWatchManager;
    @Autowired
    protected EventFacade evtf;


    private void syncHostHwMonitor(String hostUuid, Completion completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("host-%s-hw-monitor-track", hostUuid));
        chain.then(new ShareFlow() {
            HwMonitorStatus cpuStatus = HwMonitorStatus.Unknown;
            HwMonitorStatus memoryStatus = HwMonitorStatus.Unknown;
            HwMonitorStatus diskStatus = HwMonitorStatus.Unknown;
            HwMonitorStatus nicStatus = HwMonitorStatus.Unknown;
            HwMonitorStatus gpuStatus = HwMonitorStatus.Unknown;
            HwMonitorStatus powerSupplyStatus = HwMonitorStatus.Unknown;
            HwMonitorStatus fanStatus = HwMonitorStatus.Unknown;
            HwMonitorStatus raidStatus = HwMonitorStatus.Unknown;
            HwMonitorStatus temperatureStatus = HwMonitorStatus.Unknown;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = String.format("sync-host[%s]-cpu-status", hostUuid);
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        syncHostCpuStatus(hostUuid, new ReturnValueCompletion<Boolean>(trigger) {
                            @Override
                            public void success(Boolean returnValue) {
                                if (returnValue == null) {
                                    trigger.next();
                                    return;
                                }

                                cpuStatus = returnValue ? HwMonitorStatus.Normal : HwMonitorStatus.Error;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });
                flow(new NoRollbackFlow() {
                    String __name__ = String.format("sync-host[%s]-memory-status", hostUuid);
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        syncHostMemoryStatus(hostUuid, new ReturnValueCompletion<Boolean>(trigger) {

                            @Override
                            public void success(Boolean returnValue) {
                                if (returnValue == null) {
                                    trigger.next();
                                    return;
                                }
                                memoryStatus = returnValue ? HwMonitorStatus.Normal : HwMonitorStatus.Error;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });
                flow(new NoRollbackFlow() {
                    String __name__ = String.format("sync-host[%s]-disk-status", hostUuid);
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        syncHostDiskStatus(hostUuid, new ReturnValueCompletion<Boolean>(trigger) {

                            @Override
                            public void success(Boolean returnValue) {
                                if (returnValue == null) {
                                    trigger.next();
                                    return;
                                }
                                diskStatus = returnValue ? HwMonitorStatus.Normal : HwMonitorStatus.Error;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });
               /* flow(new NoRollbackFlow() {
                    String __name__ = String.format("sync-host[%s]-nic-status", hostUuid);
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        syncHostNetworkStatus(hostUuid, new ReturnValueCompletion<Boolean>(trigger) {

                            @Override
                            public void success(Boolean returnValue) {
                                if (returnValue == null) {
                                    trigger.next();
                                    return;
                                }
                                nicStatus = returnValue;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });*/
                flow(new NoRollbackFlow() {
                    String __name__ = String.format("sync-host[%s]-gpu-status", hostUuid);
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        syncHostGpuStatus(hostUuid, new ReturnValueCompletion<Boolean>(trigger) {

                            @Override
                            public void success(Boolean returnValue) {
                                if (returnValue == null) {
                                    trigger.next();
                                    return;
                                }
                                gpuStatus = returnValue ? HwMonitorStatus.Normal : HwMonitorStatus.Error;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });
                flow(new NoRollbackFlow() {
                    String __name__ = String.format("sync-host[%s]-power-supply-status", hostUuid);
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        syncHostPowerSupplyStatus(hostUuid, new ReturnValueCompletion<Boolean>(trigger) {

                            @Override
                            public void success(Boolean returnValue) {
                                if (returnValue == null) {
                                    trigger.next();
                                    return;
                                }
                                powerSupplyStatus = returnValue ? HwMonitorStatus.Normal : HwMonitorStatus.Error;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });
                flow(new NoRollbackFlow() {

                    String __name__ = String.format("sync-host[%s]-fan-status", hostUuid);

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        syncFanStatus(hostUuid, new ReturnValueCompletion<Boolean>(trigger) {

                            @Override
                            public void success(Boolean returnValue) {
                                if (returnValue == null) {
                                    trigger.next();
                                    return;
                                }
                                fanStatus = returnValue ? HwMonitorStatus.Normal : HwMonitorStatus.Error;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });
                flow(new NoRollbackFlow() {
                    String __name__ = String.format("sync-host[%s]-raid-status", hostUuid);
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        syncRaidStatus(hostUuid, new ReturnValueCompletion<Boolean>(trigger) {

                            @Override
                            public void success(Boolean returnValue) {
                                if (returnValue == null) {
                                    trigger.next();
                                    return;
                                }
                                raidStatus = returnValue ? HwMonitorStatus.Normal : HwMonitorStatus.Error;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });
                flow(new NoRollbackFlow() {
                    String __name__ = String.format("sync-host[%s]-temperature-status", hostUuid);
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        syncHostTemperatureStatus(hostUuid, new ReturnValueCompletion<Boolean>(trigger) {

                            @Override
                            public void success(Boolean returnValue) {
                                if (returnValue == null) {
                                    trigger.next();
                                    return;
                                }
                                temperatureStatus = returnValue ? HwMonitorStatus.Normal : HwMonitorStatus.Error;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });
                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        boolean update = false;
                        HostHwMonitorStatusVO vo = Q.New(HostHwMonitorStatusVO.class)
                                .eq(HostHwMonitorStatusVO_.uuid, hostUuid)
                                .find();
                        if (vo == null) {
                            vo = new HostHwMonitorStatusVO();
                            vo.setUuid(hostUuid);
                            vo.setCpuStatus(cpuStatus );
                            vo.setDiskStatus(diskStatus);
                            vo.setMemoryStatus(memoryStatus);
                            vo.setPowerSupplyStatus(powerSupplyStatus);
                            vo.setRaidStatus(raidStatus);
                            vo.setTemperatureStatus(temperatureStatus);
                            vo.setFanStatus(fanStatus);
                            vo.setGpuStatus(gpuStatus);
                            vo.setNicStatus(nicStatus);
                            dbf.persist(vo);
                            completion.success();
                            return;
                        }

                        if (cpuStatus != null && vo.getCpuStatus() != cpuStatus) {
                            vo.setCpuStatus(cpuStatus);
                            update = true;
                        }
                        if (memoryStatus != null && vo.getMemoryStatus() != memoryStatus) {
                            vo.setMemoryStatus(memoryStatus);
                            update = true;
                        }
                        if (diskStatus != null && vo.getDiskStatus() != diskStatus) {
                            vo.setDiskStatus(diskStatus);
                            update = true;
                        }
                        if (powerSupplyStatus != null && vo.getPowerSupplyStatus() != powerSupplyStatus) {
                            vo.setPowerSupplyStatus(powerSupplyStatus);
                            update = true;
                        }

                        if (raidStatus != null && vo.getRaidStatus() != raidStatus) {
                            vo.setRaidStatus(raidStatus);
                            update = true;
                        }
                        if (temperatureStatus != null && vo.getTemperatureStatus() != temperatureStatus) {
                            vo.setTemperatureStatus(temperatureStatus);
                            update = true;
                        }
                        if (fanStatus != null && vo.getFanStatus() != fanStatus) {
                            vo.setFanStatus(fanStatus);
                            update = true;
                        }
                        if (gpuStatus != null && vo.getGpuStatus() != gpuStatus) {
                            vo.setGpuStatus(gpuStatus);
                            update = true;
                        }
                        /*if (nicStatus != null && vo.getNicStatus() != nicStatus) {
                            vo.setNicStatus(nicStatus);
                            update = true;
                        }*/

                        if (update) {
                            dbf.update(vo);
                        }
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        logger.warn(String.format("failed to sync host[uuid:%s] hw monitor, %s", hostUuid, errCode.getDetails()));
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }


    private void syncHostTemperatureStatus(String hostUuid, ReturnValueCompletion<Boolean> completion) {
        getMetricDataList(Namespace.zstackNamespaceName(HostNamespace.NAME), HostNamespace.IPMITemperatureState.getName(),
                hostUuid, new ReturnValueCompletion<List<Double>>(null) {
                    @Override
                    public void success(List<Double> returnValue) {
                        if (returnValue == null || returnValue.isEmpty()) {
                            completion.success(null);
                            return;
                        }
                        completion.success(!returnValue.stream().anyMatch(value -> value != 0.0));
                    }
                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.debug(String.format("failed to sync host[uuid:%s] temperature status, error: %s",
                                hostUuid, errorCode.getDetails()));
                        completion.success(null);
                    }
                });
    }

    private void syncRaidStatus(String hostUuid, ReturnValueCompletion<Boolean> completion) {
        getMetricDataList(Namespace.zstackNamespaceName(HostNamespace.NAME), HostNamespace.RaidState.getName(),
                hostUuid, new ReturnValueCompletion<List<Double>>(null) {
                    @Override
                    public void success(List<Double> returnValue) {
                        if (returnValue == null || returnValue.isEmpty()) {
                            completion.success(null);
                            return;
                        }

                        completion.success(!returnValue.stream().anyMatch(value -> value == 100.0));
                    }
                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.debug(String.format("failed to sync host[uuid:%s] raid status, error: %s",
                                hostUuid, errorCode.getDetails()));
                        completion.success(null);
                    }
                });
    }

    private void syncFanStatus(String hostUuid, ReturnValueCompletion<Boolean> completion) {
        getMetricDataList(Namespace.zstackNamespaceName(HostNamespace.NAME), HostNamespace.IPMIFanSpeedState.getName(),
                hostUuid, new ReturnValueCompletion<List<Double>>(null) {
                    @Override
                    public void success(List<Double> returnValue) {
                        if (returnValue == null || returnValue.isEmpty()) {
                            completion.success(null);
                            return;
                        }

                        completion.success(!returnValue.stream().anyMatch(value -> value != 0.0));
                    }
                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.debug(String.format("failed to sync host[uuid:%s] fan status, error: %s",
                                hostUuid, errorCode.getDetails()));
                        completion.success(null);
                    }
                });
    }

    private void syncHostPowerSupplyStatus(String hostUuid, ReturnValueCompletion<Boolean> completion) {
        getMetricDataList(Namespace.zstackNamespaceName(HostNamespace.NAME), HostNamespace.IPMIPowerSupply.getName(),
                hostUuid, new ReturnValueCompletion<List<Double>>(null) {
                    @Override
                    public void success(List<Double> returnValue) {
                        if (returnValue == null || returnValue.isEmpty()) {
                            completion.success(null);
                            return;
                        }
                        completion.success(!returnValue.stream().anyMatch(value -> value != 0.0));
                    }
                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.debug(String.format("failed to sync host[uuid:%s] power supply status, error: %s",
                                hostUuid, errorCode.getDetails()));
                        completion.success(null);
                    }
                });
    }

    private void syncHostGpuStatus(String hostUuid, ReturnValueCompletion<Boolean> completion) {
        getMetricDataList(Namespace.zstackNamespaceName(HostNamespace.NAME), HostNamespace.GpuStatus.getName(),
                hostUuid, new ReturnValueCompletion<List<Double>>(null) {
                    @Override
                    public void success(List<Double> returnValue) {
                        if (returnValue == null || returnValue.isEmpty()) {
                            completion.success(null);
                            return;
                        }
                        completion.success(returnValue.stream().anyMatch(value -> value != 0.0));
                    }
                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.debug(String.format("failed to sync host[uuid:%s] gpu status, error: %s",
                                hostUuid, errorCode.getDetails()));
                        completion.success(null);
                    }
                });
    }

    private void syncHostNetworkStatus(String hostUuid, ReturnValueCompletion<Boolean> completion) {
        getMetricDataList(Namespace.zstackNamespaceName(HostNamespace.NAME), HostNamespace.PhysicalNetworkInterface.getName(),
                hostUuid, new ReturnValueCompletion<List<Double>>(null) {
                    @Override
                    public void success(List<Double> returnValue) {
                        if (returnValue == null || returnValue.isEmpty()) {
                            completion.success(null);
                            return;
                        }

                        completion.success(!returnValue.stream().anyMatch(value -> value != 0.0));
                    }
                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.debug(String.format("failed to sync host[uuid:%s] network status, error: %s",
                                hostUuid, errorCode.getDetails()));
                        completion.success(null);
                    }
                });
    }

    private void syncHostDiskStatus(String hostUuid, ReturnValueCompletion<Boolean> completion) {
        getMetricDataList(Namespace.zstackNamespaceName(HostNamespace.NAME), HostNamespace.PhysicalDiskState.getName(),
                hostUuid, new ReturnValueCompletion<List<Double>>(null) {
                    @Override
                    public void success(List<Double> returnValue) {
                        if (returnValue == null || returnValue.isEmpty()) {
                            completion.success(null);
                            return;
                        }
                        completion.success(!returnValue.stream().anyMatch(value -> value != 0.0));
                    }
                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.debug(String.format("failed to sync host[uuid:%s] disk status, error: %s",
                                hostUuid, errorCode.getDetails()));
                        completion.success(null);
                    }
                });
    }

    private void syncHostMemoryStatus(String hostUuid, ReturnValueCompletion<Boolean> completion) {
        getMetricDataList(Namespace.zstackNamespaceName(HostNamespace.NAME), HostNamespace.IPMIPhysicalMemoryStatus.getName(),
                hostUuid, new ReturnValueCompletion<List<Double>>(null) {
                    @Override
                    public void success(List<Double> returnValue) {
                        if (returnValue == null || returnValue.isEmpty()) {
                            completion.success(null);
                            return;
                        }

                        completion.success(!returnValue.stream().anyMatch(value -> value != 0.0));
                    }
                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.debug(String.format("failed to sync host[uuid:%s] memory status, error: %s",
                                hostUuid, errorCode.getDetails()));
                        completion.success(null);
                    }
                });
    }

    private void syncHostCpuStatus(String hostUuid, ReturnValueCompletion<Boolean> completion) {
        getMetricDataList(Namespace.zstackNamespaceName(HostNamespace.NAME), HostNamespace.CpuStatus.getName(), hostUuid,
                new ReturnValueCompletion<List<Double>>(null) {
                    @Override
                    public void success(List<Double> returnValue) {
                        if (returnValue == null || returnValue.isEmpty()) {
                            completion.success(null);
                            return;
                        }

                        completion.success(!returnValue.stream().anyMatch(value -> value == 10.0));
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.debug(String.format("failed to sync host[uuid:%s] cpu status, error: %s",
                                hostUuid, errorCode.getDetails()));

                        completion.success(null);
                    }
                });
    }



    private void getMetricDataList(String nameSpace, String metricName, String hostUuid,
                                   ReturnValueCompletion<List<Double>> completion) {
        String label = String.format("%s%s%s", HostNamespace.LabelNames.HostUuid,Label.Operator.Equal, hostUuid);
        List<String> labels = new ArrayList<>();
        labels.add(label);
        GetMetricDataMsg msg = new GetMetricDataMsg();
        msg.setNamespace(nameSpace);
        msg.setMetricName(metricName);
        msg.setOffsetAheadOfCurrentTime(1L);
        msg.setLabels(labels);
        zWatchManager.getMetricData(msg, new ReturnValueCompletion<List<Datapoint>>(completion) {
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

    private class Tracker extends AsyncTimer {
        private final CLogger logger = Utils.getLogger(HostHwStatusMonitor.class);

        private String uuid;
        private String hypervisorType;
        public Tracker(String uuid) {
            super(TimeUnit.SECONDS, HostGlobalConfig.SYNC_HOST_HW_MONITOR_INTERVAL.value(Long.class));
            this.uuid = uuid;

            hypervisorType = Q.New(HostVO.class).select(HostVO_.hypervisorType)
                    .eq(HostVO_.uuid, uuid).findValue();
            if (hypervisorType == null) {
                throw new CloudRuntimeException(String.format("host[uuid:%s] is deleted, why you submit a host hw monitor tracker for it???", uuid));
            }

            if (!KVMConstant.KVM_HYPERVISOR_TYPE.equals(hypervisorType)) {
                throw new CloudRuntimeException(String.format("host[uuid:%s] is not a kvm host, why you submit a host hw monitor tracker for it???", uuid));
            }

            __name__ = String.format("host-hw-monitor-tracker-%s", uuid);
        }

        @Override
        protected void execute() {
            track();
        }

        private void track() {
            Tuple t = Q.New(HostVO.class).select(HostVO_.state, HostVO_.status)
                    .eq(HostVO_.uuid, uuid).findTuple();

            if (t == null) {
                logger.debug(String.format("host[uuid:%s] seems to be deleted, stop sync host hw monitor tracking it", uuid));
                return;
            }

            HostState state = t.get(0, HostState.class);

            if (state == HostState.PreMaintenance || state == HostState.Maintenance) {
                logger.debug(String.format("host[uuid:%s] is in state of %s, not sync host hw monitor tracking it this time", uuid, state));
                continueToRunThisTimer();
                return;
            }

            syncHostHwMonitor(uuid, new Completion(null) {
                @Override
                public void success() {
                    continueToRunThisTimer();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    continueToRunThisTimer();
                }
            });
        }
    }


    private void onHostStatusChange() {
        evtf.on(HostCanonicalEvents.HOST_STATUS_CHANGED_PATH, new EventCallback() {

            @Override
            protected void run(Map tokens, Object data) {
                HostCanonicalEvents.HostStatusChangedData d = (HostCanonicalEvents.HostStatusChangedData) data;
                if (HostStatus.Connected.toString().equals(d.getNewStatus())) {

                    if (!destMaker.isManagedByUs(d.getHostUuid())) {
                        return;
                    }
                    trackHost(d.getHostUuid());
                }
            }
        });

        evtf.on(HostCanonicalEvents.HOST_DELETED_PATH, new EventCallback() {

            @Override
            protected void run(Map tokens, Object data) {
                HostCanonicalEvents.HostDeletedData d = (HostCanonicalEvents.HostDeletedData) data;
                if (!destMaker.isManagedByUs(d.getHostUuid())) {
                    return;
                }
                untrackHost(d.getHostUuid());
            }
        });
    }

    private void reScanHost() {
        reScanHost(false);
    }

    private void reScanHost(boolean skipExisting) {
        if (!skipExisting) {
            new HashSet<>(trackers.values()).forEach(HostHwStatusMonitor.Tracker::cancel);
        }

        new SQLBatch() {
            @Override
            protected void scripts() {
                long count = sql("select count(h) from HostVO h where h.hypervisorType = :hypervisorType", Long.class)
                        .param("hypervisorType", KVMConstant.KVM_HYPERVISOR_TYPE)
                        .find();
                sql("select h.uuid from HostVO h where h.hypervisorType = :hypervisorType", String.class)
                        .param("hypervisorType", KVMConstant.KVM_HYPERVISOR_TYPE).limit(1000)
                        .paginate(count, (List<String> hostUuids) -> {
                    List<String> byUs = hostUuids.stream().filter(huuid -> {
                        if (skipExisting) {
                            return destMaker.isManagedByUs(huuid) && !trackers.containsKey(huuid);
                        } else {
                            return destMaker.isManagedByUs(huuid);
                        }
                    }).collect(Collectors.toList());

                    trackHost(byUs);
                });
            }
        }.execute();
    }


    @Override
    public boolean start() {
        onHostStatusChange();

        HostGlobalConfig.SYNC_HOST_HW_MONITOR_INTERVAL.installUpdateExtension((oldConfig, newConfig) -> {
            logger.debug(String.format("%s change from %s to %s, restart host hw monitor trackers",
                    oldConfig.getCanonicalName(), oldConfig.value(), newConfig.value()));
            reScanHost();
        });

        HostGlobalConfig.AUTO_RECONNECT_ON_ERROR.installUpdateExtension((oc, nc)-> {
            if (nc.value(Boolean.class)) {
                logger.debug(String.format("%s change from %s to %s, restart host trackers",
                        oc.getCanonicalName(), oc.value(), nc.value()));
                reScanHost(true);
            }
        });

        evtf.on(HostCanonicalEvents.HOST_PHYSICAL_CPU_STATUS_ABNORMAL, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                HostCanonicalEvents.HostPhysicalCpuStatusAbnormalData cdata = (HostCanonicalEvents.HostPhysicalCpuStatusAbnormalData) data;
                UpdateQuery.New(HostHwMonitorStatusVO.class)
                        .eq(HostHwMonitorStatusVO_.uuid, cdata.getHostUuid())
                        .set(HostHwMonitorStatusVO_.cpuStatus, HwMonitorStatus.Error)
                        .update();
            }
        });

        evtf.on(HostCanonicalEvents.HOST_PHYSICAL_MEMORY_STATUS_ABNORMAL, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                HostCanonicalEvents.HostPhysicalMemoryStatusAbnormalData cdata = (HostCanonicalEvents.HostPhysicalMemoryStatusAbnormalData) data;
                UpdateQuery.New(HostHwMonitorStatusVO.class)
                        .eq(HostHwMonitorStatusVO_.uuid, cdata.getHostUuid())
                        .set(HostHwMonitorStatusVO_.memoryStatus, HwMonitorStatus.Error)
                        .update();
            }
        });

        evtf.on(HostCanonicalEvents.HOST_PHYSICAL_DISK_STATUS_ABNORMAL, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                HostCanonicalEvents.HostPhysicalDiskStatusAbnormalData cdata = (HostCanonicalEvents.HostPhysicalDiskStatusAbnormalData) data;
                UpdateQuery.New(HostHwMonitorStatusVO.class)
                        .eq(HostHwMonitorStatusVO_.uuid, cdata.getHostUuid())
                        .set(HostHwMonitorStatusVO_.diskStatus, HwMonitorStatus.Error)
                        .update();
            }
        });

        evtf.on(HostCanonicalEvents.HOST_PHYSICAL_GPU_STATUS_ABNORMAL, new EventCallback() {

            @Override
            protected void run(Map tokens, Object data) {
                HostCanonicalEvents.HostPhysicalGpuStatusAbnormalData cdata = (HostCanonicalEvents.HostPhysicalGpuStatusAbnormalData) data;
                UpdateQuery.New(HostHwMonitorStatusVO.class)
                        .eq(HostHwMonitorStatusVO_.uuid, cdata.getHostUuid())
                        .set(HostHwMonitorStatusVO_.gpuStatus, HwMonitorStatus.Error)
                        .update();
            }
        });

        evtf.on(HostCanonicalEvents.HOST_PHYSICAL_POWER_SUPPLY_STATUS_ABNORMAL, new EventCallback() {

            @Override
            protected void run(Map tokens, Object data) {
                HostCanonicalEvents.HostPhysicalPowerSupplyStatusAbnormalData cdata = (HostCanonicalEvents.HostPhysicalPowerSupplyStatusAbnormalData) data;
                UpdateQuery.New(HostHwMonitorStatusVO.class)
                        .eq(HostHwMonitorStatusVO_.uuid, cdata.getHostUuid())
                        .set(HostHwMonitorStatusVO_.powerSupplyStatus, HwMonitorStatus.Error)
                        .update();
            }
        });

        evtf.on(HostCanonicalEvents.HOST_PHYSICAL_FAN_STATUS_ABNORMAL, new EventCallback() {

            @Override
            protected void run(Map tokens, Object data) {
                HostCanonicalEvents.HostPhysicalFanStatusAbnormalData cdata = (HostCanonicalEvents.HostPhysicalFanStatusAbnormalData) data;
                UpdateQuery.New(HostHwMonitorStatusVO.class)
                        .eq(HostHwMonitorStatusVO_.uuid, cdata.getHostUuid())
                        .set(HostHwMonitorStatusVO_.fanStatus, HwMonitorStatus.Error)
                        .update();
            }
        });

        evtf.on(HostCanonicalEvents.HOST_PHYSICAL_RAID_STATUS_ABNORMAL, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                HostCanonicalEvents.HostPhysicalRaidStatusAbnormalData cdata = (HostCanonicalEvents.HostPhysicalRaidStatusAbnormalData) data;
                UpdateQuery.New(HostHwMonitorStatusVO.class)
                        .eq(HostHwMonitorStatusVO_.uuid, cdata.getHostUuid())
                        .set(HostHwMonitorStatusVO_.raidStatus, HwMonitorStatus.Error)
                        .update();
            }
        });

        evtf.on(HostCanonicalEvents.HOST_PHYSICAL_NIC_STATUS_DOWN, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                HostCanonicalEvents.HostPhysicalNicStatusData cdata = (HostCanonicalEvents.HostPhysicalNicStatusData)data;
                UpdateQuery.New(HostHwMonitorStatusVO.class)
                        .eq(HostHwMonitorStatusVO_.uuid, cdata.getHostUuid())
                        .set(HostHwMonitorStatusVO_.nicStatus, HwMonitorStatus.Error)
                        .update();
            }
        });

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    public void trackHost(String hostUuid) {
        Tracker t = trackers.get(hostUuid);
        if (t != null) {
            t.cancel();
        }

        t = new HostHwStatusMonitor.Tracker(hostUuid);
        trackers.put(hostUuid, t);

        if (CoreGlobalProperty.UNIT_TEST_ON) {
            t.start();
        } else {
            t.startRightNow();
        }

        logger.debug(String.format("starting tracking hosts[uuid:%s] hw monitor", hostUuid));
    }

    public void untrackHost(String hostUuid) {
        Tracker t = trackers.get(hostUuid);
        if (t != null) {
            t.cancel();
        }
        trackers.remove(hostUuid);
        logger.debug(String.format("stop tracking hosts[uuid:%s] hw monitor", hostUuid));
    }

    public void trackHost(Collection<String> hostUuids) {
        hostUuids.forEach(this::trackHost);
    }

    public void untrackHost(Collection<String> hostUuids) {
        hostUuids.forEach(this::untrackHost);
    }

    @Override
    public void nodeJoin(ManagementNodeInventory inv) {
        reScanHost();
    }

    @Override
    public void nodeLeft(ManagementNodeInventory inv) {
        reScanHost();
    }

    @Override
    public void iAmDead(ManagementNodeInventory inv) {

    }

    @Override
    public void iJoin(ManagementNodeInventory inv) {

    }

    @Override
    public void managementNodeReady() {
        reScanHost();
    }
}
