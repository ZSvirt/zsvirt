package org.zstack.upgrade.hack;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.Component;
import org.zstack.header.host.HostInventory;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.tag.SystemTagLifeCycleListener;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMGlobalConfig;
import org.zstack.kvm.KVMSystemTags;
import org.zstack.kvm.KvmHostUpdateOsExtensionPoint;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.VersionComparator;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

public class LibvirtUpgradeExtensionPoint implements KvmHostUpdateOsExtensionPoint, Component {
    private static final CLogger logger = Utils.getLogger(LibvirtUpgradeExtensionPoint.class);

    @Autowired
    private ResourceConfigFacade rcf;

    @Override
    public void afterUpdateOs(Map data, HostInventory host) {
        if (!data.containsKey(KvmHostUpdateOsExtensionPoint.UPDATE_OS_RSP)) {
            logger.debug("LibvirtUpgradeExtensionPoint do not contain key: " + KvmHostUpdateOsExtensionPoint.UPDATE_OS_RSP);
            return;
        }

        KVMAgentCommands.UpdateHostOSRsp rsp = (KVMAgentCommands.UpdateHostOSRsp) data.get(KvmHostUpdateOsExtensionPoint.UPDATE_OS_RSP);

        if (rsp.getLibvirtVersion() == null) {
            logger.debug(String.format("Libvirt version is null, skip %s", this.getClass().getSimpleName()));
            return;
        }

        String libvirtPackageVersion = KVMSystemTags.LIBVIRT_PACKAGE_VERSION.getTokenByResourceUuid(host.getUuid(), KVMSystemTags.LIBVIRT_PACKAGE_VERSION_TOKEN);
        if (libvirtPackageVersion != null && libvirtPackageVersion.equals(rsp.getLibvirtVersion().trim())) {
            logger.debug(String.format("Libvirt version is not changed, skip %s", this.getClass().getSimpleName()));
            return;
        }

        enableLibvirtRestartDuringDeployment(rsp.getLibvirtVersion(), host.getUuid());

        String enable = rcf.getResourceConfigValue(KVMGlobalConfig.RECONNECT_HOST_RESTART_LIBVIRTD_SERVICE, host.getUuid(), String.class);
        if (enable != null && enable.equals("true")) {
            SystemTagCreator creator = KVMSystemTags.FORCE_DEPLOYMENT_ONCE.newSystemTagCreator(host.getUuid());
            creator.recreate = true;
            creator.create();
        }
    }


