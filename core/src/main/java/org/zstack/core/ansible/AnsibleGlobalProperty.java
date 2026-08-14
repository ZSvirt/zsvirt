package org.zstack.core.ansible;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 */
@GlobalPropertyDefinition
public class AnsibleGlobalProperty {
    @GlobalProperty(name = "Ansible.executable", defaultValue = "python")
    public static String EXECUTABLE;
    @GlobalProperty(name = "Ansible.zstacklibPackageName", defaultValue = "zstacklib-5.0.0.tar.gz")
    public static String ZSTACKLIB_PACKAGE_NAME;
    @GlobalProperty(name = "Ansible.zstackRoot", defaultValue = "/var/lib/zstack")
    public static String ZSTACK_ROOT;
    @GlobalProperty(name = "Ansible.var.zstack_repo", defaultValue = "false")
    public static String ZSTACK_REPO;
    @GlobalProperty(name = "Ansible.fullDeploy", defaultValue = "false")
    public static boolean FULL_DEPLOY;
    @GlobalProperty(name = "Ansible.keepHostsFileInMemory", defaultValue = "true")
    public static boolean KEEP_HOSTS_FILE_IN_MEMORY;
    @GlobalProperty(name = "Ansible.debugMode", defaultValue = "false")
    public static boolean DEBUG_MODE;
    @GlobalProperty(name = "Ansible.debugMode2", defaultValue = "false")
    public static boolean DEBUG_MODE2;
}
