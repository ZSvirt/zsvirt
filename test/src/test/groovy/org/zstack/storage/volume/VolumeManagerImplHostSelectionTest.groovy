package org.zstack.storage.volume

import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.SimpleQuery
import org.zstack.header.host.HostInventory
import org.zstack.header.host.HostState
import org.zstack.header.host.HostStatus
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostVO_
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO_
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO_
import org.zstack.header.storage.primary.PrimaryStorageHostStatus
import org.zstack.header.vm.VmInstanceConstant

import java.lang.reflect.Field
import java.lang.reflect.Method

class VolumeManagerImplHostSelectionTest {
    private static final String PS_UUID = "ps-uuid"

    private VolumeManagerImpl manager
    private DatabaseFacade dbf
    private SimpleQuery<PrimaryStorageClusterRefVO> clusterRefQuery
    private SimpleQuery<PrimaryStorageHostRefVO> hostRefQuery
    private SimpleQuery<HostVO> hostQuery

    @Before
    void setUp() {
        manager = new VolumeManagerImpl()
        dbf = Mockito.mock(DatabaseFacade.class)
        clusterRefQuery = Mockito.mock(SimpleQuery.class)
        hostRefQuery = Mockito.mock(SimpleQuery.class)
        hostQuery = Mockito.mock(SimpleQuery.class)
        setField("dbf", dbf)

        Mockito.when(dbf.createQuery(PrimaryStorageClusterRefVO.class)).thenReturn(clusterRefQuery)
        Mockito.when(dbf.createQuery(PrimaryStorageHostRefVO.class)).thenReturn(hostRefQuery)
        Mockito.when(dbf.createQuery(HostVO.class)).thenReturn(hostQuery)
        Mockito.when(clusterRefQuery.select(PrimaryStorageClusterRefVO_.clusterUuid)).thenReturn(clusterRefQuery)
        Mockito.when(clusterRefQuery.add(PrimaryStorageClusterRefVO_.primaryStorageUuid, SimpleQuery.Op.EQ, PS_UUID)).thenReturn(clusterRefQuery)
        Mockito.when(hostRefQuery.add(PrimaryStorageHostRefVO_.primaryStorageUuid, SimpleQuery.Op.EQ, PS_UUID)).thenReturn(hostRefQuery)
        Mockito.when(hostQuery.add(HostVO_.clusterUuid, SimpleQuery.Op.IN, Arrays.asList("cluster-uuid"))).thenReturn(hostQuery)
        Mockito.when(hostQuery.add(HostVO_.uuid, SimpleQuery.Op.IN, Arrays.asList("host-connected"))).thenReturn(hostQuery)
        Mockito.when(hostQuery.add(HostVO_.hypervisorType, SimpleQuery.Op.EQ, VmInstanceConstant.KVM_HYPERVISOR_TYPE)).thenReturn(hostQuery)
        Mockito.when(hostQuery.add(HostVO_.status, SimpleQuery.Op.EQ, HostStatus.Connected)).thenReturn(hostQuery)
        Mockito.when(hostQuery.add(HostVO_.state, SimpleQuery.Op.EQ, HostState.Enabled)).thenReturn(hostQuery)
        Mockito.when(hostQuery.orderBy(HostVO_.uuid, SimpleQuery.Od.ASC)).thenReturn(hostQuery)
        Mockito.when(hostQuery.setLimit(1)).thenReturn(hostQuery)
    }

    @Test
    void testSelectHostForEncryptInPlaceUsesConnectedPrimaryStorageHostRef() {
        Mockito.when(clusterRefQuery.listValue()).thenReturn(Arrays.asList("cluster-uuid"))
        Mockito.when(hostRefQuery.list()).thenReturn(Arrays.asList(
                primaryStorageHostRef("host-disconnected", PrimaryStorageHostStatus.Disconnected),
                primaryStorageHostRef("host-connected", PrimaryStorageHostStatus.Connected)
        ))
        HostVO host = new HostVO()
        host.uuid = "host-connected"
        host.clusterUuid = "cluster-uuid"
        host.name = "host"
        host.managementIp = "172.20.0.10"
        host.hypervisorType = VmInstanceConstant.KVM_HYPERVISOR_TYPE
        host.status = HostStatus.Connected
        host.state = HostState.Enabled
        Mockito.when(hostQuery.find()).thenReturn(host)

        HostInventory inventory = invokeSelectHostForEncryptInPlace()

        assert inventory.uuid == "host-connected" : "create-from-template encrypt-in-place should stage the LUKS secret on a connected PS host ref: expected=host-connected actual=${inventory?.uuid}"
        Mockito.verify(hostQuery).add(HostVO_.uuid, SimpleQuery.Op.IN, Arrays.asList("host-connected"))
    }

    private HostInventory invokeSelectHostForEncryptInPlace() {
        Method method = VolumeManagerImpl.class.getDeclaredMethod("selectHostForEncryptInPlace", String.class)
        method.accessible = true
        return method.invoke(manager, PS_UUID) as HostInventory
    }

    private PrimaryStorageHostRefVO primaryStorageHostRef(String hostUuid, PrimaryStorageHostStatus status) {
        PrimaryStorageHostRefVO ref = new PrimaryStorageHostRefVO()
        ref.primaryStorageUuid = PS_UUID
        ref.hostUuid = hostUuid
        ref.status = status
        return ref
    }

    private void setField(String name, Object value) {
        Field field = VolumeManagerImpl.class.getDeclaredField(name)
        field.accessible = true
        field.set(manager, value)
    }
}
