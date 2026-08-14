package org.zstack.kvm.hypervisor;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Constant set for KvmHypervisorInfo manager
 * 
 * Created by Wenhao.Zhang on 23/03/01
 */
public class KvmHypervisorConstant {
    private KvmHypervisorConstant() {}

    public static final Path DVD_ROOT_PATH =
            Paths.get("/opt", "zstack-dvd");
    public static final Path VIRTUALIZER_INFO_SCRIPT_PATH =
            Paths.get("Extra", "virtualizer-info.sh");
    public static final Set<String> IGNORE_DIR_AT_DVD = new HashSet<>(
            Arrays.asList("native-repos")
    );

    public static final String KEY_QEMU_KVM_VERSION = "qemu-kvm.version";
    public static final String KEY_PLATFORM_DIST_NAME = "platform.distname";
    public static final String KEY_PLATFORM_VERSION = "platform.version";
    public static final String KEY_PLATFORM_ID = "platform.id";
}
