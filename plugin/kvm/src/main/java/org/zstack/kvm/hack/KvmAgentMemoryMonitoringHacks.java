package org.zstack.kvm.hack;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.timeout.TimeHelper;
import org.zstack.header.Component;
import org.zstack.header.host.HostCanonicalEvents;
import org.zstack.header.host.HostConstant;
import org.zstack.header.rest.RESTFacade;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KvmPingCommandExtensionPoint;
import org.zstack.kvm.RestartKvmAgentMsg;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.zstack.kvm.KVMConstant.*;
import static org.zstack.kvm.KVMGlobalConfig.*;

/**
 * There is a bug report indicating that
 * kvmagent may crash due to excessive memory usage.
 *
 * Here is a temporary solution:
 * continuously monitor the memory usage of the kvmagent
 * and restart the service when it exceeds the threshold.
 *
 * This is a hack. See ZSTAC-71000 and ZSV-8178 for details.
 */
public class KvmAgentMemoryMonitoringHacks implements Component,
        KvmPingCommandExtensionPoint {
    private static final CLogger logger = Utils.getLogger(KvmAgentMemoryMonitoringHacks.class);

    /**
     * The factor to calculate the busy status duration of the host.
     * Original name: skipHostPingTimeWhenKvmagentBusy
     */
    public int busyHostDurationFactor = 300;
    public final Map<String, Object> kvmAgentConfigs = Collections.synchronizedMap(new HashMap<>());

    @Autowired
    private RESTFacade restFacade;
    @Autowired
    private EventFacade eventFacade;
    @Autowired
    private CloudBus bus;
    @Autowired
    private TimeHelper timeHelper;

    @Override
    public boolean start() {
        registerGlobalConfigHooks();
        registerEvents();
        return true;
    }

    private void processKvmAgentPhysicalMemUsageAbnormal(KVMAgentCommands.HostProcessPhysicalMemoryUsageAlarmCmd cmd) {
        if (cmd.getMemoryUsage() <= KVMAGENT_PHYSICAL_MEMORY_USAGE_HARD_LIMIT.value(Long.class)) {
            return;
        }

        logger.debug("The zstack-kvmagent service has exceeded the hard limit for physical memory usage, " +
                "and we will try restart it later");
        RestartKvmAgentMsg restartKvmAgentMsg = new RestartKvmAgentMsg();
        restartKvmAgentMsg.setHostUuid(cmd.getHostUuid());
        bus.makeTargetServiceIdByResourceUuid(restartKvmAgentMsg, HostConstant.SERVICE_ID, restartKvmAgentMsg.getHostUuid());
        bus.send(restartKvmAgentMsg);
    }

    private void registerGlobalConfigHooks() {
        kvmAgentConfigs.put(KVMAGENT_PHYSICAL_MEMORY_USAGE_ALARM_THRESHOLD.getName(), KVMAGENT_PHYSICAL_MEMORY_USAGE_ALARM_THRESHOLD.value(Long.class));
        kvmAgentConfigs.put(KVMAGENT_PHYSICAL_MEMORY_USAGE_HARD_LIMIT.getName(), KVMAGENT_PHYSICAL_MEMORY_USAGE_HARD_LIMIT.value(Long.class));

        KVMAGENT_PHYSICAL_MEMORY_USAGE_ALARM_THRESHOLD.installUpdateExtension(
                (oldConfig, newConfig) -> kvmAgentConfigs.put(newConfig.getName(), newConfig.value(Long.class)));

        KVMAGENT_PHYSICAL_MEMORY_USAGE_HARD_LIMIT.installUpdateExtension(
                (oldConfig, newConfig) -> kvmAgentConfigs.put(newConfig.getName(), newConfig.value(Long.class)));
    }

    private void registerEvents() {
        restFacade.registerSyncHttpCallHandler(HOST_PROCESS_PHYSICAL_MEMORY_USAGE_ALARM_PATH, KVMAgentCommands.HostProcessPhysicalMemoryUsageAlarmCmd.class, cmd -> {
            HostCanonicalEvents.HostProcessPhysicalMemoryUsageAlarmData data = new HostCanonicalEvents.HostProcessPhysicalMemoryUsageAlarmData();
            data.setHostUuid(cmd.getHostUuid());
            data.setPid(cmd.getPid());
            data.setMemoryUsage(String.format("%s MB", cmd.getMemoryUsage() / 1048576));
            data.setProcessName(cmd.getProcessName());
            eventFacade.fire(HostCanonicalEvents.HOST_PROCESS_PHYSICAL_MEMORY_USAGE_ABNORMAL, data);

            switch (data.getProcessName()) {
                case "zstack-kvmagent":
                    processKvmAgentPhysicalMemUsageAbnormal(cmd);
                    break;
                default:
                    logger.debug(String.format("unknown process name[%s] in host[uuid:%s]", cmd.getProcessName(), cmd.getHostUuid()));
            }

            return null;
        });

        restFacade.registerSyncHttpCallHandler(HOST_KVMAGENT_STATUS_PATH, KVMAgentCommands.HostKvmAgentStatusCmd.class, cmd -> {
            if (KVM_AGENT_STATUS_BUSY.equals(cmd.getStatus())) {
                HostCanonicalEvents.HostPingSkipData data = new HostCanonicalEvents.HostPingSkipData();
                data.setHostUuid(cmd.getHostUuid());
                long durationInSeconds = busyHostDurationFactor * (int)(cmd.getMemoryUsage() / SizeUnit.GIGABYTE.toByte(4) + 1);
                data.setBusyUntil(timeHelper.getCurrentTimeMillis() + durationInSeconds * 1000);
                eventFacade.fire(HostCanonicalEvents.HOST_STATUS_BUSY, data);
            } else if (KVM_AGENT_STATUS_AVAILABLE.equals(cmd.getStatus())) {
                HostCanonicalEvents.HostPingSkipData data = new HostCanonicalEvents.HostPingSkipData();
                data.setHostUuid(cmd.getHostUuid());
                eventFacade.fire(HostCanonicalEvents.HOST_STATUS_RESUME_FROM_BUSY, data);
            } else {
                logger.debug("unknown kvmagent status: " + cmd.getStatus());
            }
            return null;
        });
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void beforeKvmPing(KVMAgentCommands.PingCmd command) {
        command.configs = new HashMap<>(kvmAgentConfigs);
    }
}
