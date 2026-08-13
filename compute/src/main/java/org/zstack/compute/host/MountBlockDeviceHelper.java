package org.zstack.compute.host;

import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.APIMountBlockDeviceMsg;
import org.zstack.header.host.BlockDevices;
import org.zstack.header.host.BlockDevicesParser;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshResult;

import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.header.host.APIMountBlockDeviceMsg.mkfsCommd.buildMkfsCommd;
import static org.zstack.header.host.BlockDevicesParser.getBlockDevicesCommand;

final class MountBlockDeviceHelper {
    private static final CLogger logger = Utils.getLogger(MountBlockDeviceHelper.class);

    static final String LOG_PREFIX = "[MountBlockDevice]";
    private static final String FSTAB_SCAN_NO_UUID_SENTINEL = "__zs_no_uuid__";
    private static final String FSTAB_OPTIONS_MULTIPATH = "defaults,_netdev,nofail,x-systemd.device-timeout=10";
    private static final String FSTAB_OPTIONS_DEFAULT = "defaults,nofail,x-systemd.device-timeout=10";
    private static final String MULTIPATH_PREFIX = "/dev/mapper/mpath";

    private MountBlockDeviceHelper() {
    }

    enum Phase {
        IDEMPOTENT("idempotent"),
        IDEMPOTENT_POST_REDIRECT("idempotent-post-redirect");

        private final String label;

        Phase(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }
    }

    static final class MountRequest {
        final String hostName;
        final int sshPort;
        final String username;
        final String path;
        final String mountPoint;
        final String filesystemType;

        MountRequest(String hostName, int sshPort, String username,
                     String path, String mountPoint, String filesystemType) {
            this.hostName = hostName;
            this.sshPort = sshPort;
            this.username = username;
            this.path = path;
            this.mountPoint = mountPoint;
            this.filesystemType = filesystemType;
        }

        static MountRequest from(APIMountBlockDeviceMsg msg) {
            return new MountRequest(msg.getHostName(), msg.getSshPort(), msg.getUsername(),
                    msg.getPath(), msg.getMountPoint(), msg.getFilesystemType());
        }
    }

    static final class DeviceResolution {
        final String devicePath;
        final boolean isMultipath;
        final boolean redirected;

        DeviceResolution(String devicePath, boolean isMultipath, boolean redirected) {
            this.devicePath = devicePath;
            this.isMultipath = isMultipath;
            this.redirected = redirected;
        }
    }

    static final class MountState {
        String uuid;
        boolean mounted;
        boolean fstabAppended;
    }

    static void logStart(MountRequest req) {
        logger.info(String.format(
                "%s phase=start host=%s:%d user=%s path=%s mountPoint=%s fsType=%s",
                LOG_PREFIX, req.hostName, req.sshPort, req.username,
                req.path, req.mountPoint, req.filesystemType));
    }

    static boolean checkMountPointIdempotent(Ssh ssh, MountRequest req) {
        SshResult mountPointCheck = ssh.command(
                String.format("findmnt -n -o SOURCE '%s'", req.mountPoint)).run();
        ssh.reset();
        String stdout = mountPointCheck.getStdout() == null ? "" : mountPointCheck.getStdout().trim();
        if (mountPointCheck.getReturnCode() != 0 || stdout.isEmpty()) {
            return false;
        }
        String currentSource = stdout;
        String requestedUuid = readDeviceUuid(ssh, req.path);
        String currentUuid = readDeviceUuid(ssh, currentSource);
        if (requestedUuid.isEmpty() || !requestedUuid.equals(currentUuid)) {
            throw new OperationFailureException(operr("mountPoint %s is already mount on device %s",
                    req.mountPoint, currentSource));
        }
        boolean isMultipath = currentSource.startsWith(MULTIPATH_PREFIX);
        ensureFstabEntry(ssh, req, currentSource, currentUuid, isMultipath, Phase.IDEMPOTENT);
        logger.info(String.format(
                "%s phase=idempotent host=%s mountPoint=%s already bound to requested device uuid=%s source=%s",
                LOG_PREFIX, req.hostName, req.mountPoint, currentUuid, currentSource));
        return true;
    }

