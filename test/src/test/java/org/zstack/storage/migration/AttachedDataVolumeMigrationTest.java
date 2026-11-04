package org.zstack.storage.migration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.storage.primary.PrimaryStorageState;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.volume.VolumeState;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.storage.migration.primary.APIPrimaryStorageMigrateVolumeMsg;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AttachedDataVolumeMigrationTest {
    private final StorageMigrationApiInterceptor interceptor = new StorageMigrationApiInterceptor();
    private final DatabaseFacade database = mock(DatabaseFacade.class);
    private final VolumeVO volume = new VolumeVO();
    private final VmInstanceVO vm = new VmInstanceVO();
    private final PrimaryStorageVO source = new PrimaryStorageVO();
    private final PrimaryStorageVO destination = new PrimaryStorageVO();
    private final APIPrimaryStorageMigrateVolumeMsg message = new APIPrimaryStorageMigrateVolumeMsg();
    private MockedStatic<Q> queries;
    private MockedStatic<Platform> platform;
    private MockedStatic<StorageMigrationLayoutChecker> layouts;

    @Before
    public void setUp() throws Exception {
        Field field = StorageMigrationApiInterceptor.class.getDeclaredField("dbf");
        field.setAccessible(true);
        field.set(interceptor, database);
        volume.setUuid("volume");
        volume.setPrimaryStorageUuid("source");
        volume.setVmInstanceUuid("vm");
        volume.setType(VolumeType.Data);
        volume.setStatus(VolumeStatus.Ready);
        volume.setState(VolumeState.Enabled);
        vm.setUuid("vm");
        vm.setState(VmInstanceState.Stopped);
        source.setUuid("source");
        source.setState(PrimaryStorageState.Enabled);
        destination.setUuid("destination");
        destination.setState(PrimaryStorageState.Enabled);
        message.setVolumeUuid("volume");
        message.setDstPrimaryStorageUuid("destination");
        when(database.findByUuid("volume", VolumeVO.class)).thenReturn(volume);
        when(database.findByUuid("vm", VmInstanceVO.class)).thenReturn(vm);
        when(database.findByUuid("source", PrimaryStorageVO.class)).thenReturn(source);
        when(database.findByUuid("destination", PrimaryStorageVO.class)).thenReturn(destination);
        Q query = mock(Q.class, RETURNS_SELF);
        queries = mockStatic(Q.class);
        queries.when(() -> Q.New(any(Class.class))).thenReturn(query);
        platform = mockStatic(Platform.class);
        platform.when(() -> Platform.argerr(anyString(), any(), any()))
                .thenReturn(new ErrorCode("TEST.ARGUMENT", "invalid migration", "invalid migration"));
        layouts = mockStatic(StorageMigrationLayoutChecker.class);
    }

    @After
    public void tearDown() {
        if (layouts != null) {
            layouts.close();
        }
        if (platform != null) {
            platform.close();
        }
        if (queries != null) {
            queries.close();
        }
    }

    @Test
    public void stoppedCephVolumeCanMigrateWithoutDetach() throws Exception {
        configure("Ceph");
        validate();
        assertNotNull(message.getType());
        verifyLayoutValidation();
    }

    @Test
    public void stoppedNfsVolumeCanMigrateWithoutDetach() throws Exception {
        configure("NFS");
        validate();
        assertNotNull(message.getType());
        verifyLayoutValidation();
    }

    @Test
    public void stoppedSharedBlockVolumeStillCanMigrate() throws Exception {
        configure("SharedBlock");
        validate();
        assertNotNull(message.getType());
        verifyLayoutValidation();
    }

    @Test
    public void runningVmStillRejectsAttachedVolumes() throws Exception {
        vm.setState(VmInstanceState.Running);
        for (String type : new String[]{"Ceph", "NFS", "SharedBlock"}) {
            configure(type);
            assertThrows(ApiMessageInterceptionException.class, this::validate);
        }
        layouts.verifyNoInteractions();
    }

    @Test
    public void unsupportedAttachedStorageStillRequiresDetach() throws Exception {
        configure("LocalStorage");
        assertThrows(ApiMessageInterceptionException.class, this::validate);
        layouts.verifyNoInteractions();
    }

    @Test
    public void detachedVolumeDoesNotRequireStoppedVm() throws Exception {
        configure("LocalStorage");
        volume.setVmInstanceUuid(null);
        validate();
        layouts.verifyNoInteractions();
    }

    private void configure(String type) {
        source.setType(type);
        destination.setType(type);
        interceptor.setPrimaryStoragePrimaryStorageMetrics(
                Collections.singletonMap(type, Collections.singletonList(type)));
    }

    private void verifyLayoutValidation() {
        layouts.verify(() -> StorageMigrationLayoutChecker.validateFinalLayout(
                vm, Collections.singletonMap("volume", "destination"), null, message.getSystemTags()));
    }

    private void validate() throws Exception {
        Method method = StorageMigrationApiInterceptor.class.getDeclaredMethod(
                "validate", APIPrimaryStorageMigrateVolumeMsg.class);
        method.setAccessible(true);
        try {
            method.invoke(interceptor, message);
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }
}
