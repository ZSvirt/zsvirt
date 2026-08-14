package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.Component;
import org.zstack.header.allocator.*;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.*;
import java.util.stream.Collectors;

/**
 */
public class HostCapacityReserveManagerImpl implements HostCapacityReserveManager, Component {
    private static final CLogger logger = Utils.getLogger(HostCapacityReserveManagerImpl.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private HostCapacityOverProvisioningManager ratioMgr;

    private Map<String, HostReservedCapacityExtensionPoint> exts = new HashMap<>();

    private void populateExtensions() {
        for (HostReservedCapacityExtensionPoint extp : pluginRgty.getExtensionList(HostReservedCapacityExtensionPoint.class)) {
            HostReservedCapacityExtensionPoint ext = exts.get(extp.getHypervisorTypeForHostReserveCapacityExtension());
            if (ext != null) {
                throw new CloudRuntimeException(String.format("duplicate HostReserveCapacityExtensionPoint[%s, %s] for hypervisor type[%s]",
                        extp.getClass().getName(), ext.getClass().getName(), extp.getHypervisorTypeForHostReserveCapacityExtension()));
            }

            exts.put(extp.getHypervisorTypeForHostReserveCapacityExtension(), extp);
        }
    }

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private class ReservedCapacityFinder {
        List<String> hostUuids;
        Map<String, ReservedHostCapacity> result = new HashMap<>();

        private void findReservedCapacityByHypervisorType() {
            SimpleQuery<HostVO> hq = dbf.createQuery(HostVO.class);
            hq.select(HostVO_.uuid, HostVO_.hypervisorType);
            hq.add(HostVO_.uuid, Op.IN, hostUuids);
            hq.add(HostVO_.state,Op.EQ, HostState.Enabled);
            hq.add(HostVO_.status,Op.EQ, HostStatus.Connected);
            List<Tuple> tuples = hq.listTuple();

            tuples.parallelStream().forEach(t -> {
                String huuid = t.get(0, String.class);
                String hvType = t.get(1, String.class);

                HostReservedCapacityExtensionPoint ext = exts.get(hvType);
                if (ext == null) {
                    return;
                }

                ReservedHostCapacity hc = result.get(huuid);
                ReservedHostCapacity extHc = ext.getReservedHostCapacity(huuid);
                if (hc.getReservedMemoryCapacity() == -1) {
                    hc.setReservedMemoryCapacity(extHc.getReservedMemoryCapacity());
                }
                if (hc.getReservedCpuCapacity() == -1) {
                    hc.setReservedCpuCapacity(extHc.getReservedCpuCapacity());
                }
            });
        }

        private void squeeze() {
            result.entrySet()
                    .stream()
                    .filter(e -> e.getValue().getReservedCpuCapacity() != -1 && e.getValue().getReservedMemoryCapacity() != -1)
                    .forEach(e -> hostUuids.remove(e.getKey()));
        }

        private void done() {
            for (ReservedHostCapacity hc : result.values()) {
                if (hc.getReservedCpuCapacity() == -1) {
                    hc.setReservedCpuCapacity(0);
                }
                if (hc.getReservedMemoryCapacity() == -1) {
                    hc.setReservedMemoryCapacity(0);
                }
            }
        }

        Map<String, ReservedHostCapacity> find() {
            if (hostUuids.isEmpty()) {
                return result;
            }

            for (String huuid : hostUuids) {
                ReservedHostCapacity hc = new ReservedHostCapacity();
                hc.setReservedCpuCapacity(-1);
                hc.setReservedMemoryCapacity(-1);
                result.put(huuid, hc);
            }

            findReservedCapacityByHypervisorType();
            done();
            return result;
        }
    }

    @Override
    public List<HostVO> filterOutHostsByReservedCapacity(List<HostVO> candidates, long requiredCpu, long requiredMemory) {
        ReservedCapacityFinder finder = new ReservedCapacityFinder();
        finder.hostUuids = candidates.stream().map(HostVO::getUuid).collect(Collectors.toList());

        Map<String, ReservedHostCapacity> reserves = finder.find();
        return candidates.parallelStream()
                .filter(hvo -> {
                    ReservedHostCapacity hc = reserves.get(hvo.getUuid());
                    if (requiredMemory == 0 || hvo.getCapacity().getAvailableMemory() - hc.getReservedMemoryCapacity() >= ratioMgr.calculateMemoryByRatio(hvo.getUuid(), requiredMemory)) {
                        return true;
                    } else {
                        if (logger.isTraceEnabled()) {
                            if (hvo.getCapacity().getAvailableMemory() - hc.getReservedMemoryCapacity() < requiredMemory) {
                                logger.trace(String.format("remove host[uuid:%s] from candidates;because after subtracting reserved memory[%s bytes]," +
                                                " it cannot provide required memory[%s bytes]",
                                        hvo.getUuid(), hc.getReservedMemoryCapacity(), requiredMemory));
                            }

                            if (hvo.getCapacity().getAvailableCpu() - hc.getReservedCpuCapacity() < requiredCpu) {
                                logger.trace(String.format("remove host[uuid:%s] from candidates;because after subtracting reserved cpu[%s]," +
                                                " it cannot provide required cpu[%s]",
                                        hvo.getUuid(), hc.getReservedCpuCapacity(), requiredCpu));
                            }
                        }
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    @Override
    public ReservedHostCapacity getReservedHostCapacityByZones(List<String> zoneUuids, String hypervisorType) {
        ReservedHostCapacity ret = new ReservedHostCapacity();
        ret.setReservedCpuCapacity(0);
        ret.setReservedMemoryCapacity(0);

        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
        q.select(HostVO_.uuid);
        q.add(HostVO_.zoneUuid, Op.IN, zoneUuids);
        if (hypervisorType != null) {
            q.add(HostVO_.hypervisorType, Op.EQ, hypervisorType);
        }
        List<String> huuids = q.listValue();
        if (huuids.isEmpty()) {
            return ret;
        }

        ReservedCapacityFinder finder = new ReservedCapacityFinder();
        finder.hostUuids = huuids;
        Collection<ReservedHostCapacity> col = finder.find().values();
        for (ReservedHostCapacity rc : col) {
            ret.setReservedMemoryCapacity(ret.getReservedMemoryCapacity() + rc.getReservedMemoryCapacity());
            ret.setReservedCpuCapacity(ret.getReservedCpuCapacity() + rc.getReservedCpuCapacity());
        }

        return ret;
    }

    @Override
    public ReservedHostCapacity getReservedHostCapacityByClusters(List<String> clusterUuids, String hypervisorType) {
        ReservedHostCapacity ret = new ReservedHostCapacity();
        ret.setReservedCpuCapacity(0);
        ret.setReservedMemoryCapacity(0);

        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
        q.select(HostVO_.uuid);
        q.add(HostVO_.clusterUuid, Op.IN, clusterUuids);
        if (hypervisorType != null) {
            q.add(HostVO_.hypervisorType, Op.EQ, hypervisorType);
        }
        List<String> huuids = q.listValue();
        if (huuids.isEmpty()) {
            return ret;
        }

        ReservedCapacityFinder finder = new ReservedCapacityFinder();
        finder.hostUuids = huuids;
        Collection<ReservedHostCapacity> col = finder.find().values();
        for (ReservedHostCapacity rc : col) {
            ret.setReservedMemoryCapacity(ret.getReservedMemoryCapacity() + rc.getReservedMemoryCapacity());
            ret.setReservedCpuCapacity(ret.getReservedCpuCapacity() + rc.getReservedCpuCapacity());
        }

        return ret;
    }

    @Override
    public ReservedHostCapacity getReservedHostCapacityByHosts(List<String> hostUuids) {
        ReservedCapacityFinder finder = new ReservedCapacityFinder();
        finder.hostUuids = hostUuids;
        return finder.find().values().iterator().next();
    }

    @Override
    public void reserveCapacity(final String hostUuid, final long requestCpu, final long requestMemory, boolean skipCheck) {
        if (skipCheck) {
            updateCapacityWithoutChecking(hostUuid, requestCpu, requestMemory);
        } else {
            reserveCapacityWithChecking(hostUuid, requestCpu, requestMemory);
        }
    }

    private void reserveCapacityWithChecking(String hostUuid, long requestCpu, long requestMemory) {
        HostCapacityUpdater updater = new HostCapacityUpdater(hostUuid);
        HostVO host = dbf.findByUuid(hostUuid, HostVO.class);
        HostReservedCapacityExtensionPoint ext = exts.get(host.getHypervisorType());

        ReservedHostCapacity ret = new ReservedHostCapacity();
        if (ext != null) {
            ReservedHostCapacity extHc = ext.getReservedHostCapacity(host.getUuid());
            ret.setReservedMemoryCapacity(extHc.getReservedMemoryCapacity());
            ret.setReservedCpuCapacity(extHc.getReservedCpuCapacity());
        } else {
            ret.setReservedCpuCapacity(0);
            ret.setReservedMemoryCapacity(0);
        }

        updater.run(cap -> {
            long availCpu = cap.getAvailableCpu() - requestCpu;
            if (requestCpu != 0 && availCpu < 0) {
                throw new UnableToReserveHostCapacityException(
                        String.format("no enough CPU[%s] on the host[uuid:%s]", requestCpu, hostUuid));
            }

            cap.setAvailableCpu(availCpu);

            long availMemory = cap.getAvailableMemory() - ratioMgr.calculateMemoryByRatio(hostUuid, requestMemory);
            if (requestMemory != 0 && availMemory - ret.getReservedMemoryCapacity() < 0) {
                throw new UnableToReserveHostCapacityException(
                        String.format("no enough memory[%s] on the host[uuid:%s]", requestMemory, hostUuid));
            }

            cap.setAvailableMemory(availMemory);

            return cap;
        });
    }

    private void updateCapacityWithoutChecking(String hostUuid, long cpuNum, long memorySize) {
        HostCapacityUpdater updater = new HostCapacityUpdater(hostUuid);
        updater.run(cap -> {
            long availCpu = cap.getAvailableCpu() - cpuNum;
            cap.setAvailableCpu(availCpu);
            long availMemory = cap.getAvailableMemory() - ratioMgr.calculateMemoryByRatio(hostUuid, memorySize);
            cap.setAvailableMemory(availMemory);
            return cap;
        });
    }
}
