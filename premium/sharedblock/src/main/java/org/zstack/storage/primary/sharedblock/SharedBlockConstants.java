package org.zstack.storage.primary.sharedblock;

import org.zstack.header.configuration.PythonClass;

import java.util.Arrays;
import java.util.List;

public class SharedBlockConstants {
    public static final String SERVICE_ID = "sharedblock";

    @PythonClass
    public static final String SHARED_BLOCK_PRIMARY_STORAGE_TYPE = "SharedBlock";

    @PythonClass
    public static final String SHARED_BLOCK_INSTALL_PATH_SCHEME = "sharedblock://";

    @PythonClass
    public static final String SHARED_BLOCK_ABSOLUTE_PATH_SCHEME = "/dev/";

    @PythonClass
    public static final List<String> VALID_QCOW2_ALLOCATION = Arrays.asList("none", "metadata");

    public static final String ANSIBLE_MODULE_PATH = "ansible/zsblkagentansible";
    public static final String ANSIBLE_PLAYBOOK_NAME = "zsblkagentansible.py";
    public static final String AGENT_PACKAGE_NAME = "zsblk-agent.bin";
    public static final String DEVICE_ALLOCATE_STRATEGY = "allocateStrategy";

    public static final long MIN_THIN_PROVISIONING_INITIALIZE_SIZE = 1073741824;
}
