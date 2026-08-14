package org.zstack.zwatch.prometheus;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.Component;
import org.zstack.header.baremetal.pxeserver.BaremetalPxeServerVO;
import org.zstack.header.core.BypassWhenUnitTest;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostVO;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;
import org.zstack.premium.externalservice.prometheus.Prometheus;
import org.zstack.premium.externalservice.prometheus.PrometheusConfig;
import org.zstack.premium.externalservice.prometheus.PrometheusFactory;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.StringDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static java.util.Arrays.asList;

public class PrometheusStaticConfigManagerImpl implements PrometheusStaticConfigManager, ManagementNodeReadyExtensionPoint, ManagementNodeChangeListener, Component {
    private static final CLogger logger = Utils.getLogger(PrometheusStaticConfigManagerImpl.class);

    public static final String HOST_DIR = PathUtil.join(Prometheus.DISCORVERY_ROOT, "hosts");
    public static final String BS_DIR = PathUtil.join(Prometheus.DISCORVERY_ROOT, "backupStorage");
    public static final String VR_DIR = PathUtil.join(Prometheus.DISCORVERY_ROOT, "vrouter");
    public static final String PXESERVER_DIR = PathUtil.join(Prometheus.DISCORVERY_ROOT, "pxeserver");

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
                    if (file.getName().contains(String.format("-%s.json", ipstr)) || file.getName().contains(uuid)) {
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
        ConfigWriter writer = new ConfigWriter();
        writer.resourceType = HostVO.class;
        writer.configs = configs;
        writer.dir = dir;
        writer.ip = ip;
        writer.uuid = hostUuid;
        writer.write();
    }


    public void deleteHostConfig(String hostUuid, String ip) {
        File cfile = new File(PathUtil.join(HOST_DIR, configFilename(hostUuid, ip)));
        deleteFile(cfile);
    }

    public void deleteVRConfig(String vrUuid, String ip) {
        File cfile = new File(PathUtil.join(VR_DIR, configFilename(vrUuid, ip)));
        deleteFile(cfile);
    }

    private void deleteConfig(String uuid) {
        Runnable r = deletors.get(uuid);
        if (r != null) {
            r.run();
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
        ConfigWriter writer = new ConfigWriter();
        writer.resourceType = VirtualRouterVmVO.class;
        writer.configs = Collections.singletonList(config);
        writer.dir = VR_DIR;
        writer.ip = ip;
        writer.uuid = vrUuid;
        writer.write();
    }

    @Override
    @BypassWhenUnitTest
    public void deleteVRConfig(String vrUuid) {
        deleteConfig(vrUuid);
    }

    @Override
    @BypassWhenUnitTest
    public void writeBackupStorageConfig(String bsUuid, String bsIp, PrometheusConfig.StaticConfig config) {
        ConfigWriter writer = new ConfigWriter();
        writer.resourceType = BackupStorageVO.class;
        writer.configs = Collections.singletonList(config);
        writer.dir = BS_DIR;
        writer.ip = bsIp;
        writer.uuid = bsUuid;
        writer.write();
    }

    @Override
    @BypassWhenUnitTest
    public void deleteBackupStorageConfig(String bsUuid) {
        deleteConfig(bsUuid);
    }

    @Override
    @BypassWhenUnitTest
    public void writeBaremetaPxeServerConfig(String uuid, String ip, PrometheusConfig.StaticConfig config) {
        ConfigWriter writer = new ConfigWriter();
        writer.resourceType = BaremetalPxeServerVO.class;
        writer.configs = Collections.singletonList(config);
        writer.dir = PXESERVER_DIR;
        writer.ip = ip;
        writer.uuid = uuid;
        writer.write();
    }

    @Override
    public void nodeJoin(ManagementNodeInventory inv) {
        createConfigManagedByUs();
    }

    private void createConfigManagedByUs() {
        if (PrometheusHelper.isPrometheusDisabled()) {
            logger.debug("Prometheus is disabled, skip writing config");
            return;
        }

        createConfigForVRManagedByUs();
        createConfigForHostsManagedByUs();
    }

    @Override
    public void nodeLeft(ManagementNodeInventory inv) {
        createConfigManagedByUs();
    }

    @Override
    public void iAmDead(ManagementNodeInventory inv) {
    }

    @Override
    public void iJoin(ManagementNodeInventory inv) {
    }

    @Override
    public void managementNodeReady() {
        cleanupStaticConfigNotManagedByUs();
    }

    private void cleanupStaticConfigNotManagedByUs() {
        cleanupStaticConfigNotManagedByUs(HOST_DIR);
        cleanupStaticConfigNotManagedByUs(VR_DIR);
    }

    private void cleanupStaticConfigNotManagedByUs(String dirPath) {
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
                    throw new CloudRuntimeException(String.format("%s is not a zstack uuid, %s", uuid, f.getAbsolutePath()));
                }

                if (!destinationMaker.isManagedByUs(uuid)) {
                    deleteFile(f);
                }
            }
        }
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
                if (HostVO.class.getSimpleName().equals(d.getResourceType())) {
                    deleteHostConfig(d.getUuid(), d.getIp());
                } else if (VirtualRouterVmVO.class.getSimpleName().equals(d.getResourceType())) {
                    deleteVRConfig(d.getUuid(), d.getIp());
                } else {
                    throw new CloudRuntimeException(String.format("unsupported resource type[%s]", d.getResourceType()));
                }
            }
        });

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