    static void ensureDeviceNotAlreadyMounted(Ssh ssh, MountRequest req) {
        SshResult devicePathCheck = ssh.command(
                String.format("findmnt -n -o TARGET '%s'", req.path)).run();
        ssh.reset();
        String stdout = devicePathCheck.getStdout() == null ? "" : devicePathCheck.getStdout().trim();
        if (devicePathCheck.getReturnCode() == 0 && !stdout.isEmpty()) {
            throw new OperationFailureException(operr("device %s is already mount on mountPoint %s",
                    req.path, stdout));
        }
    }

    static DeviceResolution resolveEffectiveDevice(Ssh ssh, MountRequest req) {
        SshResult listRet = ssh.command(getBlockDevicesCommand()).run();
        ssh.reset();
        if (listRet.getReturnCode() != 0) {
            throw new OperationFailureException(operr(
                    "failed to list block devices on host %s, stderr:%s",
                    req.hostName, listRet.getStderr()));
        }
        BlockDevices allDevices = BlockDevices.valueOf(BlockDevicesParser.parse(listRet.getStdout()));
        BlockDevices.BlockDevice target = BlockDevices.findByName(allDevices.getAllBlockDevices(), req.path);
        if (target == null) {
            List<String> availableNames = allDevices.getAllBlockDevices().stream()
                    .map(BlockDevices.BlockDevice::getName)
                    .collect(Collectors.toList());
            throw new OperationFailureException(operr(
                    "device %s not found on host %s, available devices: %s",
                    req.path, req.hostName, availableNames));
        }

        boolean isMultipath = target.isMultipath();
        boolean isUnderlyingMultipathMember = isMultipath && "disk".equalsIgnoreCase(target.getType());
        if (!isUnderlyingMultipathMember) {
            return new DeviceResolution(req.path, isMultipath, false);
        }

        BlockDevices.BlockDevice mpathChild = BlockDevices.findMultipathChild(target);
        if (mpathChild == null || mpathChild.getName() == null || mpathChild.getName().isEmpty()) {
            throw new OperationFailureException(operr(
                    "device %s is an underlying path of a multipath device but the "
                            + "aggregator (/dev/mapper/mpathX) could not be located",
                    req.path));
        }
        logger.warn(String.format(
                "%s phase=redirect host=%s requested=%s effective=%s reason=underlying-multipath-member",
                LOG_PREFIX, req.hostName, req.path, mpathChild.getName()));
        return new DeviceResolution(mpathChild.getName(), true, true);
    }

    static boolean checkPostRedirectIdempotent(Ssh ssh, MountRequest req, DeviceResolution resolved) {
        if (!resolved.redirected) {
            return false;
        }
        SshResult redirectedCheck = ssh.command(
                String.format("findmnt -n -o TARGET '%s'", resolved.devicePath)).run();
        ssh.reset();
        String stdout = redirectedCheck.getStdout() == null ? "" : redirectedCheck.getStdout().trim();
        if (redirectedCheck.getReturnCode() != 0 || stdout.isEmpty()) {
            return false;
        }
        String currentTarget = stdout;
        if (!currentTarget.equals(req.mountPoint)) {
            throw new OperationFailureException(operr("device %s is already mount on mountPoint %s",
                    resolved.devicePath, currentTarget));
        }
        String aggUuid = readDeviceUuid(ssh, resolved.devicePath);
        ensureFstabEntry(ssh, req, resolved.devicePath, aggUuid, resolved.isMultipath,
                Phase.IDEMPOTENT_POST_REDIRECT);
        logger.info(String.format(
                "%s phase=idempotent-post-redirect host=%s aggregator=%s already mounted at %s uuid=%s",
                LOG_PREFIX, req.hostName, resolved.devicePath, currentTarget, aggUuid));
        return true;
    }

