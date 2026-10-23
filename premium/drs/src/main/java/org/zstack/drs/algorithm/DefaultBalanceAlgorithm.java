package org.zstack.drs.algorithm;

import org.zstack.drs.DRSGlobalConfig;
import org.zstack.drs.data.HostNode;
import org.zstack.drs.data.MigrateTask;
import org.zstack.drs.data.VmNode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by lining on 2019/12/6.
 */

public class DefaultBalanceAlgorithm implements BalanceAlgorithm {
    private static CLogger logger = Utils.getLogger(DefaultBalanceAlgorithm.class);

    private List<HostNode> highLoadHost;
    private List<HostNode> lowLoadHost;

    private Float cpuUsedPercentThreshold;
    private Float memUsedPercentThreshold;

    private float averageCpuLoadPercent = -1f;
    private float averageMemoryLoadPercent = -1f;

    private List<MigrateTask> tasks = new ArrayList<>();


    private static final float HYSTERESIS = 2.0f; // in percentage points

    private final boolean trace = logger.isTraceEnabled() || DRSGlobalConfig.LOG_TRACE_DRS.value(Boolean.class);

    private void logTrace(String msg) {
        if (trace) {
            logger.debug(msg);
        }
    }


    @Override
    public List<MigrateTask> makeTasks(List<HostNode> hostNodeList, Float cpuUsedPercentThreshold, Float memUsedPercentThreshold) {
        hostNodeList = deepCopy(hostNodeList);

        this.cpuUsedPercentThreshold = cpuUsedPercentThreshold;
        this.memUsedPercentThreshold = memUsedPercentThreshold;

        this.averageCpuLoadPercent = (float) (hostNodeList.stream().mapToDouble(HostNode::getUsedCPUPercent).sum() / hostNodeList.size());
        this.averageMemoryLoadPercent = (float) (hostNodeList.stream().mapToDouble(HostNode::getUsedMemoryPercent).sum() / hostNodeList.size());

        logTrace(String.format("Before scheduling\n" +
                        "average CPU usage: %s\n" +
                        "average memory usage: %s\n" +
                        "the cluster host load: %s",
                this.averageCpuLoadPercent,
                this.averageMemoryLoadPercent,
                JSONObjectUtil.toJsonString(hostNodeList)
        ));

        this.groupHosts(hostNodeList); // highLoadHost, lowLoadHost

        this.initHostMaxSize2Move();

        this.sortHosts();

        this.generateMigrateTasks();

        logTrace(String.format("After scheduling\n" +
                        "the cluster host load: %s\n" +
                        "migrate tasks: %s",
                JSONObjectUtil.toJsonString(hostNodeList),
                JSONObjectUtil.toJsonString(tasks)
        ));

        //printLog(hostNodeList, tasks);
        return tasks;
    }

    private void groupHosts(List<HostNode> hostNodeList) {
        this.highLoadHost = new LinkedList<>();
        this.lowLoadHost = new LinkedList<>();

        for (HostNode hostNode : hostNodeList) {
            if (cpuUsedPercentThreshold != null) {
                if (hostNode.getUsedCPUPercent() > cpuUsedPercentThreshold) {
                    highLoadHost.add(hostNode);
                    continue;
                }
            }

            if (memUsedPercentThreshold != null) {
                float percent = (1f * hostNode.getTotalMemoryBit() - hostNode.getFreePhysicalMemoryBit()) / hostNode.getTotalMemoryBit();
                percent = percent * 100;
                if (percent > memUsedPercentThreshold) {
                    highLoadHost.add(hostNode);
                    continue;
                }
            }

            lowLoadHost.add(hostNode);
        }
    }

