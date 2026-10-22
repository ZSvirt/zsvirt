package org.zstack.test.unittest.drs;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zstack.drs.DRSGlobalConfig;
import org.zstack.drs.algorithm.DefaultBalanceAlgorithm;
import org.zstack.drs.data.HostNode;
import org.zstack.drs.data.MigrateTask;
import org.zstack.drs.data.VmNode;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class DRSMemoryCapacityTest {
    private static final long GIB = 1024L * 1024 * 1024;
    private String traceValue;

    @Before
    public void disableTrace() {
        traceValue = DRSGlobalConfig.LOG_TRACE_DRS.value();
        DRSGlobalConfig.LOG_TRACE_DRS.setValue("false");
    }

    @After
    public void restoreTrace() {
        DRSGlobalConfig.LOG_TRACE_DRS.setValue(traceValue);
    }

    @Test
    public void rejectsReportedInsufficientAllocationCapacityDespiteLowPhysicalLoad() {
        // ZSV-13521: only 1.36 GiB remains in the allocation ledger, while
        // physical load is low and a stale physical-capacity report says 15.36 GiB.
        HostNode source = source("source", 2 * GIB, 66.7f);
        HostNode target = host("target", 2.4f, GIB, 1461346304L, 1, 0, null);
        assertTrue(schedule(source, target).isEmpty());
    }

    @Test
    public void preservesLargeVmRescueWhenAllocationCapacityIsSufficient() {
        HostNode source = source("source", 2 * GIB, 66.7f);
        HostNode target = host("target", 2.4f, GIB, 16493731840L, 1, 0, null);
        List<MigrateTask> tasks = schedule(source, target);
        assertEquals(1, tasks.size());
        assertEquals("source-vm", tasks.get(0).vmUuid);
        assertEquals("target", tasks.get(0).targetHostUuid);
    }

    @Test
    public void acceptsOverProvisionedMemoryAtTheExactAllocationBoundary() {
        HostNode target = host("target", 2, GIB, GIB, 2, 0, null);
        assertEquals(1, schedule(source("source", 2 * GIB, 30), target).size());
    }

    @Test
    public void protectsReservedMemoryAfterApplyingTheHostRatio() {
        HostNode target = host("target", 2, GIB, 2 * GIB, 2, GIB + 1, null);
        assertTrue(schedule(source("source", 2 * GIB, 30), target).isEmpty());
        target.setMemoryCapacityPolicy(2, GIB, null);
        assertEquals(1, schedule(source("source", 2 * GIB, 30), target).size());
    }

    @Test
    public void cumulativelyReservesMemoryAcrossMultipleSources() {
        HostNode target = host("target", 2, GIB, 3 * GIB, 2, GIB, null);
        List<MigrateTask> tasks = new DefaultBalanceAlgorithm().makeTasks(Arrays.asList(
                source("source1", 3 * GIB, 30), source("source2", 3 * GIB, 30), target), 80f, null);
        assertEquals(1, tasks.size());
        assertEquals("target", tasks.get(0).targetHostUuid);
        // Scheduling operates on a deep copy, without reserving real capacity.
        assertEquals(3 * GIB, target.getHostCapacityAvailableMemoryBit());
    }

    @Test
    public void preservesPerVmRoundingInsteadOfSubtractingFromAScaledBudget() {
        // At ratio 2 each three-byte request costs two ledger bytes. A scaled
        // six-byte budget would incorrectly admit both requests.
        HostNode target = host("target", 2, GIB, 3, 2, 0, null);
        List<MigrateTask> tasks = new DefaultBalanceAlgorithm().makeTasks(Arrays.asList(
                source("source1", 3, 30), source("source2", 3, 30), target), 80f, null);
        assertEquals(1, tasks.size());
    }

    @Test
    public void updatesSourceAndTargetLedgersUsingTheirOwnRatios() throws Exception {
        HostNode source = host("source", 90, GIB, GIB, 3, 0, null);
        HostNode target = host("target", 2, GIB, 4 * GIB, 2, GIB, null);
        VmNode vm = vm("vm", 3 * GIB, 30);
        source.getVmList().add(vm);
        Method migrate = DefaultBalanceAlgorithm.class.getDeclaredMethod(
                "generateMigrateTask", HostNode.class, HostNode.class, VmNode.class);
        migrate.setAccessible(true);
        migrate.invoke(new DefaultBalanceAlgorithm(), source, target, vm);
        assertEquals(2 * GIB, source.getHostCapacityAvailableMemoryBit());
        assertEquals(5 * GIB / 2, target.getHostCapacityAvailableMemoryBit());
        assertTrue(source.getVmList().isEmpty());
        assertEquals(vm, target.getVmList().get(0));
    }

    @Test
    public void enforcesOptionalPerVmPhysicalSizeLimit() {
        HostNode target = host("target", 2, GIB, 16 * GIB, 2, 0, 2 * GIB);
        assertTrue(schedule(source("source", 2 * GIB, 30), target).isEmpty());
        target.setMemoryCapacityPolicy(2, 0, null);
        assertEquals(1, schedule(source("source", 2 * GIB, 30), target).size());
    }

    @Test
    public void retainsCpuThresholdProtection() {
        HostNode target = host("target", 31, GIB, 16 * GIB, 1, 0, null);
        assertTrue(schedule(source("source", 2 * GIB, 66.7f), target).isEmpty());
    }

    @Test
    public void retainsPhysicalMemoryThresholdProtection() {
        HostNode source = host("source", 10, 14 * GIB, 2 * GIB, 1, 0, null);
        VmNode vm = new VmNode("vm", 1, 6.25f, 9 * GIB, 56.25f, 1, 10 * GIB);
        source.getVmList().add(vm);
        HostNode target = host("target", 2, 4 * GIB, 16 * GIB, 2, 0, null);
        assertTrue(new DefaultBalanceAlgorithm().makeTasks(Arrays.asList(source, target), null, 80f).isEmpty());
    }

    @Test
    public void ordinaryBalancingAlsoRejectsInsufficientMemory() throws Exception {
        HostNode source = source("source", 2 * GIB, 30);
        HostNode target = host("target", 2, GIB, GIB, 1, 0, null);
        source.setMaxSize2MoveCPUPercent(40f);
        target.setMaxSize2MoveCPUPercent(40f);
        DefaultBalanceAlgorithm algorithm = new DefaultBalanceAlgorithm();
        Method balance = DefaultBalanceAlgorithm.class.getDeclaredMethod(
                "generateMigrateTasks", HostNode.class, HostNode.class);
        balance.setAccessible(true);
        balance.invoke(algorithm, source, target);
        assertEquals(1, source.getVmList().size());
        assertTrue(target.getVmList().isEmpty());
    }

    private List<MigrateTask> schedule(HostNode source, HostNode target) {
        return new DefaultBalanceAlgorithm().makeTasks(Arrays.asList(source, target), 80f, null);
    }

    private HostNode source(String uuid, long vmMemory, float cpuPercent) {
        HostNode source = host(uuid, 94.9f, 2 * GIB, 8 * GIB, 1, 0, null);
        source.getVmList().add(vm(uuid + "-vm", vmMemory, cpuPercent));
        return source;
    }

    private VmNode vm(String uuid, long memory, float cpuPercent) {
        return new VmNode(uuid, cpuPercent * 16 / 100, cpuPercent, 0, 0, 1, memory);
    }

    private HostNode host(String uuid, float cpuPercent, long usedMemory, long availableMemory,
                          double ratio, long reservedMemory, Long maxVmMemory) {
        HostNode host = new HostNode(uuid, cpuPercent, usedMemory, 160, availableMemory, 16, 16 * GIB);
        host.setMemoryCapacityPolicy(ratio, reservedMemory, maxVmMemory);
        return host;
    }
}