    static void prepareMountPoint(Ssh ssh, MountRequest req) {
        executeSshCommand(ssh, String.format("mkdir -p '%s'", req.mountPoint));
    }

    static void validateFstabClean(Ssh ssh, MountRequest req, String devicePath) {
        String existingUuid = readDeviceUuid(ssh, devicePath);
        String uuidLiteral = existingUuid.isEmpty() ? FSTAB_SCAN_NO_UUID_SENTINEL : "UUID=" + existingUuid;
        SshResult staleCheck = ssh.command(String.format(
                "awk -v d='%s' -v u='%s' -v m='%s' "
                        + "'$1!~/^#/ && ($1==d || $1==u || $2==m) {print NR\": \"$0}' /etc/fstab",
                devicePath, uuidLiteral, req.mountPoint)).run();
        ssh.reset();
        if (staleCheck.getReturnCode() != 0) {
            throw new OperationFailureException(operr(
                    "failed to scan /etc/fstab on host %s, stderr:%s",
                    req.hostName, staleCheck.getStderr()));
        }
        String stale = staleCheck.getStdout() == null ? "" : staleCheck.getStdout().trim();
        if (!stale.isEmpty()) {
            throw new OperationFailureException(operr(
                    "fstab on host %s already contains entry referencing device %s, uuid %s or mountPoint %s. "
                            + "matched lines: [%s]. mkfs/mount aborted to protect existing data. "
                            + "Please clean up /etc/fstab on the host and retry.",
                    req.hostName, devicePath, uuidLiteral, req.mountPoint, stale));
        }
    }

    static void doFormatAndMount(Ssh ssh, MountRequest req, DeviceResolution resolved) {
        executeSshCommand(ssh, buildMkfsCommd(req.filesystemType, resolved.devicePath));

        MountState state = new MountState();
        try {
            SshResult ret = executeSshCommand(ssh,
                    String.format("blkid -s UUID -o value '%s'", resolved.devicePath));
            state.uuid = ret.getStdout().trim();
            if (state.uuid.isEmpty()) {
                throw new OperationFailureException(operr(
                        "failed to get UUID for device %s", resolved.devicePath));
            }

            executeSshCommand(ssh, String.format("mount -U '%s' '%s'", state.uuid, req.mountPoint));
            state.mounted = true;

            String fstabOptions = resolved.isMultipath ? FSTAB_OPTIONS_MULTIPATH : FSTAB_OPTIONS_DEFAULT;
            String fstabEntry = String.format("UUID=%s %s %s %s 0 0",
                    state.uuid, req.mountPoint, req.filesystemType, fstabOptions);
            executeSshCommand(ssh, String.format("echo '%s' >> /etc/fstab", fstabEntry));
            state.fstabAppended = true;

            logger.info(String.format(
                    "%s phase=done host=%s path=%s effective=%s mountPoint=%s uuid=%s isMultipath=%s",
                    LOG_PREFIX, req.hostName, req.path, resolved.devicePath,
                    req.mountPoint, state.uuid, resolved.isMultipath));
        } catch (RuntimeException e) {
            // only compensate; caller still gets the original failure
            compensateAfterMkfs(ssh, req, resolved, state);
            throw e;
        }
    }

