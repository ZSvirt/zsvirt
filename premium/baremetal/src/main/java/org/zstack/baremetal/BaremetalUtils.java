package org.zstack.baremetal;

import org.apache.commons.codec.digest.Crypt;
import org.apache.commons.lang.StringUtils;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.defer.Defer;
import org.zstack.core.defer.Deferred;
import org.zstack.header.baremetal.chassis.BaremetalChassisConstant;
import org.zstack.header.baremetal.chassis.BaremetalChassisInventory;
import org.zstack.header.baremetal.chassis.BaremetalChassisSystemTags;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by GuoYi on 4/26/17.
 */
public class BaremetalUtils {
    private static final CLogger logger = Utils.getLogger(BaremetalUtils.class);

    public static boolean isValidMacAddress(String macAddress) {
        final String MAC_REGEX = "^([A-Fa-f0-9]{2}[-,:]){5}[A-Fa-f0-9]{2}$";
        return macAddress != null && !macAddress.isEmpty() && (macAddress.matches(MAC_REGEX));
    }

    public static boolean isBelongToSameSubnet(String addr1, String addr2, String mask) {
        byte[] addr1Byte = new byte[0];
        byte[] addr2Byte = new byte[0];
        byte[] maskByte = new byte[0];
        try {
            addr1Byte = InetAddress.getByName(addr1).getAddress();
            addr2Byte = InetAddress.getByName(addr2).getAddress();
            maskByte = InetAddress.getByName(mask).getAddress();
        } catch (UnknownHostException e) {
            return false;
        }

        for (int i = 0; i < addr1Byte.length; i++)
            if ((addr1Byte[i] & maskByte[i]) != (addr2Byte[i] & maskByte[i]))
                return false;
        return true;
    }

    public static boolean isIpmiServerReachable(BaremetalChassisInventory bmc) {
        String status = getServerPowerStatus(bmc);
        return !status.equals(BaremetalChassisConstant.SERVER_POWER_UNKNOWN);
    }

    public static boolean isIpmiServerReachable(String addr, Integer port, String username, String password) {
        String status = getServerPowerStatus(addr, port, username, password);
        return !status.equals(BaremetalChassisConstant.SERVER_POWER_UNKNOWN);
    }

    private static class IPMIToolCaller {
        private final String interfaceToUse = "lanplus";
        public String hostname;
        public int port;
        public String username;
        public String password;
        public String passFile;
        public boolean efiBoot;

        public static IPMIToolCaller fromBaremetalChassisInventory(BaremetalChassisInventory bmc) {
            IPMIToolCaller caller = new IPMIToolCaller();
            caller.hostname = bmc.getIpmiAddress();
            caller.port = bmc.getIpmiPort();
            caller.username = bmc.getIpmiUsername();
            caller.password = bmc.getIpmiPassword();
            caller.efiBoot = !BaremetalChassisSystemTags.LEGACY_BOOT.hasTag(bmc.getUuid());
            return caller;
        }

        private String buildBaseCmd() {
            passFile = PathUtil.createTempFileWithContent(password);
            return String.format("ipmitool -I %s -H '%s' -p '%d' -U '%s' -f '%s'",
                    interfaceToUse, hostname, port, username, passFile);
        }

        private ShellResult status() {
            return runWithResult("chassis power status");
        }

        private int powerOff() {
            return runWithReturnCode("chassis power off");
        }

        private int powerOn() {
            return runWithReturnCode("chassis power on");
        }

        private int powerReset() {
            return runWithReturnCode("chassis power reset");
        }

        private int setBootDev(String bootDev) {
            // always use 'options=efiboot' to deal with Legacy/UEFI at the same time
            String options = efiBoot ? " options=efiboot" : "";
            return runWithReturnCode(String.format("chassis bootdev %s%s", bootDev, options));
        }

        @Deferred
        public ShellResult runWithResult(String command) {
            DebugUtils.Assert(command != null, "command should be set before execution");
            Defer.defer(() -> PathUtil.forceRemoveFile(passFile));
            return ShellUtils.runAndReturn(String.format("%s %s", buildBaseCmd(), command));
        }

        @Deferred
        public int runWithReturnCode(String command) {
            DebugUtils.Assert(command != null, "command should be set before execution");
            Defer.defer(() -> PathUtil.forceRemoveFile(passFile));
            return ShellUtils.runAndReturn(String.format("%s %s", buildBaseCmd(), command)).getRetCode();
        }
    }