    private void sortHosts() {
        //highLoadHost 负载降序排序;
        //lowLoadHost 负载升序排序;
        if (cpuUsedPercentThreshold != null && memUsedPercentThreshold == null) {
            highLoadHost.sort(Comparator.comparingDouble(HostNode::getUsedCPUPercent));
            Collections.reverse(highLoadHost);
            lowLoadHost.sort(Comparator.comparingDouble(HostNode::getUsedCPUPercent));

            for (HostNode hostNode : highLoadHost) {
                hostNode.getVmList().sort(Comparator.comparingDouble(VmNode::getUsedCPUPercent));
                Collections.reverse(hostNode.getVmList());
            }
        } else if (memUsedPercentThreshold != null && cpuUsedPercentThreshold == null) {
            highLoadHost.sort(Comparator.comparingDouble(HostNode::getUsedMemoryPercent));
            Collections.reverse(highLoadHost);
            lowLoadHost.sort(Comparator.comparingDouble(HostNode::getUsedMemoryPercent));

            for (HostNode hostNode : highLoadHost) {
                hostNode.getVmList().sort(Comparator.comparingDouble(VmNode::getUsedMemoryPercent));
                Collections.reverse(hostNode.getVmList());
            }
        } else if (cpuUsedPercentThreshold != null && memUsedPercentThreshold != null) {
            highLoadHost.sort((HostNode h1, HostNode h2) -> {
                float h1Load = (1f * h1.getUsedCPUPercent() / cpuUsedPercentThreshold) + (1f * h1.getUsedMemoryPercent() / memUsedPercentThreshold);
                float h2Load = (1f * h2.getUsedCPUPercent() / cpuUsedPercentThreshold) + (1f * h2.getUsedMemoryPercent() / memUsedPercentThreshold);
                return (h1Load < h2Load) ? -1 : ((h1Load == h2Load) ? 0 : 1);
            });
            Collections.reverse(highLoadHost);
            lowLoadHost.sort((HostNode h1, HostNode h2) -> {
                float h1Load = (1f * h1.getUsedCPUPercent() / cpuUsedPercentThreshold) + (1f * h1.getUsedMemoryPercent() / memUsedPercentThreshold);
                float h2Load = (1f * h2.getUsedCPUPercent() / cpuUsedPercentThreshold) + (1f * h2.getUsedMemoryPercent() / memUsedPercentThreshold);
                return (h1Load < h2Load) ? -1 : ((h1Load == h2Load) ? 0 : 1);
            });

            for (HostNode hostNode : highLoadHost) {
                hostNode.getVmList().sort((VmNode v1, VmNode v2) -> {
                    float h1Load = (1f * v1.getUsedCPUPercent() / cpuUsedPercentThreshold) + (1f * v1.getUsedMemoryPercent() / memUsedPercentThreshold);
                    float h2Load = (1f * v2.getUsedCPUPercent() / cpuUsedPercentThreshold) + (1f * v2.getUsedMemoryPercent() / memUsedPercentThreshold);
                    return (h1Load < h2Load) ? -1 : ((h1Load == h2Load) ? 0 : 1);
                });
                Collections.reverse(hostNode.getVmList());
            }
        }
    }

    private void initHostMaxSize2Move() {
        List<HostNode> allHosts = new ArrayList<>();
        allHosts.addAll(highLoadHost);
        allHosts.addAll(lowLoadHost);
        for (HostNode hostNode : allHosts) {
            if (cpuUsedPercentThreshold != null) {
                hostNode.setMaxSize2MoveCPUPercent(Math.abs(averageCpuLoadPercent - hostNode.getUsedCPUPercent()));
            }

            if (memUsedPercentThreshold != null) {
                long bit = (long) (hostNode.getTotalMemoryBit() * (averageMemoryLoadPercent / 100f)
                        - (hostNode.getTotalMemoryBit() - hostNode.getFreePhysicalMemoryBit()));
                hostNode.setMaxSize2MoveMemoryBit(Math.abs(bit));
            }
        }
    }

    private HostNode findTargetHost(List<HostNode> targetHosts) {
        for (HostNode targetHost : targetHosts) {
            if (cpuUsedPercentThreshold != null && memUsedPercentThreshold == null) {
                if (targetHost.getMaxSize2MoveCPUPercent() > 0) {
                    return targetHost;
                }
            } else if (memUsedPercentThreshold != null && cpuUsedPercentThreshold == null) {
                if (targetHost.getMaxSize2MoveMemoryBit() > 0) {
                    return targetHost;
                }
            } else {
                if (targetHost.getMaxSize2MoveCPUPercent() > 0 && targetHost.getMaxSize2MoveMemoryBit() > 0) {
                    return targetHost;
                }
            }
        }

        return null;
    }