    private static void ensureFstabEntry(Ssh ssh, MountRequest req, String devicePath,
                                         String uuid, boolean isMultipath, Phase phase) {
        if (uuid == null || uuid.isEmpty()) {
            logger.warn(String.format(
                    "%s phase=%s host=%s devicePath=%s: no UUID readable, skip fstab ensure "
                            + "(admin should verify /etc/fstab manually)",
                    LOG_PREFIX, phase.label(), req.hostName, devicePath));
            return;
        }
        try {
            SshResult grep = ssh.command(String.format(
                    "grep -E '^UUID=%s[[:space:]]' /etc/fstab || true", uuid)).run();
            ssh.reset();
            String grepOut = grep.getStdout() == null ? "" : grep.getStdout().trim();
            if (!grepOut.isEmpty()) {
                return;
            }
            String fstabOptions = isMultipath ? FSTAB_OPTIONS_MULTIPATH : FSTAB_OPTIONS_DEFAULT;
            String fstabEntry = String.format("UUID=%s %s %s %s 0 0",
                    uuid, req.mountPoint, req.filesystemType, fstabOptions);
            SshResult append = ssh.command(String.format("echo '%s' >> /etc/fstab", fstabEntry)).run();
            ssh.reset();
            if (append.getReturnCode() != 0) {
                logger.warn(String.format(
                        "%s phase=%s host=%s failed to append fstab entry uuid=%s stderr=%s",
                        LOG_PREFIX, phase.label(), req.hostName, uuid, append.getStderr()));
                return;
            }
            logger.info(String.format(
                    "%s phase=%s-fstab-backfill host=%s appended missing fstab entry uuid=%s mountPoint=%s",
                    LOG_PREFIX, phase.label(), req.hostName, uuid, req.mountPoint));
        } catch (Exception e) {
            logger.warn(String.format(
                    "%s phase=%s host=%s fstab ensure threw: %s",
                    LOG_PREFIX, phase.label(), req.hostName, e.getMessage()));
        }
    }

    private static String readDeviceUuid(Ssh ssh, String devicePath) {
        try {
            SshResult ret = ssh.command(String.format(
                    "blkid -s UUID -o value '%s' 2>/dev/null || true", devicePath)).run();
            ssh.reset();
            if (ret.getReturnCode() != 0) {
                return "";
            }
            return ret.getStdout() == null ? "" : ret.getStdout().trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static void compensateAfterMkfs(Ssh ssh, MountRequest req,
                                            DeviceResolution resolved, MountState state) {
        logger.warn(String.format(
                "%s phase=compensate host=%s devicePath=%s mountPoint=%s (fs overwritten, not reversible)",
                LOG_PREFIX, req.hostName, resolved.devicePath, req.mountPoint));

        if (state.mounted) {
            try {
                SshResult ret = ssh.command(String.format("umount '%s'", req.mountPoint)).run();
                ssh.reset();
                if (ret.getReturnCode() != 0) {
                    logger.warn(String.format(
                            "%s phase=compensate-umount host=%s mountPoint=%s stderr=%s",
                            LOG_PREFIX, req.hostName, req.mountPoint, ret.getStderr()));
                }
            } catch (Exception e) {
                logger.warn(String.format(
                        "%s phase=compensate-umount host=%s mountPoint=%s err=%s",
                        LOG_PREFIX, req.hostName, req.mountPoint, e.getMessage()));
            }
        }

        if (state.fstabAppended && state.uuid != null && !state.uuid.isEmpty()) {
            try {
                SshResult r = ssh.command(String.format(
                        "sed -i.zsbak '/^UUID=%s[[:space:]]/d' /etc/fstab", state.uuid)).run();
                ssh.reset();
                if (r.getReturnCode() != 0) {
                    logger.warn(String.format(
                            "%s phase=compensate-fstab host=%s uuid=%s stderr=%s",
                            LOG_PREFIX, req.hostName, state.uuid, r.getStderr()));
                }
            } catch (Exception e) {
                logger.warn(String.format(
                        "%s phase=compensate-fstab host=%s uuid=%s err=%s",
                        LOG_PREFIX, req.hostName, state.uuid, e.getMessage()));
            }
        }
    }

    private static SshResult executeSshCommand(Ssh ssh, String command) {
        SshResult ret = ssh.command(command).run();
        ssh.reset();
        if (ret.getReturnCode() != 0) {
            throw new CloudRuntimeException(String.format(
                    "SSH command failed [%s]: stderr=%s, stdout=%s",
                    command, ret.getStderr(), ret.getStdout()));
        }
        return ret;
    }
}
