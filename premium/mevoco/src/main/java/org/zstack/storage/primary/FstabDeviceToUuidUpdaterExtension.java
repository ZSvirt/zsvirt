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

    private List<HostInfo> getImageStoreHostInfos() {
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

    private List<HostInfo> getLocalStorageHostInfos() {
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
        List<HostInfo> hostInfos = getImageStoreHostInfos();
        hostInfos.addAll(getLocalStorageHostInfos());
        Collections.shuffle(hostInfos);
        hostInfos.forEach(hostInfo -> {
            Ssh ssh = null;
            try {
                ssh = new Ssh();
                ssh.setUsername(hostInfo.username).setPassword(hostInfo.password).setPort(hostInfo.port)
                        .setHostname(hostInfo.hostname).setTimeout(180);

                // 1 Check if the mountPath exists in /etc/fstab
                SshResult mountPathCheck = ssh.command(String.format("grep -Fw '%s' /etc/fstab", hostInfo.mountPath)).run();
                ssh.reset();
                if (mountPathCheck.getReturnCode() != 0) {
                    logger.debug(String.format("mountPath[%s] not found in /etc/fstab on host[type:%s, uuid:%s], skipping UUID update",
                            hostInfo.mountPath, hostInfo.type, hostInfo.uuid));
                    return;
                }

                List<String> entries = Arrays.asList(mountPathCheck.getStdout().split("\n"));
                Ssh finalSsh = ssh;

                for (String entry : entries) {
                    if (entry == null || entry.trim().isEmpty()) {
                        continue;
                    }
                    // 2 Check if the fstab entry already uses UUID
                    String fstabEntry = entry.trim();
                    if (fstabEntry.startsWith("#")) {
                        logger.debug(String.format("skipping commented fstab entry[%s] on host[type:%s, uuid:%s]", fstabEntry, hostInfo.type, hostInfo.uuid));
                        continue;
                    }
                    if (fstabEntry.startsWith("UUID")) {
                        logger.debug(String.format("host[type:%s, uuid:%s] already uses UUID for [%s]", hostInfo.type, hostInfo.uuid, hostInfo.mountPath));
                        continue;
                    }

                    // 3 Extract the device path from the fstab entry.  fstabEntry = '/dev/sda /root/data xfs defaults 0 0'
                    String device = fstabEntry.split("\\s+")[0].trim();

                    // 4 Get the UUID of the device using blkid
                    SshResult blkidResult = finalSsh.command(String.format("blkid -s UUID -o value '%s'", device)).run();
                    finalSsh.reset();
                    if (blkidResult.getReturnCode() != 0 || blkidResult.getStdout().isEmpty()) {
                        logger.warn(String.format("failed to get UUID for device[%s] on host[type:%s, uuid:%s]: %s", device, hostInfo.type, hostInfo.uuid, blkidResult.getStderr()));
                        continue;
                    }
                    String uuid = blkidResult.getStdout().trim();

                    // 5 Backup the current fstab file and Delete the existing fstabEntry using sed
                    String sedCmd = String.format("cp -f /etc/fstab /etc/fstab.bak ; sed -i '\\|^%s|d' /etc/fstab", fstabEntry.replace("/", "\\/"));
                    SshResult delResult = finalSsh.command(sedCmd).run();
                    finalSsh.reset();
                    if (delResult.getReturnCode() != 0) {
                        throw new SshException(String.format("sed[%s] delete failed: %s", sedCmd, delResult.getStderr()));
                    }

                    // 6 Update the fstab entry with UUID
                    String newFstabEntry = fstabEntry.replace(device, String.format("UUID=%s", uuid));

                    String cmd = String.format("grep -Fx -- '%s' /etc/fstab >/dev/null 2>&1 || (echo '%s' >> /etc/fstab && grep -Fx -- '%s' /etc/fstab)",
                            newFstabEntry, newFstabEntry, newFstabEntry);
                    SshResult appendResult = finalSsh.command(cmd).run();
                    finalSsh.reset();
                    if (appendResult.getReturnCode() != 0) {
                        if (appendResult.getStderr().contains("No such file or directory")) {
                            throw new SshException("fstab file not found");
                        } else if (appendResult.getStdout().isEmpty()) {
                            throw new SshException("failed to add and verify new entry");
                        } else {
                            throw new SshException("command failed: " + appendResult.getStderr());
                        }
                    }
                    logger.info(String.format("updated fstab for device[%s] to UUID[%s] on host[type:%s, uuid:%s]", device, uuid, hostInfo.type, hostInfo.uuid));
                }
            } catch (Exception e) {
                logger.warn(String.format("fstab update failed on host[type:%s, uuid:%s]: %s. attempting rollback", hostInfo.type, hostInfo.uuid, e.getMessage()));

                if (ssh != null) {
                    SshResult rollback = ssh.command("[ -f /etc/fstab.bak ] && mv -f /etc/fstab.bak /etc/fstab").run();
                    ssh.reset();
                    if (rollback.getReturnCode() != 0) {
                        logger.error(String.format("rollback failed on host[type:%s, uuid:%s] ", hostInfo.type, hostInfo.uuid));
                    }
                }
            } finally {
                if (ssh != null) {
                    ssh.close();
                }
            }
        });
    }
}