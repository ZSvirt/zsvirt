package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.host.HostGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.allocator.HostCpuOverProvisioningManager;
import org.zstack.header.host.RecalculateHostCapacityMsg;
import org.zstack.header.zone.ZoneVO;
import org.zstack.header.zone.ZoneVO_;

import javax.persistence.Query;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.zstack.utils.CollectionUtils.transform;

/**
 * Created by xing5 on 2016/5/12.
 */
public class HostCpuOverProvisioningManagerImpl implements HostCpuOverProvisioningManager {
    private Integer globalRatio;
    private ConcurrentHashMap<String, Integer> ratios = new ConcurrentHashMap<String, Integer>();

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ResourceConfigFacade rcf;

    @Override
    public void setGlobalRatio(int ratio) {
        globalRatio = ratio;

        updateHostsCpuCapacity(ratio);
        recalculateAllHostCapacity();
    }

    private void recalculateAllHostCapacity() {
        SimpleQuery<ZoneVO> q = dbf.createQuery(ZoneVO.class);
        q.select(ZoneVO_.uuid);
        List<String> zuuids = q.listValue();
        if (zuuids.isEmpty()) {
            return;
        }

        List<RecalculateHostCapacityMsg> rmsgs = transform(zuuids, arg -> {
            RecalculateHostCapacityMsg msg = new RecalculateHostCapacityMsg();
            msg.setZoneUuid(arg);
            bus.makeLocalServiceId(msg, HostAllocatorConstant.SERVICE_ID);
            return msg;
        });

        bus.send(rmsgs);
    }

    @Transactional
    private void updateHostsCpuCapacity(int ratio) {
        if (ratios.isEmpty()) {
            // all hosts use global ratio
            String sql = String.format("update HostCapacityVO cap set cap.totalCpu = cap.cpuNum * %s", ratio);
            Query q = dbf.getEntityManager().createQuery(sql);
            q.executeUpdate();
        } else {
            // part of hosts use global ratio
            String sql = String.format("update HostCapacityVO cap set cap.totalCpu = cap.cpuNum * %s where cap.uuid not in (:uuids)", ratio);
            Query q = dbf.getEntityManager().createQuery(sql);
            q.setParameter("uuids", ratios.keySet());
            q.executeUpdate();
        }
    }

    @Override
    public int getGlobalRatio() {
        return globalRatio == null ? HostGlobalConfig.HOST_CPU_OVER_PROVISIONING_RATIO.value(Integer.class) : globalRatio;
    }

    @Override
    public void setRatio(String hostUuid, int ratio) {
        ratios.put(hostUuid, ratio);
        updateHostCpuCapacityByUuid(hostUuid, ratio);
        recalculateHostCapacityByUuid(hostUuid);
    }

    @Transactional
    private void updateHostCpuCapacityByUuid(String hostUuid, int ratio) {
        String sql = String.format("update HostCapacityVO cap set cap.totalCpu = cap.cpuNum * %s where cap.uuid = :huuid", ratio);
        Query q = dbf.getEntityManager().createQuery(sql);
        q.setParameter("huuid", hostUuid);
        q.executeUpdate();
    }

    @Override
    public void deleteRatio(String hostUuid) {
        ratios.remove(hostUuid);
        updateHostCpuCapacityByUuid(hostUuid, getGlobalRatio());
        recalculateHostCapacityByUuid(hostUuid);
    }

    private void recalculateHostCapacityByUuid(String hostUuid) {
        RecalculateHostCapacityMsg msg = new RecalculateHostCapacityMsg();
        msg.setHostUuid(hostUuid);
        bus.makeLocalServiceId(msg, HostAllocatorConstant.SERVICE_ID);
        bus.send(msg);
    }

    @Override
    public int getRatio(String hostUuid) {
        Integer r = ratios.get(hostUuid);
        // TODO: init from db, not get from db every time.
        return r == null ? rcf.getResourceConfigValue(HostGlobalConfig.HOST_CPU_OVER_PROVISIONING_RATIO, hostUuid, Integer.class) : r;
    }

    @Override
    public Map<String, Integer> getAllRatio() {
        return ratios;
    }

    @Override
    public int calculateByRatio(String hostUuid, int cpuNum) {
        int r = getRatio(hostUuid);
        int ret = Math.round((float)cpuNum / r);
        return ret == 0 ? 1 : ret;
    }

    @Override
    public int calculateHostCpuByRatio(String hostUuid, int cpuNum) {
        int r = getRatio(hostUuid);
        return cpuNum * r;
    }
}