    private void generateMigrateTasks() {
        scheduleFireFighting();

        logTrace(String.format("After fire fighting, the cluster host load: %s\n" +
                        "migrate tasks size: %d",
                JSONObjectUtil.toJsonString(highLoadHost),
                tasks.size())
        );
        logTrace("start balance scheduling");

        boolean flag = true;
        while (flag) {
            int taskSize = tasks.size();
            for(final Iterator<HostNode> i = highLoadHost.iterator(); i.hasNext();) {
                final HostNode sourceHost = i.next();

                HostNode targetHost = this.findTargetHost(lowLoadHost);
                if (targetHost == null) {
                    break;
                }

                this.generateMigrateTasks(sourceHost, targetHost);

                if (cpuUsedPercentThreshold != null && sourceHost.getMaxSize2MoveCPUPercent() == 0) {
                    i.remove();
                } else if (memUsedPercentThreshold != null && sourceHost.getMaxSize2MoveMemoryBit() == 0) {
                    i.remove();
                }
            }

            if (tasks.size() == taskSize) {
                flag = false;
            }
        }
    }

    private void generateMigrateTasks(HostNode sourceHost, HostNode targetHost) {
        for (VmNode vmNode : new ArrayList<>(sourceHost.getVmList())) {
            if (cpuUsedPercentThreshold != null) {
                if (vmNode.getUsedCPUPercent() > sourceHost.getMaxSize2MoveCPUPercent()) {
                    continue;
                }
                if (vmNode.getUsedCPUPercent() > targetHost.getMaxSize2MoveCPUPercent()) {
                    continue;
                }
            }

            if (memUsedPercentThreshold != null) {
                if (vmNode.getUsedPhysicalMemoryBit() > sourceHost.getMaxSize2MoveMemoryBit()) {
                    continue;
                }
                if (vmNode.getUsedPhysicalMemoryBit() > targetHost.getMaxSize2MoveMemoryBit()) {
                    continue;
                }
            }

            if (targetHost.getHostCapacityAvailableCpuNum() < vmNode.getCpuNum() || !targetHost.hasMemoryCapacity(vmNode.getMemorySize())) {
                continue;
            }

            generateMigrateTask(sourceHost, targetHost, vmNode);
        }

        // check if source host is still high load
    }

    private void generateMigrateTask(HostNode sourceHost, HostNode targetHost, VmNode vmNode) {
        logTrace(String.format("Generate migrate task, source host[uuid: %s], target host[uuid: %s], VM[uuid: %s]",
                sourceHost.getUuid(), targetHost.getUuid(), vmNode.uuid));

        MigrateTask task = new MigrateTask();
        task.sourceHostUuid = sourceHost.getUuid();
        task.targetHostUuid = targetHost.getUuid();
        task.vmUuid = vmNode.uuid;
        tasks.add(task);

        targetHost.setHostCapacityAvailableCpuNum(targetHost.getHostCapacityAvailableCpuNum() - vmNode.getCpuNum());
        targetHost.reserveMemoryCapacity(vmNode.getMemorySize());
        targetHost.setFreePhysicalMemoryBit(targetHost.getFreePhysicalMemoryBit() - vmNode.getUsedPhysicalMemoryBit());
        targetHost.setUsedCPUPercent(targetHost.getUsedCPUPercent() + vmCpuPctOn(targetHost, vmNode));

        sourceHost.setHostCapacityAvailableCpuNum(sourceHost.getHostCapacityAvailableCpuNum() + vmNode.getCpuNum());
        sourceHost.releaseMemoryCapacity(vmNode.getMemorySize());
        sourceHost.setFreePhysicalMemoryBit(sourceHost.getFreePhysicalMemoryBit() + vmNode.getUsedPhysicalMemoryBit());
        sourceHost.setUsedCPUPercent(sourceHost.getUsedCPUPercent() - vmCpuPctOn(sourceHost, vmNode));

        if (cpuUsedPercentThreshold != null) {
            targetHost.setMaxSize2MoveCPUPercent(targetHost.getMaxSize2MoveCPUPercent() - vmCpuPctOn(targetHost, vmNode));
            sourceHost.setMaxSize2MoveCPUPercent(sourceHost.getMaxSize2MoveCPUPercent() - vmCpuPctOn(sourceHost, vmNode));
        }

        if (memUsedPercentThreshold != null) {
            targetHost.setMaxSize2MoveMemoryBit(targetHost.getMaxSize2MoveMemoryBit() - vmNode.getUsedPhysicalMemoryBit());
            sourceHost.setMaxSize2MoveMemoryBit(sourceHost.getMaxSize2MoveMemoryBit() - vmNode.getUsedPhysicalMemoryBit());
        }

        targetHost.getVmList().add(vmNode);
        sourceHost.getVmList().remove(vmNode);
    }

