package org.zstack.drs.algorithm;

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

    @Override
    public List<MigrateTask> makeTasks(List<HostNode> hostNodeList, Float cpuUsedPercentThreshold, Float memUsedPercentThreshold) {
        hostNodeList = deepCopy(hostNodeList);

        this.cpuUsedPercentThreshold = cpuUsedPercentThreshold;
        this.memUsedPercentThreshold = memUsedPercentThreshold;

        this.averageCpuLoadPercent = (float) (hostNodeList.stream().mapToDouble(HostNode::getUsedCPUPercent).sum() / hostNodeList.size());
        this.averageMemoryLoadPercent = (float) (hostNodeList.stream().mapToDouble(HostNode::getUsedMemoryPercent).sum() / hostNodeList.size());

        logger.trace(String.format("Before scheduling\n" +
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

        logger.trace(String.format("After scheduling\n" +
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
        for(final Iterator<VmNode> vmNodeIterator = sourceHost.getVmList().iterator(); vmNodeIterator.hasNext();) {
            final VmNode vmNode = vmNodeIterator.next();

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

            if (targetHost.getHostCapacityAvailableCpuNum() < vmNode.getCpuNum() || targetHost.getHostCapacityAvailableMemoryBit() < vmNode.getMemorySize()) {
                continue;
            }

            MigrateTask task = new MigrateTask();
            task.sourceHostUuid = sourceHost.getUuid();
            task.targetHostUuid = targetHost.getUuid();
            task.vmUuid = vmNode.uuid;
            tasks.add(task);

            targetHost.setHostCapacityAvailableCpuNum(targetHost.getHostCapacityAvailableCpuNum() - vmNode.getCpuNum());
            targetHost.setHostCapacityAvailableMemoryBit(targetHost.getHostCapacityAvailableMemoryBit() - vmNode.getMemorySize());
            targetHost.setFreePhysicalMemoryBit(targetHost.getFreePhysicalMemoryBit() - vmNode.getUsedPhysicalMemoryBit());
            targetHost.setUsedCPUPercent(targetHost.getUsedCPUPercent() + vmNode.getUsedCPUPercent());

            sourceHost.setHostCapacityAvailableCpuNum(sourceHost.getHostCapacityAvailableCpuNum() - vmNode.getCpuNum());
            sourceHost.setHostCapacityAvailableMemoryBit(sourceHost.getHostCapacityAvailableMemoryBit() - vmNode.getMemorySize());
            sourceHost.setFreePhysicalMemoryBit(sourceHost.getFreePhysicalMemoryBit() + vmNode.getUsedPhysicalMemoryBit());
            sourceHost.setUsedCPUPercent(targetHost.getUsedCPUPercent() - vmNode.getUsedCPUPercent());

            if (cpuUsedPercentThreshold != null) {
                targetHost.setMaxSize2MoveCPUPercent(targetHost.getMaxSize2MoveCPUPercent() - vmNode.getUsedCPUPercent());
                sourceHost.setMaxSize2MoveCPUPercent(sourceHost.getMaxSize2MoveCPUPercent() - vmNode.getUsedCPUPercent());
            }

            if (memUsedPercentThreshold != null) {
                targetHost.setMaxSize2MoveMemoryBit(targetHost.getMaxSize2MoveMemoryBit() - vmNode.getUsedPhysicalMemoryBit());
                sourceHost.setMaxSize2MoveMemoryBit(sourceHost.getMaxSize2MoveMemoryBit() - vmNode.getUsedPhysicalMemoryBit());
            }

            vmNodeIterator.remove();
            targetHost.getVmList().add(vmNode);
        }
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
}