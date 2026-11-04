package org.zstack.test.unittest.drs;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.allocator.HostAllocatorGlobalConfig;
import org.zstack.compute.allocator.HostCapacityReserveManager;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.drs.DRSBase;
import org.zstack.drs.data.HostLoad;
import org.zstack.drs.data.HostNode;
import org.zstack.header.allocator.HostCapacityOverProvisioningManager;
import org.zstack.header.allocator.HostCapacityVO;
import org.zstack.header.allocator.ReservedHostCapacity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DRSHostCapacityInputTest {
    private static final long GIB = 1024L * 1024 * 1024;
    private String maxMemoryCheckValue;

    @Before
    public void disableMaxMemoryCheck() {
        maxMemoryCheckValue = HostAllocatorGlobalConfig.HOST_ALLOCATOR_MAX_MEMORY.value();
        HostAllocatorGlobalConfig.HOST_ALLOCATOR_MAX_MEMORY.setValue("false");
    }

    @After
    public void restoreMaxMemoryCheck() {
        HostAllocatorGlobalConfig.HOST_ALLOCATOR_MAX_MEMORY.setValue(maxMemoryCheckValue);
    }

    @Test
    public void readsAllocationLedgerInsteadOfStalePhysicalAvailability() throws Exception {
        HostCapacityVO capacity = capacity(1461346304L);
        HostNode host = buildHost(capacity, 1, 0);
        assertEquals(1461346304L, host.getHostCapacityAvailableMemoryBit());
        assertFalse(host.hasMemoryCapacity(2 * GIB));
    }

    @Test
    public void loadsTheHostRatioAndReservedMemoryFromAllocatorServices() throws Exception {
        HostCapacityVO capacity = capacity(2 * GIB);
        assertTrue(buildHost(capacity, 2, GIB).hasMemoryCapacity(2 * GIB));
        assertFalse(buildHost(capacity, 2, GIB + 1).hasMemoryCapacity(2 * GIB));
    }

    @Test
    public void loadsTheOptionalPhysicalSizeLimit() throws Exception {
        HostCapacityVO capacity = capacity(4 * GIB);
        capacity.setTotalPhysicalMemory(2 * GIB);
        HostAllocatorGlobalConfig.HOST_ALLOCATOR_MAX_MEMORY.setValue("true");
        assertFalse(buildHost(capacity, 2, 0).hasMemoryCapacity(2 * GIB));
        HostAllocatorGlobalConfig.HOST_ALLOCATOR_MAX_MEMORY.setValue("false");
        assertTrue(buildHost(capacity, 2, 0).hasMemoryCapacity(2 * GIB));
    }

    private HostCapacityVO capacity(long availableMemory) {
        HostCapacityVO capacity = new HostCapacityVO();
        capacity.setAvailableMemory(availableMemory);
        capacity.setAvailablePhysicalMemory(16493731840L);
        capacity.setAvailableCpu(160);
        capacity.setCpuNum(16);
        capacity.setTotalMemory(16 * GIB);
        capacity.setTotalPhysicalMemory(16 * GIB);
        return capacity;
    }

    @SuppressWarnings("unchecked")
    private HostNode buildHost(HostCapacityVO capacity, double ratio, long reservedMemory) throws Exception {
        DatabaseFacade dbf = mock(DatabaseFacade.class);
        when(dbf.findByUuid("host", HostCapacityVO.class)).thenReturn(capacity);
        HostCapacityOverProvisioningManager ratios = mock(HostCapacityOverProvisioningManager.class);
        when(ratios.getMemoryRatio("host")).thenReturn(ratio);
        HostCapacityReserveManager reserves = mock(HostCapacityReserveManager.class);
        ReservedHostCapacity reserved = new ReservedHostCapacity();
        reserved.setReservedMemoryCapacity(reservedMemory);
        when(reserves.getReservedHostCapacityByHosts(Collections.singletonList("host"))).thenReturn(reserved);

        // Only buildHostNodes is exercised; no DRS workflow or database writes.
        DRSBase drs = mock(DRSBase.class, CALLS_REAL_METHODS);
        inject(drs, "dbf", dbf);
        inject(drs, "memoryRatioMgr", ratios);
        inject(drs, "capacityReserveMgr", reserves);
        Method build = DRSBase.class.getDeclaredMethod("buildHostNodes", List.class);
        build.setAccessible(true);
        HostLoad load = new HostLoad("host", 2.4f, GIB, Collections.emptyList());
        List<HostNode> nodes = (List<HostNode>) build.invoke(drs, Collections.singletonList(load));
        return nodes.get(0);
    }

    private void inject(DRSBase drs, String name, Object value) throws Exception {
        Field field = DRSBase.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(drs, value);
    }
}