    private <T> List<T> deepCopy(List<T> src) {
        try {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(byteOut);
            out.writeObject(src);

            ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
            ObjectInputStream in = new ObjectInputStream(byteIn);
            @SuppressWarnings("unchecked")
            List<T> dest = (List<T>) in.readObject();
            return dest;
        } catch (IOException | ClassNotFoundException e) {
            throw new CloudRuntimeException(e.getMessage());
        }
    }

    @Override
    public String getAlgorithmName() {
        return DefaultBalanceAlgorithm.class.getSimpleName();
    }

    private void printLog(List<HostNode> hostNodes, List<MigrateTask> tasks) {
        System.out.println("Migrate vm list:");
        for (MigrateTask task : tasks) {
            System.out.println(String.format(" %s(%s) -> %s",
                    task.vmUuid, task.sourceHostUuid, task.targetHostUuid));
        }

        List<VmNode> allVmNodes = new ArrayList<>();
        hostNodes.forEach(h -> {
            allVmNodes.addAll(h.getVmList());
        });

        System.out.println("Load reduction host:");
        for (HostNode host : hostNodes) {
            List vmUuids = tasks.stream().filter(t -> t.sourceHostUuid.equals(host.getUuid()))
                    .map(MigrateTask::getVmUuid).collect(Collectors.toList());

            if (!vmUuids.isEmpty()) { // sourceHost
                List finalVmUuids = vmUuids;
                double migrateCPU = allVmNodes.stream().filter(vm -> finalVmUuids.contains(vm.uuid)).mapToDouble(VmNode::getUsedCPUPercent).sum();
                long migrateMemory = allVmNodes.stream().filter(vm -> finalVmUuids.contains(vm.uuid)).mapToLong(VmNode::getUsedPhysicalMemoryBit).sum();
                System.out.println(String.format(" %s cpu:%s -> %s, mem:%s -> %s",
                        host.getUuid(),
                        (host.getUsedCPUPercent() + migrateCPU),
                        host.getUsedCPUPercent(),
                        (int)(host.getUsedMemoryPercent() + (1f * migrateMemory / host.getTotalMemoryBit()) * 100),
                        host.getUsedMemoryPercent()
                ));
            }
        }

        System.out.println("Load increase host:");
        for (HostNode host : hostNodes) {
            List vmUuids = tasks.stream().filter(t -> t.targetHostUuid.equals(host.getUuid()))
                    .map(MigrateTask::getVmUuid).collect(Collectors.toList());
            if (!vmUuids.isEmpty()) { // targetHost
                List finalVmUuids = vmUuids;
                double migrateCPU = allVmNodes.stream().filter(vm -> finalVmUuids.contains(vm.uuid)).mapToDouble(VmNode::getUsedCPUPercent).sum();
                long migrateMemory = allVmNodes.stream().filter(vm -> finalVmUuids.contains(vm.uuid)).mapToLong(VmNode::getUsedPhysicalMemoryBit).sum();
                System.out.println(String.format(String.format(" %s cpu:%s -> %s, mem:%s -> %s",
                        host.getUuid(),
                        (host.getUsedCPUPercent() - migrateCPU),
                        host.getUsedCPUPercent(),
                        (int)(host.getUsedMemoryPercent() - (1f * migrateMemory / host.getTotalMemoryBit()) * 100),
                        host.getUsedMemoryPercent()
                )));
            }
        }

        System.out.println("Load constant host:");
        for (HostNode host : hostNodes) {
            List vmUuids = tasks.stream().filter(t -> t.targetHostUuid.equals(host.getUuid()) || t.getSourceHostUuid().equals(host.getUuid()))
                    .map(MigrateTask::getVmUuid).collect(Collectors.toList());
            if (vmUuids.isEmpty()) {
                System.out.println(String.format(String.format(" %s cpu:%s -> %s, mem:%s -> %s",
                        host.getUuid(),
                        host.getUsedCPUPercent(),
                        host.getUsedCPUPercent(),
                        host.getUsedMemoryPercent(),
                        host.getUsedMemoryPercent()
                )));
            }
        }
    }

    private static float vmCpuPctOn(HostNode host, VmNode v) {
        return 100f * v.getUsedPhysicalCpuNum() / (float) host.getTotalCpu();
    }

    private static float vmMemPctOn(HostNode host, VmNode v) {
        return 100f * v.getUsedPhysicalMemoryBit() / (float) host.getTotalMemoryBit();
    }

