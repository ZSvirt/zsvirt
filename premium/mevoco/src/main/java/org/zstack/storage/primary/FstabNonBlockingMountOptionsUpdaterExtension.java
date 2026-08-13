package org.zstack.storage.primary;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.zstack.storage.primary.FstabDeviceToUuidUpdaterGlobalProperty.FSTAB_DEVICE_TO_UUID_UPDATER;
import static org.zstack.storage.primary.FstabDeviceToUuidUpdaterGlobalProperty.FSTAB_NON_BLOCKING_MOUNT_OPTIONS_UPDATER;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class FstabNonBlockingMountOptionsUpdaterExtension implements ManagementNodeReadyExtensionPoint {
    protected static final CLogger logger = Utils.getLogger(FstabNonBlockingMountOptionsUpdaterExtension.class);
    private static final List<String> NON_BLOCKING_MOUNT_OPTIONS = Arrays.asList("nofail", "x-systemd.device-timeout=10");

    @Override
    public void managementNodeReady() {
        if (!FSTAB_NON_BLOCKING_MOUNT_OPTIONS_UPDATER || FSTAB_DEVICE_TO_UUID_UPDATER) {
            return;
        }
        fstabNonBlockingMountOptionsUpdater();
    }

    public static String normalizeMountOptions(String options) {
        List<String> normalized = new ArrayList<>();
        if (options != null) {
            for (String option : options.split(",")) {
                String trimmed = option.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("x-systemd.device-timeout=")) {
                    continue;
                }
                if (!normalized.contains(trimmed)) {
                    normalized.add(trimmed);
                }
            }
        }

        if (normalized.isEmpty()) {
            normalized.add("defaults");
        }

        for (String option : NON_BLOCKING_MOUNT_OPTIONS) {
            if (!normalized.contains(option)) {
                normalized.add(option);
            }
        }

        return String.join(",", normalized);
    }

    public static String buildFstabEntryWithNonBlockingOptions(String fstabEntry) {
        String[] fields = fstabEntry.trim().split("\\s+");
        if (fields.length < 4) {
            return fstabEntry;
        }

        fields[3] = normalizeMountOptions(fields[3]);
        return String.join(" ", fields);
    }

    public static String buildUuidFstabEntryWithNonBlockingOptions(String fstabEntry, String uuid) {
        String[] fields = fstabEntry.trim().split("\\s+");
        if (fields.length < 4) {
            return fstabEntry;
        }

        fields[0] = String.format("UUID=%s", uuid);
        fields[3] = normalizeMountOptions(fields[3]);
        return String.join(" ", fields);
    }

    @AsyncThread
    private void fstabNonBlockingMountOptionsUpdater() {
        List<FstabDeviceToUuidUpdaterExtension.HostInfo> hostInfos = FstabDeviceToUuidUpdaterExtension.findImageStoreHostInfos();
        hostInfos.addAll(FstabDeviceToUuidUpdaterExtension.findLocalStorageHostInfos());
        Collections.shuffle(hostInfos);
        hostInfos.forEach(hostInfo -> {
            Ssh ssh = null;
            try {
                ssh = new Ssh();
                ssh.setUsername(hostInfo.username).setPassword(hostInfo.password).setPort(hostInfo.port)
                        .setHostname(hostInfo.hostname).setTimeout(180);

                SshResult mountPathCheck = ssh.command(FstabDeviceToUuidUpdaterExtension.buildFindFstabEntriesCmd(hostInfo.mountPath)).run();
                ssh.reset();
                if (FstabDeviceToUuidUpdaterExtension.isFstabEntryMissing(mountPathCheck)) {
                    logger.debug(String.format("mountPath[%s] not found in /etc/fstab on host[type:%s, uuid:%s], skipping non-blocking option update",
                            hostInfo.mountPath, hostInfo.type, hostInfo.uuid));
                    return;
                }

                for (String entry : Arrays.asList(mountPathCheck.getStdout().split("\n"))) {
                    if (entry == null || entry.trim().isEmpty()) {
                        continue;
                    }
                    String oldFstabEntry = entry;
                    String fstabEntry = entry.trim();
                    if (fstabEntry.startsWith("#")) {
                        logger.debug(String.format("skipping commented fstab entry[%s] on host[type:%s, uuid:%s]", fstabEntry, hostInfo.type, hostInfo.uuid));
                        continue;
                    }

                    String[] fields = fstabEntry.split("\\s+");
                    if (fields.length < 4) {
                        logger.warn(String.format("skipping invalid fstab entry[%s] on host[type:%s, uuid:%s]", fstabEntry, hostInfo.type, hostInfo.uuid));
                        continue;
                    }
                    if (!fields[1].equals(FstabDeviceToUuidUpdaterExtension.encodeFstabField(hostInfo.mountPath))) {
                        logger.warn(String.format("skipping fstab entry[%s] because its mount path does not match [%s] on host[type:%s, uuid:%s]",
                                fstabEntry, hostInfo.mountPath, hostInfo.type, hostInfo.uuid));
                        continue;
                    }

                    String newFstabEntry = buildFstabEntryWithNonBlockingOptions(fstabEntry);
                    if (newFstabEntry.equals(fstabEntry)) {
                        logger.debug(String.format("host[type:%s, uuid:%s] already uses non-blocking fstab options for [%s]",
                                hostInfo.type, hostInfo.uuid, hostInfo.mountPath));
                        continue;
                    }

                    FstabDeviceToUuidUpdaterExtension.updateFstabEntry(ssh, oldFstabEntry, newFstabEntry);
                    logger.info(String.format("updated fstab non-blocking options for mountPath[%s] on host[type:%s, uuid:%s]",
                            hostInfo.mountPath, hostInfo.type, hostInfo.uuid));
                }
            } catch (Exception e) {
                logger.warn(String.format("fstab non-blocking option update failed on host[type:%s, uuid:%s]: %s",
                        hostInfo.type, hostInfo.uuid, e.getMessage()));
            } finally {
                if (ssh != null) {
                    ssh.close();
                }
            }
        });
    }
}
