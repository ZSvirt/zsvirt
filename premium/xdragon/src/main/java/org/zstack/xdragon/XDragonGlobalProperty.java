package org.zstack.xdragon;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

import java.util.List;

@GlobalPropertyDefinition
public class XDragonGlobalProperty {
    @GlobalProperty(name = "xdragon.qemuPath", defaultValue = "/usr/bin/iohub-ctrl")
    public static String QEMU_Path;

    @GlobalProperty(name = "xdragon.onCrash", defaultValue = "coredump-restart")
    public static String ON_CRASH;

    @GlobalProperty(name = "xdragon.useMemBalloon", defaultValue = "false")
    public static Boolean USE_MEM_BALLOON;

    @GlobalProperty(name = "xdragon.useDataPlane", defaultValue = "true")
    public static Boolean USE_DATA_PLANE;

    @GlobalProperty(name = "xdragon.disableConsole", defaultValue = "true")
    public static Boolean DISABLE_CONSOLE;

    @GlobalProperty(name = "xdragon.vhostSourcePath", defaultValue = "/run/boot_sock0")
    public static String VHOST_SRC_PATH;

    @GlobalProperty(name = "xdragon.asApplianceVm", defaultValue = "true")
    public static boolean AS_APPLIANCE_VM;

    @GlobalProperty(name = "xdragon.disableQgaChannel", defaultValue = "true")
    public static boolean DISABLE_QGA_CHANNEL;

    @GlobalProperty(name = "xdragon.loaderROM", defaultValue = "/usr/share/qemu/bios-iohub.bin")
    public static String LOADER_ROM;

    @GlobalProperty(name = "xdragon.tapName", defaultValue = "dtap0")
    public static String tapName;

    @GlobalProperty(name = "xdragon.masterVethName", defaultValue = "veth1")
    public static String masterVethName;

    @GlobalProperty(name = "xdragon.skip.packages", defaultValue = "collectd-virt ipmitool libvirt libvirt-client libvirt-python MegaCli OpenIPMI-modalias qemu-kvm qemu-kvm-ev")
    public static String SKIP_PACKAGES;

    @GlobalProperty(name = "xdragon.qemu.args.")
    public static List<String> QEMU_ARGS;
}