    private static boolean withinThresholdAfter(HostNode target, VmNode v, Float thCpu, Float thMem, float h) {
        float uCpuAfter = target.getUsedCPUPercent() + vmCpuPctOn(target, v);
        float uMemAfter = target.getUsedMemoryPercent() + vmMemPctOn(target, v);
        boolean cpuOk = thCpu == null || uCpuAfter <= (thCpu - h);
        boolean memOk = thMem == null || uMemAfter <= (thMem - h);
        boolean capacityOk = target.getHostCapacityAvailableCpuNum() >= v.getCpuNum()
                && target.hasMemoryCapacity(v.getMemorySize());
        return cpuOk && memOk && capacityOk;
    }

    /*** fire fighting
     * rule： As long as the target machine is not pushed beyond the threshold (leaving a lag h),
     * it is acceptable; Do not force Δ V<0, as firefighting is given priority
     */
    private void scheduleFireFighting() {
        for (HostNode src : new ArrayList<>(highLoadHost)) {
            // filter: single fix VM that can be moved
            List<VmNode> singleFix = src.getVmList().stream().filter(v -> {
                boolean cpuOk = cpuUsedPercentThreshold == null
                        || (src.getUsedCPUPercent() - vmCpuPctOn(src, v) <= cpuUsedPercentThreshold);
                boolean memOk = memUsedPercentThreshold == null
                        || (src.getUsedMemoryPercent() - vmMemPctOn(src, v) <= memUsedPercentThreshold);
                return cpuOk && memOk;
            }).collect(Collectors.toList());

            // sort：sort by benefit/cost
            singleFix.sort((a, b) -> {
                int c = Long.compare(a.getUsedPhysicalMemoryBit(), b.getUsedPhysicalMemoryBit());
                if (c != 0) {
                    return c;
                }
                return -Double.compare(fireScore(src, a), fireScore(src, b));
            });

            logTrace(String.format("Fire fighting, source host[uuid: %s], candidate single fix VMs[uuids: %s]",
                    src.getUuid(), singleFix.stream().map(it -> it.uuid).collect(Collectors.toList())));

            boolean moved = false;
            for (VmNode v : singleFix) {
                HostNode tgt = firstAcceptableTarget(lowLoadHost, v);
                if (tgt != null) {
                    generateMigrateTask(src, tgt, v);
                    sortHosts();
                    moved = true;
                    break;
                }
            }
            if (!moved) {
                // undertake: choose the VM with the highest benefit/cost ratio that has an acceptable target
                Optional<VmNode> best = src.getVmList().stream()
                        .filter(v -> firstAcceptableTarget(lowLoadHost, v) != null)
                        .max((x, y) -> Double.compare(fireScore(src, x), fireScore(src, y)));


                if (best.isPresent()) {
                    HostNode tgt = firstAcceptableTarget(lowLoadHost, best.get());
                    if (tgt != null) {
                        logTrace(String.format("Fire fighting, source host[uuid: %s]. No single fixed VM can be migrated. " +
                                        "The best remaining VM[uuid: %s] is selected to take over.",
                                src.getUuid(),
                                best.get().uuid));

                        generateMigrateTask(src, tgt, best.get());
                        sortHosts();
                    }
                } else {
                    logTrace(String.format("Fire fighting, source host[uuid: %s]. No single fixed VM can be migrated. " +
                                    "No suitable VM is found to undertake.",
                            src.getUuid()));
                }
            }
        }
    }

    private double fireScore(HostNode src, VmNode v) {
        double needCpu = (cpuUsedPercentThreshold == null) ? 0.0
                : Math.max(0.0, src.getUsedCPUPercent() - cpuUsedPercentThreshold);
        double needMem = (memUsedPercentThreshold == null) ? 0.0
                : Math.max(0.0, src.getUsedMemoryPercent() - memUsedPercentThreshold);
        double vCpu = vmCpuPctOn(src, v);
        double vMem = vmMemPctOn(src, v);
        double benefit = Math.min(vCpu, needCpu) + Math.min(vMem, needMem);
        double cost = Math.max(1.0, v.getUsedCPUPercent() + v.getUsedMemoryPercent());
        return benefit / cost;
    }

    private HostNode firstAcceptableTarget(List<HostNode> targets, VmNode v) {
        for (HostNode t : targets) {
            if (withinThresholdAfter(t, v, cpuUsedPercentThreshold, memUsedPercentThreshold, HYSTERESIS)) {
                return t;
            }
        }
        return null;
    }
}