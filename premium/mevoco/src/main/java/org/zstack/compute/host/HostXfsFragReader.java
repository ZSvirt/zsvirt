package org.zstack.compute.host;

import com.google.common.collect.Maps;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.*;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.MessageReply;
import org.zstack.header.volume.VolumeDeletionExtensionPoint;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.kvm.*;
import org.zstack.mevoco.MevocoGlobalConfig;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @ Author : yh.w
 * @ Date   : Created in 15:04 2019/5/15
 */
public class HostXfsFragReader implements Component, ManagementNodeReadyExtensionPoint, HostDeleteExtensionPoint, VolumeDeletionExtensionPoint {
    private static final CLogger logger = Utils.getLogger(HostXfsFragReader.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    protected EventFacade evtf;

    private Future xfsFragScrapePeriodTask;
    private static Map<String, Map<String, String>> volumePathMap = Maps.newHashMap();
    private static Map<String, String> hostXfsFragMap = Maps.newConcurrentMap();
    private static Map<String, String> volumeXfsFragMap = Maps.newConcurrentMap();
    private static Map<String, String> hostTempMap = Maps.newConcurrentMap();
    private static Map<String, String> volumeTempMap = Maps.newConcurrentMap();

    public static final String HOST_XFS_SCRAPE_PATH = "/host/xfs/scrape";
    public static final String XFS_TYPE = "xfs";

    private int getScrapeInterval() {
        return MevocoGlobalConfig.XFS_FRAG_SCRAPE_INTERVAL.value(Integer.class);
    }

    public static class GetXfsDataCmd extends KVMAgentCommands.AgentCommand {
        public Map<String, String> volumePathMap;
    }

    public static class GetXfsDataRsp extends KVMAgentCommands.AgentResponse {
        public String fsType;
        public String hostFrag;
        public Map<String, String> volumeFragMap;
    }

    @Override
    public boolean start() {
        MevocoGlobalConfig.XFS_FRAG_SCRAPE_INTERVAL.installUpdateExtension((oldConfig, newConfig) -> {
            if (MevocoGlobalConfig.ENABLE_XFS_FRAG_SCRAPE.value(Boolean.class)) {
                startXfsFragScrapePeriodTask();
            }
        });
        MevocoGlobalConfig.ENABLE_XFS_FRAG_SCRAPE.installUpdateExtension((oldConfig, newConfig) -> {
            if (newConfig.value(Boolean.class)) {
                startXfsFragScrapePeriodTask();
            } else if (!newConfig.value(Boolean.class) && xfsFragScrapePeriodTask != null) {
                xfsFragScrapePeriodTask.cancel(true);
            }
        });
        return true;
    }

    public static Map<String, String> getHostXfsFrag() {
        return hostXfsFragMap;
    }

    public static Map<String, String> getVolumeXfsFrag() {
        return volumeXfsFragMap;
    }

    private void updateVolumePath() {
        List<String> hostIps = Q.New(KVMHostVO.class)
                .select(KVMHostVO_.uuid)
                .eq(HostVO_.status, HostStatus.Connected)
                .listValues();
        for (String ip : hostIps) {
            List<Tuple> ts = SQL.New("SELECT v.uuid, v.installPath from VolumeVO v where v.uuid in" +
                    "(SELECT ref.resourceUuid from LocalStorageResourceRefVO ref WHERE ref.hostUuid = :uuid and ref.resourceType = :rtype)" +
                    "AND v.size >= :size AND v.installPath is not null", Tuple.class)
                    .param("uuid", ip)
                    .param("rtype", VolumeVO.class.getSimpleName())
                    .param("size", SizeUnit.GIGABYTE.toByte(MevocoGlobalConfig.XFS_VOLUME_DETECT_SIZE.value(Long.class)))
                    .list();
            volumePathMap.put(ip, ts.stream().collect(Collectors.toMap(t -> t.get(0, String.class), t -> t.get(1, String.class))));
        }
    }

    //scrap host and volume xfs data together
    private void scrapeXfsData() {
        List<KVMHostAsyncHttpCallMsg> msgs = new ArrayList<>();
        for (Map.Entry<String, Map<String, String>> map : volumePathMap.entrySet()) {
            KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
            GetXfsDataCmd cmd = new GetXfsDataCmd();
            cmd.volumePathMap = map.getValue();
            msg.setCommand(cmd);
            msg.setPath(HOST_XFS_SCRAPE_PATH);
            msg.setHostUuid(map.getKey());
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, map.getKey());
            msgs.add(msg);
        }

        new While<>(msgs).step((msg, complet) -> {
            bus.send(msg, new CloudBusCallBack(complet) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(String.format("failed to get xfs frag data on host:%s", msg.getHostUuid()));
                        complet.done();
                        return;
                    }
                    KVMHostAsyncHttpCallReply r = reply.castReply();
                    final GetXfsDataRsp rsp = r.toResponse(GetXfsDataRsp.class);
                    updateXfsFragData(msg.getHostUuid(), rsp);
                    complet.done();
                }
            });
        }, 10).run(new WhileDoneCompletion(null) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                hostXfsFragMap = hostTempMap;
                volumeXfsFragMap = volumeTempMap;
            }
        });
    }

    private void updateXfsFragData(String hostUuid, GetXfsDataRsp rsp) {
        if (StringUtils.isEmpty(rsp.fsType) || !rsp.fsType.equals(XFS_TYPE)) {
            return;
        }

        if (StringUtils.isEmpty(rsp.hostFrag)) {
            return;
        }

        hostTempMap.put(hostUuid, rsp.hostFrag);

        if (MapUtils.isNotEmpty(rsp.volumeFragMap)) {
            volumeTempMap.putAll(rsp.volumeFragMap);
        }
    }

    private void scrapeXfsFragData() {
        clearTempMap();
        updateVolumePath();
        scrapeXfsData();
    }

    private void clearTempMap() {
        volumePathMap.clear();
        hostTempMap.clear();
        volumeTempMap.clear();
    }

    @Override
    public boolean stop() {
        if (xfsFragScrapePeriodTask != null) {
            xfsFragScrapePeriodTask.cancel(true);
        }
        return true;
    }

    private synchronized void startXfsFragScrapePeriodTask() {
        if (xfsFragScrapePeriodTask != null) {
            xfsFragScrapePeriodTask.cancel(true);
        }
        xfsFragScrapePeriodTask = thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.HOURS;
            }

            @Override
            public long getInterval() {
                return getScrapeInterval();
            }

            @Override
            public String getName() {
                return "scrape-host-frag-metric-data-task";
            }

            @Override
            public void run() {
                scrapeXfsFragData();
            }
        });
    }

    @Override
    public void managementNodeReady() {
        if (MevocoGlobalConfig.ENABLE_XFS_FRAG_SCRAPE.value(Boolean.class)) {
            startXfsFragScrapePeriodTask();
        }
    }

    @Override
    public void preDeleteHost(HostInventory inventory) throws HostException {

    }

    @Override
    public void beforeDeleteHost(HostInventory inventory) {

    }

    @Override
    public void afterDeleteHost(HostInventory inventory) {
        hostXfsFragMap.remove(inventory.getUuid());
    }

    @Override
    public void preDeleteVolume(VolumeInventory volume) {

    }

    @Override
    public void beforeDeleteVolume(VolumeInventory volume) {

    }

    @Override
    public void afterDeleteVolume(VolumeInventory volume, Completion completion) {
        volumeXfsFragMap.remove(volume.getUuid());
        completion.success();
    }

    @Override
    public void failedToDeleteVolume(VolumeInventory volume, ErrorCode errorCode) {

    }
}
