package org.zstack.storage.ceph;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

import java.util.List;

/**
 * Created by frank on 7/29/2015.
 */
@GlobalPropertyDefinition
public class CephGlobalProperty {
    @GlobalProperty(name="Ceph.backupStorageAgent.port", defaultValue = "7761")
    public static int BACKUP_STORAGE_AGENT_PORT;
    @GlobalProperty(name="Ceph.backupStorageAgent.urlRootPath", defaultValue = "")
    public static String BACKUP_STORAGE_AGENT_URL_ROOT_PATH;
    @GlobalProperty(name="Ceph.primaryStorageAgent.urlRootPath", defaultValue = "")
    public static String PRIMARY_STORAGE_AGENT_URL_ROOT_PATH;
    @GlobalProperty(name="Ceph.primaryStorageAgent.port", defaultValue = "7762")
    public static int PRIMARY_STORAGE_AGENT_PORT;
    @GlobalProperty(name="Ceph.backupStorage.agentPackageName", defaultValue = "cephbackupstorage-5.0.0.tar.gz")
    public static String BACKUP_STORAGE_PACKAGE_NAME;
    @GlobalProperty(name="Ceph.backupStorage.ansiblePlaybook", defaultValue = "cephb.py")
    public static String BACKUP_STORAGE_PLAYBOOK_NAME;
    @GlobalProperty(name="Ceph.backupStorage.ansibleModulePath", defaultValue = "ansible/cephb")
    public static String BACKUP_STORAGE_MODULE_PATH;
    @GlobalProperty(name="Ceph.primaryStorage.agentPackageName", defaultValue = "cephprimarystorage-5.0.0.tar.gz")
    public static String PRIMARY_STORAGE_PACKAGE_NAME;
    @GlobalProperty(name="Ceph.primaryStorage.ansiblePlaybook", defaultValue = "cephp.py")
    public static String PRIMARY_STORAGE_PLAYBOOK_NAME;
    @GlobalProperty(name="Ceph.primaryStorage.ansibleModulePath", defaultValue = "ansible/cephp")
    public static String PRIMARY_STORAGE_MODULE_PATH;
    @GlobalProperty(name="Ceph.vendor.getXskyLicense.Port", defaultValue = "8051")
    public static String GET_XSKY_LICENSE_PORT;
    @GlobalProperty(name="MN.network.", defaultValue = "")
    public static List<String> MN_NETWORKS;
}
