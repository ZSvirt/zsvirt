package org.zstack.zwatch.prometheus;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.Component;
import org.zstack.header.baremetal.pxeserver.BaremetalPxeServerGlobalProperty;
import org.zstack.header.baremetal.pxeserver.BaremetalPxeServerStatus;
import org.zstack.header.baremetal.pxeserver.BaremetalPxeServerVO;
import org.zstack.header.baremetal.pxeserver.BaremetalPxeServerVO_;
import org.zstack.header.core.BypassWhenUnitTest;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostVO;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.storage.backup.BackupStorageStatus;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;
import org.zstack.premium.externalservice.prometheus.Prometheus;
import org.zstack.premium.externalservice.prometheus.PrometheusConfig;
import org.zstack.premium.externalservice.prometheus.PrometheusFactory;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageSelector;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageVO;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageVO_;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.StringDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PrometheusStaticConfigManagerImpl implements PrometheusStaticConfigManager, ManagementNodeReadyExtensionPoint, ManagementNodeChangeListener, Component {
    private static final CLogger logger = Utils.getLogger(PrometheusStaticConfigManagerImpl.class);

    public static final String HOST_DIR = PathUtil.join(Prometheus.DISCORVERY_ROOT, "hosts");
    public static final String BS_DIR = PathUtil.join(Prometheus.DISCORVERY_ROOT, "backupStorage");
    public static final String VR_DIR = PathUtil.join(Prometheus.DISCORVERY_ROOT, "vrouter");
    public static final String PXESERVER_DIR = PathUtil.join(Prometheus.DISCORVERY_ROOT, "pxeserver");

    private final Object staticConfigLock = new Object();
    private Map<String, Runnable> deletors = new HashMap<>();
    private Map<String, List<File>> configFileMap = new HashMap<>();

    @Autowired
    private ResourceDestinationMaker destinationMaker;
    @Autowired
    private EventFacade evf;
    @Autowired
    private PrometheusFactory prometheusFactory;
    @Autowired
    private PluginRegistry pluginRgty;

    private void deleteFile(File file) {
        if (file.exists()) {
            if (!file.delete()) {
                logger.warn(String.format("failed to delete file[%s]", file.getAbsolutePath()));
            } else {
                logger.debug(String.format("delete prometheus static config[%s]", file.getAbsolutePath()));
            }
        }
    }

    private boolean isConfigFileForUuid(File file, String uuid) {
        return file.isFile() && file.getName().startsWith(String.format("%s-", uuid)) && file.getName().endsWith(".json");
    }

    private boolean isConfigFileForIp(File file, String ipstr) {
        return file.isFile() && file.getName().endsWith(String.format("-%s.json", ipstr));
    }

    private void deleteConfigFilesForUuid(String dirPath, String uuid) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            return;
        }

        File[] files = dir.listFiles(file -> isConfigFileForUuid(file, uuid));
        if (files != null) {
            for (File file : files) {
                deleteFile(file);
            }
        }
    }

    private void createConfigForHostsManagedByUs() {
        new SQLBatchWithReturn<List<Runnable>>() {
            @Override
            protected List<Runnable> scripts() {
                List<Runnable> runnables = new ArrayList<>();

                long count = q(HostVO.class).count();
                sql("select h from HostVO h", HostVO.class).limit(1000).paginate(count, hosts -> hosts.forEach(host ->  {
                    HostVO vo = (HostVO) host;
                    if (!destinationMaker.isManagedByUs(vo.getUuid()))  {
                        return;
                    }

                    pluginRgty.getExtensionList(HostConfigExtensionPoint.class).forEach(e -> {
                        runnables.add(() -> writeHostConfig(vo.getUuid(), vo.getManagementIp(), e.getHostConfig(vo.getUuid(), vo.getManagementIp()), e.getHostConfigPath()));

                    });
                }));

                return runnables;
            }
        }.execute().forEach(Runnable::run);
    }

    private void createConfigForVRManagedByUs() {
        new SQLBatchWithReturn<List<Runnable>>() {
            @Override
            protected List<Runnable> scripts() {
                List<Runnable> runnables = new ArrayList<>();

                long count = q(VirtualRouterVmVO.class).count();
                sql("select vm from VirtualRouterVmVO vm", VirtualRouterVmVO.class)
                        .limit(1000)
                        .paginate(count, vrs -> vrs.forEach(vr -> {
                            VirtualRouterVmVO vo = (VirtualRouterVmVO) vr;
                            if (!destinationMaker.isManagedByUs(vo.getUuid())) {
                                return;
                            }

                            runnables.add(()-> {
                                VirtualRouterVmInventory inv = vo.toInventory();
                                String uuid = inv.getUuid();
                                String ip = inv.getManagementNic().getIp();

                                writeVRConfig(uuid, ip, PrometheusStaticConfigManager.createVirtualRouterConfig(uuid, ip));
                            });
                        }));

                return runnables;
            }
        }.execute().forEach(Runnable::run);
    }

    private void createConfigForBackupStorageManagedByUs() {
        new SQLBatchWithReturn<List<Runnable>>() {
            @Override
            protected List<Runnable> scripts() {
                List<Runnable> runnables = new ArrayList<>();
                Set<String> remoteBsUuids = new HashSet<>(ImageStoreBackupStorageSelector.getRemoteBsUuids());

                long count = q(ImageStoreBackupStorageVO.class).count();
                sql("select bs from ImageStoreBackupStorageVO bs", ImageStoreBackupStorageVO.class)
                        .limit(1000)
                        .paginate(count, bss -> bss.forEach(bs -> {
                            ImageStoreBackupStorageVO vo = (ImageStoreBackupStorageVO) bs;
                            if (vo.getStatus() != BackupStorageStatus.Connected ||
                                    remoteBsUuids.contains(vo.getUuid()) ||
                                    !destinationMaker.isManagedByUs(vo.getUuid())) {
                                return;
                            }

                            runnables.add(() -> writeCurrentBackupStorageConfig(vo.getUuid()));
                        }));

                return runnables;
            }
        }.execute().forEach(Runnable::run);
    }

    private void createConfigForBaremetalPxeServerManagedByUs() {
        new SQLBatchWithReturn<List<Runnable>>() {
            @Override
            protected List<Runnable> scripts() {
                List<Runnable> runnables = new ArrayList<>();

                long count = q(BaremetalPxeServerVO.class).count();
                sql("select pxe from BaremetalPxeServerVO pxe", BaremetalPxeServerVO.class)
                        .limit(1000)
                        .paginate(count, pxes -> pxes.forEach(pxe -> {
                            BaremetalPxeServerVO vo = (BaremetalPxeServerVO) pxe;
                            if (vo.getStatus() != BaremetalPxeServerStatus.Connected ||
                                    !destinationMaker.isManagedByUs(vo.getUuid())) {
                                return;
                            }

                            runnables.add(() -> writeCurrentBaremetalPxeServerConfig(vo.getUuid()));
                        }));

                return runnables;
            }
        }.execute().forEach(Runnable::run);
    }

    private void writeCurrentBaremetalPxeServerConfig(String uuid) {
        BaremetalPxeServerVO vo = new SQLBatchWithReturn<BaremetalPxeServerVO>() {
            @Override
            protected BaremetalPxeServerVO scripts() {
                return q(BaremetalPxeServerVO.class).eq(BaremetalPxeServerVO_.uuid, uuid).find();
            }
        }.execute();

        if (vo == null ||
                vo.getStatus() != BaremetalPxeServerStatus.Connected ||
                !destinationMaker.isManagedByUs(vo.getUuid())) {
            return;
        }

        PrometheusConfig.StaticConfig config = new PrometheusConfig.StaticConfig();
        Set<String> targets = new HashSet<>();
        targets.add(String.format("%s:%s", vo.getHostname(), BaremetalPxeServerGlobalProperty.PUSHGATEWAY_PORT));
        config.targets = targets;
        writeBaremetaPxeServerConfig(vo.getUuid(), vo.getHostname(), config);
    }

    private void writeCurrentBackupStorageConfig(String uuid) {
        ImageStoreBackupStorageVO vo = new SQLBatchWithReturn<ImageStoreBackupStorageVO>() {
            @Override
            protected ImageStoreBackupStorageVO scripts() {
                return q(ImageStoreBackupStorageVO.class).eq(ImageStoreBackupStorageVO_.uuid, uuid).find();
            }
        }.execute();

        if (vo == null ||
                vo.getStatus() != BackupStorageStatus.Connected ||
                ImageStoreBackupStorageSelector.isRemote(vo.getUuid()) ||
                !destinationMaker.isManagedByUs(vo.getUuid())) {
            return;
        }

        writeBackupStorageConfig(vo.getUuid(), vo.getHostname(),
                PrometheusStaticConfigManager.createBackupStorageConfig(vo.getUuid(), vo.getHostname()));
    }

    private String configFilename(String uuid, String ip) {
        return String.format("%s-%s.json", uuid, ip.replaceAll("\\.", "-"));
    }

    class ConfigWriter {
        String uuid;
        String ip;
        List<PrometheusConfig.StaticConfig> configs;
        Class resourceType;
        String dir;

        private void cleanStaleConfig(String dirStr, String uuid, String ipstr) {
            File dir = new File(dirStr);
            dir.mkdirs();
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (isConfigFileForIp(file, ipstr) || isConfigFileForUuid(file, uuid)) {
                        deleteFile(file);
                    }
                }
            }
        }

        void write() {
            DebugUtils.Assert(uuid != null, "uuid cannot be null");
            DebugUtils.Assert(ip != null, "ip cannot be null");
            DebugUtils.Assert(!configs.isEmpty(), "configs cannot be empty");
            DebugUtils.Assert(resourceType != null, "resourceType cannot be null");
            DebugUtils.Assert(dir != null, "dir cannot be null");

            if (!destinationMaker.isManagedByUs(uuid)) {
                logger.warn(String.format("refuse to write prometheus static config for %s[uuid:%s], it's not managed by us", resourceType.getSimpleName(), uuid));
                return;
            }

            String ipstr = ip.replaceAll("\\.", "-");
            cleanStaleConfig(dir, uuid, ipstr);

            File cfile = new File(PathUtil.join(dir, configFilename(uuid, ip)));
            try {
                FileUtils.write(cfile, JSONObjectUtil.toJsonString(configs));
            } catch (IOException e) {
                throw new CloudRuntimeException(e);
            }

            configFileMap.computeIfAbsent(uuid, k -> new ArrayList<>()).add(cfile);
            deletors.put(uuid, () -> {
                List<File> files = configFileMap.remove(uuid);
                for (File file : files) {
                    deleteFile(file);
                }
                deletors.remove(uuid);

                prometheusFactory.getPrometheus().reload();
            });

            prometheusFactory.getPrometheus().reload();

            logger.debug(String.format("write prometheus static config for %s[uuid:%s, ip:%s]", resourceType.getSimpleName(), uuid, ip));

            PrometheusCanonicalEvents.StaticConfigWriteData data = new PrometheusCanonicalEvents.StaticConfigWriteData();
            data.setUuid(uuid);
            data.setIp(ip);
            data.setResourceType(resourceType.getSimpleName());
            evf.fire(PrometheusCanonicalEvents.STATIC_CONFIG_WRITE, data);
        }
    }

    @Override
    @BypassWhenUnitTest
    public void writeHostConfig(String hostUuid, String ip, List<PrometheusConfig.StaticConfig> configs, String dir) {
        synchronized (staticConfigLock) {
            ConfigWriter writer = new ConfigWriter();
            writer.resourceType = HostVO.class;
            writer.configs = configs;
            writer.dir = dir;
            writer.ip = ip;
            writer.uuid = hostUuid;
            writer.write();
        }
    }


    public void deleteHostConfig(String hostUuid, String ip) {
        synchronized (staticConfigLock) {
            for (String dirPath : hostConfigDirs()) {
                deleteConfigFilesForUuid(dirPath, hostUuid);
            }
        }
    }

    public void deleteVRConfig(String vrUuid, String ip) {
        synchronized (staticConfigLock) {
            deleteConfigFilesForUuid(VR_DIR, vrUuid);
        }
    }

    public void deleteBackupStorageConfig(String bsUuid, String ip) {
        synchronized (staticConfigLock) {
            deleteConfigFilesForUuid(BS_DIR, bsUuid);
        }
    }

    public void deleteBaremetaPxeServerConfig(String uuid, String ip) {
        synchronized (staticConfigLock) {
            deleteConfigFilesForUuid(PXESERVER_DIR, uuid);
        }
    }

    private void deleteConfig(String uuid) {
        synchronized (staticConfigLock) {
            Runnable r = deletors.get(uuid);
            if (r != null) {
                r.run();
            }
        }
    }

    @Override
    @BypassWhenUnitTest
    public void deleteHostConfig(String hostUuid) {
        deleteConfig(hostUuid);
    }

    @Override
    @BypassWhenUnitTest
    public void writeVRConfig(String vrUuid, String ip, PrometheusConfig.StaticConfig config) {
        synchronized (staticConfigLock) {
            ConfigWriter writer = new ConfigWriter();
            writer.resourceType = VirtualRouterVmVO.class;
            writer.configs = Collections.singletonList(config);
            writer.dir = VR_DIR;
            writer.ip = ip;
            writer.uuid = vrUuid;
            writer.write();
        }
    }

    @Override
    @BypassWhenUnitTest
    public void deleteVRConfig(String vrUuid) {
        deleteConfig(vrUuid);
    }

    @Override
    @BypassWhenUnitTest
    public void writeBackupStorageConfig(String bsUuid, String bsIp, PrometheusConfig.StaticConfig config) {
        synchronized (staticConfigLock) {
            ConfigWriter writer = new ConfigWriter();
            writer.resourceType = BackupStorageVO.class;
            writer.configs = Collections.singletonList(config);
            writer.dir = BS_DIR;
            writer.ip = bsIp;
            writer.uuid = bsUuid;
            writer.write();
        }
    }

    @Override
    @BypassWhenUnitTest
    public void deleteBackupStorageConfig(String bsUuid) {
        deleteConfig(bsUuid);
    }

    @Override
    @BypassWhenUnitTest
    public void writeBaremetaPxeServerConfig(String uuid, String ip, PrometheusConfig.StaticConfig config) {
        synchronized (staticConfigLock) {
            ConfigWriter writer = new ConfigWriter();
            writer.resourceType = BaremetalPxeServerVO.class;
            writer.configs = Collections.singletonList(config);
            writer.dir = PXESERVER_DIR;
            writer.ip = ip;
            writer.uuid = uuid;
            writer.write();
        }
    }

    @Override
    public void nodeJoin(ManagementNodeInventory inv) {
        reconcileStaticConfig();
    }

    private void createConfigManagedByUs() {
        if (PrometheusHelper.isPrometheusDisabled()) {
            logger.debug("Prometheus is disabled, skip writing config");
            return;
        }

        createConfigForVRManagedByUs();
        createConfigForHostsManagedByUs();
        createConfigForBackupStorageManagedByUs();
        createConfigForBaremetalPxeServerManagedByUs();
    }

    private void reconcileStaticConfig() {
        synchronized (staticConfigLock) {
            Throwable failure = null;
            try {
                createConfigManagedByUs();
            } catch (Throwable t) {
                failure = t;
            }

            try {
                cleanupStaticConfigNotManagedByUs();
            } catch (Throwable t) {
                if (failure == null) {
                    failure = t;
                } else {
                    failure.addSuppressed(t);
                }
            }

            if (failure != null) {
                throwFailure(failure);
            }
        }
    }

    private void throwFailure(Throwable failure) {
        if (failure instanceof RuntimeException) {
            throw (RuntimeException) failure;
        }
        if (failure instanceof Error) {
            throw (Error) failure;
        }
        throw new CloudRuntimeException(failure);
    }

    @Override
    public void nodeLeft(ManagementNodeInventory inv) {
        reconcileStaticConfig();
    }

    @Override
    public void iAmDead(ManagementNodeInventory inv) {
        // intentionally no-op: the dying node has no chance to reconcile its own files.
    }

    @Override
    public void iJoin(ManagementNodeInventory inv) {
        reconcileStaticConfig();
    }

    @Override
    public void managementNodeReady() {
        reconcileStaticConfig();
    }

    private void cleanupStaticConfigNotManagedByUs() {
        RuntimeException failure = null;
        for (String dirPath : cleanupStaticConfigDirs()) {
            try {
                cleanupStaticConfigNotManagedByUs(dirPath);
            } catch (RuntimeException e) {
                failure = addFailure(failure, e);
            }
        }

        if (failure != null) {
            throw failure;
        }
    }

    private List<String> cleanupStaticConfigDirs() {
        Set<String> dirs = new LinkedHashSet<>();
        dirs.addAll(hostConfigDirs());
        dirs.add(VR_DIR);
        dirs.add(BS_DIR);
        dirs.add(PXESERVER_DIR);
        return new ArrayList<>(dirs);
    }

    private List<String> hostConfigDirs() {
        Set<String> dirs = new LinkedHashSet<>();
        dirs.add(HOST_DIR);
        pluginRgty.getExtensionList(HostConfigExtensionPoint.class).forEach(e -> dirs.add(e.getHostConfigPath()));
        return new ArrayList<>(dirs);
    }

    private void cleanupStaticConfigNotManagedByUs(String dirPath) {
        RuntimeException failure = null;
        File dir = new File(dirPath);
        if (!dir.exists()) {
            return;
        }

        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                String[] fs = f.getName().split("-", 2);
                String uuid = fs[0];
                if (!StringDSL.isZStackUuid(uuid)) {
                    failure = addFailure(failure, new CloudRuntimeException(String.format("%s is not a zstack uuid, %s", uuid, f.getAbsolutePath())));
                    continue;
                }

                if (!destinationMaker.isManagedByUs(uuid)) {
                    deleteFileIfStillNotManaged(uuid, f);
                }
            }
        }

        if (failure != null) {
            throw failure;
        }
    }

    private void deleteFileIfStillNotManaged(String uuid, File file) {
        if (!destinationMaker.isManagedByUs(uuid)) {
            deleteFile(file);
        }
    }

    private void deleteConfigFilesForUuidIfStillNotManaged(String dirPath, String uuid) {
        if (destinationMaker.isManagedByUs(uuid)) {
            logger.debug(String.format("ignore remote prometheus static config write event for uuid[%s], it's managed by us", uuid));
            return;
        }

        deleteConfigFilesForUuid(dirPath, uuid);
    }

    private RuntimeException addFailure(RuntimeException failure, RuntimeException next) {
        if (failure == null) {
            return next;
        }

        failure.addSuppressed(next);
        return failure;
    }

    @Override
    public boolean start() {
        evf.on(PrometheusCanonicalEvents.STATIC_CONFIG_WRITE, new EventCallback() {
            // other node writing the static config file means the resource
            // is owned by him and we need remove ours if any
            @Override
            @ExceptionSafe
            protected void run(Map tokens, Object o) {
                if (evf.isFromThisManagementNode(tokens)) {
                    return;
                }

                PrometheusCanonicalEvents.StaticConfigWriteData d = (PrometheusCanonicalEvents.StaticConfigWriteData) o;
                handleStaticConfigWriteEvent(d);
            }
        });

        return true;
    }

    private void handleStaticConfigWriteEvent(PrometheusCanonicalEvents.StaticConfigWriteData d) {
        synchronized (staticConfigLock) {
            if (HostVO.class.getSimpleName().equals(d.getResourceType())) {
                for (String dirPath : hostConfigDirs()) {
                    deleteConfigFilesForUuidIfStillNotManaged(dirPath, d.getUuid());
                }
            } else if (VirtualRouterVmVO.class.getSimpleName().equals(d.getResourceType())) {
                deleteConfigFilesForUuidIfStillNotManaged(VR_DIR, d.getUuid());
            } else if (BackupStorageVO.class.getSimpleName().equals(d.getResourceType())) {
                deleteConfigFilesForUuidIfStillNotManaged(BS_DIR, d.getUuid());
            } else if (BaremetalPxeServerVO.class.getSimpleName().equals(d.getResourceType())) {
                deleteConfigFilesForUuidIfStillNotManaged(PXESERVER_DIR, d.getUuid());
            } else {
                throw new CloudRuntimeException(String.format("unsupported resource type[%s]", d.getResourceType()));
            }
        }
    }

    @Override
    public boolean stop() {
        return true;
    }
}