    private void enableLibvirtRestartDuringDeployment(String libvirtPackageVersion, String hostUuid) {
        // 4.9.0-29.g87e685d.el7
        // transform to 4.9.0.29
        String[] version = libvirtPackageVersion.trim().split("\\.");

        if (version.length < 3) {
            logger.debug("Libvirt version is not valid: " + libvirtPackageVersion);
            return;
        }

        String[] patch = version[2].split("-");

        String libvirtVersionWithoutPatch = version[0] + "." + version[1] + "." + patch[0];
        String libvirtVersion = version[0] + "." + version[1] + "." + patch[0] + "." + patch[1];
        logger.debug("Libvirt version: " + libvirtVersion);

        if (isLibvirt490(libvirtVersionWithoutPatch)) {
            logger.debug("Libvirt version is 4.9.0, do some hack");

            ResourceConfig rc = rcf.getResourceConfig(KVMGlobalConfig.RECONNECT_HOST_RESTART_LIBVIRTD_SERVICE.getIdentity());
            if (new VersionComparator(libvirtVersion).compare("4.9.0.30") >= 0) {
                logger.debug("Libvirt version is greater than 4.9.0.30");
                rc.updateValue(hostUuid, "true");
            } else {
                logger.debug("Libvirt version is less than 4.9.0.30");
                rc.updateValue(hostUuid, "false");
            }
        } else if (isLibvirt600(libvirtVersionWithoutPatch)) {
            logger.debug("Libvirt version is 6.0.0, do some hack");
            ResourceConfig rc = rcf.getResourceConfig(KVMGlobalConfig.RECONNECT_HOST_RESTART_LIBVIRTD_SERVICE.getIdentity());
            // do some hack
            if (new VersionComparator(libvirtVersion).compare("6.0.0.558") >= 0) {
                logger.debug("Libvirt version is greater than 6.0.0-558");
                rc.updateValue(hostUuid, "true");
            } else {
                logger.debug("Libvirt version is less than 6.0.0-558");
                rc.updateValue(hostUuid, "false");
            }
        } else if (isLibvirt620(libvirtVersionWithoutPatch)) {
            logger.debug("Libvirt version is 6.2.0, do some hack");
            ResourceConfig rc = rcf.getResourceConfig(KVMGlobalConfig.RECONNECT_HOST_RESTART_LIBVIRTD_SERVICE.getIdentity());
            // do some hack
            if (new VersionComparator(libvirtVersion).compare("6.2.0.179") >= 0) {
                logger.debug("Libvirt version is greater than 6.2.0-179");
                rc.updateValue(hostUuid, "true");
            } else {
                logger.debug("Libvirt version is less than 6.2.0-179");
                rc.updateValue(hostUuid, "false");
            }
        } else if (isLibvirt800(libvirtVersionWithoutPatch)) {
            logger.debug("Libvirt version is 8.0.0, do some hack");
            ResourceConfig rc = rcf.getResourceConfig(KVMGlobalConfig.RECONNECT_HOST_RESTART_LIBVIRTD_SERVICE.getIdentity());
            if (new VersionComparator(libvirtVersion).compare("8.0.0.72") >= 0) {
                logger.debug("Libvirt version is greater than 8.0.0-72");
                rc.updateValue(hostUuid, "true");
            } else {
                logger.debug("Libvirt version is less than 8.0.0-72");
                rc.updateValue(hostUuid, "false");
            }
        } else {
            logger.debug("Libvirt version is not 4.9.0, 6.0.0 or 8.0.0, do nothing");
        }
    }

    private boolean isLibvirt800(String libvirtVersionWithoutPatch) {
        VersionComparator comparator = new VersionComparator(libvirtVersionWithoutPatch);
        return comparator.compare("8.0.0") == 0;
    }

    private boolean isLibvirt490(String libvirtVersionWithoutPatch) {
        VersionComparator comparator = new VersionComparator(libvirtVersionWithoutPatch);
        return comparator.compare("4.9.0") == 0;
    }

    private boolean isLibvirt600(String libvirtVersionWithoutPatch) {
        VersionComparator comparator = new VersionComparator(libvirtVersionWithoutPatch);
        return comparator.compare("6.0.0") == 0;
    }

    private boolean isLibvirt620(String libvirtVersionWithoutPatch) {
        VersionComparator comparator = new VersionComparator(libvirtVersionWithoutPatch);
        return comparator.compare("6.2.0") == 0;
    }

    @Override
    public boolean start() {
        KVMSystemTags.LIBVIRT_PACKAGE_VERSION.installLifeCycleListener(new SystemTagLifeCycleListener() {
            @Override
            public void tagCreated(SystemTagInventory tag) {
                String libvirtPackageVersion = KVMSystemTags.LIBVIRT_PACKAGE_VERSION.getTokenByTag(tag.getTag(), KVMSystemTags.LIBVIRT_PACKAGE_VERSION_TOKEN);
                enableLibvirtRestartDuringDeployment(libvirtPackageVersion, tag.getResourceUuid());
            }

            @Override
            public void tagDeleted(SystemTagInventory tag) {

            }

            @Override
            public void tagUpdated(SystemTagInventory old, SystemTagInventory newTag) {
                String libvirtPackageVersion = KVMSystemTags.LIBVIRT_PACKAGE_VERSION.getTokenByTag(newTag.getTag(), KVMSystemTags.LIBVIRT_PACKAGE_VERSION_TOKEN);
                enableLibvirtRestartDuringDeployment(libvirtPackageVersion, newTag.getResourceUuid());
            }
        });

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