    public static boolean setServerBootDevice(BaremetalChassisInventory bmc, String bootDev) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return true;
        }

        return 0 == IPMIToolCaller.fromBaremetalChassisInventory(bmc)
                .setBootDev(bootDev);
    }

    public static String getServerPowerStatus(String addr, Integer port, String username, String password) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return BaremetalChassisConstant.SERVER_POWER_ON;
        }

        IPMIToolCaller caller = new IPMIToolCaller();
        caller.hostname = addr;
        caller.port = port;
        caller.username = username;
        caller.password = password;
        ShellResult rst = caller.status();
        if (rst.getRetCode() == 0) {
            if (rst.getStdout().trim().equals("Chassis Power is on")) {
                return BaremetalChassisConstant.SERVER_POWER_ON;
            } else if (rst.getStdout().trim().equals("Chassis Power is off")) {
                return BaremetalChassisConstant.SERVER_POWER_OFF;
            } else {
                return BaremetalChassisConstant.SERVER_POWER_UNKNOWN;
            }
        } else {
            return BaremetalChassisConstant.SERVER_POWER_UNKNOWN;
        }
    }

    public static String getServerPowerStatus(BaremetalChassisInventory bmc) {
        return getServerPowerStatus(bmc.getIpmiAddress(), bmc.getIpmiPort(), bmc.getIpmiUsername(), bmc.getIpmiPassword());
    }

    // power on remote server from disk using ipmitool, return false if failed to
    public static boolean powerOnRemoteServer(BaremetalChassisInventory bmc) {
        return powerOnRemoteServer(bmc, BaremetalChassisConstant.SERVER_BOOT_DEV_DISK);
    }

    public static boolean powerOnRemoteServer(BaremetalChassisInventory bmc, String bootDev) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return true;
        }

        if (getServerPowerStatus(bmc).equals(BaremetalChassisConstant.SERVER_POWER_ON)) {
            logger.debug(String.format("chassis[%s:%d] is already powered on", bmc.getIpmiAddress(), bmc.getIpmiPort()));
            return true;
        }

        if (StringUtils.isNotBlank(bootDev)) {
            setServerBootDevice(bmc, bootDev);
        }

        return 0 == IPMIToolCaller.fromBaremetalChassisInventory(bmc)
                .powerOn();
    }

    // power off remote server using ipmitool, return false if faild to
    public static boolean powerOffRemoteServer(BaremetalChassisInventory bmc) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return true;
        }

        if (getServerPowerStatus(bmc).equals(BaremetalChassisConstant.SERVER_POWER_OFF)) {
            logger.debug(String.format("chassis[%s:%d] is already powered off", bmc.getIpmiAddress(), bmc.getIpmiPort()));
            return true;
        }

        return 0 == IPMIToolCaller.fromBaremetalChassisInventory(bmc)
                .powerOff();
    }

    // power reset remote server from disk using ipmitool, return false if failed to
    public static boolean powerResetRemoteServer(BaremetalChassisInventory bmc) {
        return powerResetRemoteServer(bmc, BaremetalChassisConstant.SERVER_BOOT_DEV_DISK);
    }

    public static boolean powerResetRemoteServer(BaremetalChassisInventory bmc, String bootDev) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return true;
        }

        if (StringUtils.isNotBlank(bootDev)) {
            setServerBootDevice(bmc, bootDev);
        }

        if (getServerPowerStatus(bmc).equals(BaremetalChassisConstant.SERVER_POWER_OFF)) {
            logger.debug(String.format("chassis[%s:%d] is currently powered off, so power it on instead of reset", bmc.getIpmiAddress(), bmc.getIpmiPort()));
            return 0 == IPMIToolCaller.fromBaremetalChassisInventory(bmc)
                    .powerOn();
        } else {
            return 0 == IPMIToolCaller.fromBaremetalChassisInventory(bmc)
                    .powerReset();
        }
    }

    public static String lengthToNetmask(String len) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return "255.255.255.0";
        }

        int prefix = Integer.parseInt(len);
        int mask = 0xffffffff << (32 - prefix);
        int value = mask;
        byte[] bytes = new byte[]{(byte)(value >>> 24), (byte)(value >> 16 & 0xff), (byte)(value >> 8 & 0xff), (byte)(value & 0xff) };

        InetAddress netAddr = null;
        try {
            netAddr = InetAddress.getByAddress(bytes);
        } catch (UnknownHostException e) {
            return "";
        }
        return netAddr.getHostAddress();
    }

    // get encrypted password
    public static String getEncryptedPassword(String password) {
        Crypt crypt = new Crypt();
        return crypt.crypt(password);
    }

    public static void reloadNginxService() {
        ShellUtils.run("systemctl start nginx && sudo systemctl reload nginx");
    }
}
