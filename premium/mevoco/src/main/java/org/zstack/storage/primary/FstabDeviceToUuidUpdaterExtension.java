package org.zstack.storage.primary;


import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.Q;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.kvm.KVMHostVO;
import org.zstack.kvm.KVMHostVO_;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageVO;
import org.zstack.storage.primary.local.LocalStorageConstants;
import org.zstack.storage.primary.local.LocalStorageHostRefVO;
import org.zstack.storage.primary.local.LocalStorageHostRefVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshException;
import org.zstack.utils.ssh.SshResult;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.zstack.storage.primary.FstabDeviceToUuidUpdaterGlobalProperty.FSTAB_DEVICE_TO_UUID_UPDATER;


@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class FstabDeviceToUuidUpdaterExtension implements ManagementNodeReadyExtensionPoint {
    protected static final CLogger logger = Utils.getLogger(FstabDeviceToUuidUpdaterExtension.class);

    @Override
    public void managementNodeReady() {
        if (FSTAB_DEVICE_TO_UUID_UPDATER) {
            fstabDeviceToUuidUpdater();
        }
    }

    static class HostInfo {
        String hostname;
        String username;
        String password;
        int port = 22;
        String uuid;
        String type;
        String mountPath;
    }

    static List<HostInfo> findImageStoreHostInfos() {
        List<HostInfo> hostInfos = new ArrayList<>();

        List<ImageStoreBackupStorageVO> imageStores = Q.New(ImageStoreBackupStorageVO.class).list();
        imageStores.forEach(imageStore -> {
            if (imageStore.getHostname() == null || imageStore.getUrl() == null) {
                logger.warn(String.format("skipping image store[uuid:%s] due to missing hostname or url", imageStore.getUuid()));
                return;
            }
            HostInfo hostInfo = new HostInfo();
            hostInfo.hostname = imageStore.getHostname();
            hostInfo.username = imageStore.getUsername();
            hostInfo.password = imageStore.getPassword();
            hostInfo.port = imageStore.getSshPort();
            hostInfo.uuid = imageStore.getUuid();
            hostInfo.type = imageStore.getResourceType();
            hostInfo.mountPath = imageStore.getUrl();
            hostInfos.add(hostInfo);
        });
        return hostInfos;
    }

    static List<HostInfo> findLocalStorageHostInfos() {
        List<HostInfo> hostInfos = new ArrayList<>();

        List<Tuple> tuples = Q.New(PrimaryStorageVO.class)
                .eq(PrimaryStorageVO_.type, LocalStorageConstants.LOCAL_STORAGE_TYPE)
                .select(PrimaryStorageVO_.uuid, PrimaryStorageVO_.mountPath).listTuple();

        tuples.forEach(tuple -> {
            String psUuid = tuple.get(0, String.class);
            String mountPath = tuple.get(1, String.class);

            List<String> hostUuids = Q.New(LocalStorageHostRefVO.class)
                    .eq(LocalStorageHostRefVO_.primaryStorageUuid, psUuid)
                    .select(LocalStorageHostRefVO_.hostUuid).listValues();
            if (hostUuids.isEmpty()) {
                logger.debug(String.format("no hosts found for primary storage[uuid:%s, path:%s]", psUuid, mountPath));
                return;
            }

            List<KVMHostVO> hostVOS = Q.New(KVMHostVO.class).in(KVMHostVO_.uuid, hostUuids).list();
            hostVOS.forEach(hostVO -> {
                if (hostVO.getManagementIp() == null) {
                    logger.warn(String.format("skipping host[uuid:%s] due to missing management IP", hostVO.getUuid()));
                    return;
                }

                HostInfo hostInfo = new HostInfo();
                hostInfo.hostname = hostVO.getManagementIp();
                hostInfo.username = hostVO.getUsername();
                hostInfo.password = hostVO.getPassword();
                hostInfo.port = hostVO.getPort();
                hostInfo.uuid = hostVO.getUuid();
                hostInfo.type = hostVO.getResourceType();
                hostInfo.mountPath = mountPath;
                hostInfos.add(hostInfo);
            });
        });
        return hostInfos;
    }

    @AsyncThread
    private void fstabDeviceToUuidUpdater() {
        List<HostInfo> hostInfos = findImageStoreHostInfos();
        hostInfos.addAll(findLocalStorageHostInfos());
        Collections.shuffle(hostInfos);
        hostInfos.forEach(hostInfo -> {
            Ssh ssh = null;
            try {
                ssh = new Ssh();
                ssh.setUsername(hostInfo.username).setPassword(hostInfo.password).setPort(hostInfo.port)
                        .setHostname(hostInfo.hostname).setTimeout(180);

                // 1 Check if the mountPath exists in /etc/fstab
                SshResult mountPathCheck = ssh.command(buildFindFstabEntriesCmd(hostInfo.mountPath)).run();
                ssh.reset();
                if (isFstabEntryMissing(mountPathCheck)) {
                    logger.debug(String.format("mountPath[%s] not found in /etc/fstab on host[type:%s, uuid:%s], skipping UUID update",
                            hostInfo.mountPath, hostInfo.type, hostInfo.uuid));
                    return;
                }

                updateFstabEntries(ssh, mountPathCheck.getStdout(), hostInfo.mountPath, hostInfo.type, hostInfo.uuid);
            } catch (Exception e) {
                logger.warn(String.format("fstab update failed on host[type:%s, uuid:%s]: %s", hostInfo.type, hostInfo.uuid, e.getMessage()));
            } finally {
                if (ssh != null) {
                    ssh.close();
                }
            }
        });
    }

    static void updateFstabEntries(Ssh ssh, String entriesOutput, String mountPath, String hostType, String hostUuid) {
        List<String> entries = Arrays.asList(entriesOutput.split("\n"));
        for (String entry : entries) {
            if (entry == null || entry.trim().isEmpty()) {
                continue;
            }
            String oldFstabEntry = entry;
            String fstabEntry = entry.trim();
            if (fstabEntry.startsWith("#")) {
                logger.debug(String.format("skipping commented fstab entry[%s] on host[type:%s, uuid:%s]", fstabEntry, hostType, hostUuid));
                continue;
            }

            String[] fields = fstabEntry.split("\\s+");
            if (fields.length < 4) {
                logger.warn(String.format("skipping invalid fstab entry[%s] on host[type:%s, uuid:%s]", fstabEntry, hostType, hostUuid));
                continue;
            }
            if (!fields[1].equals(encodeFstabField(mountPath))) {
                logger.warn(String.format("skipping fstab entry[%s] because its mount path does not match [%s] on host[type:%s, uuid:%s]",
                        fstabEntry, mountPath, hostType, hostUuid));
                continue;
            }

            String device = fields[0].trim();
            if (device.startsWith("UUID=")) {
                String newFstabEntry = FstabNonBlockingMountOptionsUpdaterExtension.buildFstabEntryWithNonBlockingOptions(fstabEntry);
                if (newFstabEntry.equals(fstabEntry)) {
                    logger.debug(String.format("host[type:%s, uuid:%s] already uses UUID for [%s]", hostType, hostUuid, mountPath));
                    continue;
                }
                updateFstabEntry(ssh, oldFstabEntry, newFstabEntry);
                logger.info(String.format("updated fstab non-blocking options for UUID entry on host[type:%s, uuid:%s]", hostType, hostUuid));
                continue;
            }

            SshResult blkidResult = ssh.command(String.format("blkid -s UUID -o value '%s'", device)).run();
            ssh.reset();
            String uuid = blkidResult.getStdout() == null ? "" : blkidResult.getStdout().trim();
            if (blkidResult.getReturnCode() != 0 || uuid.isEmpty()) {
                logger.warn(String.format("failed to get UUID for device[%s] on host[type:%s, uuid:%s]: %s", device, hostType, hostUuid, blkidResult.getStderr()));
                String newFstabEntry = FstabNonBlockingMountOptionsUpdaterExtension.buildFstabEntryWithNonBlockingOptions(fstabEntry);
                if (!newFstabEntry.equals(fstabEntry)) {
                    updateFstabEntry(ssh, oldFstabEntry, newFstabEntry);
                    logger.info(String.format("updated fstab non-blocking options for device[%s] on host[type:%s, uuid:%s]", device, hostType, hostUuid));
                }
                continue;
            }

            String newFstabEntry = FstabNonBlockingMountOptionsUpdaterExtension.buildUuidFstabEntryWithNonBlockingOptions(fstabEntry, uuid);
            updateFstabEntry(ssh, oldFstabEntry, newFstabEntry);
            logger.info(String.format("updated fstab for device[%s] to UUID[%s] on host[type:%s, uuid:%s]", device, uuid, hostType, hostUuid));
        }
    }

    static String buildFindFstabEntriesCmd(String mountPath) {
        return String.format("awk 'BEGIN { mount_path=ARGV[1]; ARGV[1]=\"\" } $1 !~ /^#/ && $2 == mount_path { print; found=1 } END { exit found ? 0 : 1 }' %s /etc/fstab",
                shellArg(encodeFstabField(mountPath)));
    }

    static String encodeFstabField(String field) {
        return field.replace("\\", "\\134")
                .replace(" ", "\\040")
                .replace("\t", "\\011")
                .replace("\n", "\\012");
    }

    static boolean isFstabEntryMissing(SshResult result) {
        if (result.isSshFailure() || (result.getReturnCode() != 0 && result.getReturnCode() != 1)) {
            throw new SshException(String.format("failed to scan /etc/fstab, return code: %s, stderr: %s",
                    result.getReturnCode(), result.getStderr()));
        }
        return result.getReturnCode() == 1;
    }

    static String buildUpdateFstabEntryCmd(String oldFstabEntry, String newFstabEntry) {
        if (oldFstabEntry.endsWith("\r")) {
            oldFstabEntry = oldFstabEntry.substring(0, oldFstabEntry.length() - 1);
        }
        String backupPath = String.format("/etc/fstab.zstack.%s.bak", UUID.randomUUID().toString());
        return String.join("\n",
                "set -e",
                String.format("backup=%s", shellArg(backupPath)),
                "cp -a -- /etc/fstab \"$backup\"",
                String.format("old_entry=%s", shellArg(oldFstabEntry)),
                String.format("new_entry=%s", shellArg(newFstabEntry)),
                "tmp=$(mktemp /etc/fstab.zstack.XXXXXX)",
                "trap 'rm -f \"$tmp\"' EXIT",
                "cp -a -- /etc/fstab \"$tmp\"",
                "OLD_ENTRY=\"$old_entry\" NEW_ENTRY=\"$new_entry\" awk 'BEGIN { old_entry=ENVIRON[\"OLD_ENTRY\"]; new_entry=ENVIRON[\"NEW_ENTRY\"] } { line=$0; sub(/\\r$/, \"\", line); if (line == old_entry) next; if (line == new_entry) found=1; print $0 } END { if (!found) print new_entry }' /etc/fstab > \"$tmp\"",
                "NEW_ENTRY=\"$new_entry\" awk 'BEGIN { new_entry=ENVIRON[\"NEW_ENTRY\"] } { line=$0; sub(/\\r$/, \"\", line); if (line == new_entry) found=1 } END { exit found ? 0 : 1 }' \"$tmp\"",
                "mv -f -- \"$tmp\" /etc/fstab || { cp -a -- \"$backup\" /etc/fstab; exit 1; }",
                "rm -f \"$backup\" || true"
        );
    }

    static String shellArg(String value) {
        return Ssh.shellQuote(value);
    }

    static void updateFstabEntry(Ssh ssh, String oldFstabEntry, String newFstabEntry) {
        String cmd = buildUpdateFstabEntryCmd(oldFstabEntry, newFstabEntry);
        SshResult updateResult = ssh.command(String.format("bash -c %s", shellArg(cmd))).run();
        ssh.reset();
        if (updateResult.getReturnCode() != 0) {
            if (updateResult.getStderr().contains("No such file or directory")) {
                throw new SshException("fstab file not found");
            }
            throw new SshException(String.format("fstab update command failed: %s", updateResult.getStderr()));
        }
    }
}
