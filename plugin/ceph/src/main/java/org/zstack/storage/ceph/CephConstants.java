package org.zstack.storage.ceph;
/**
 * Created by frank on 7/27/2015.
 */

import org.zstack.header.configuration.PythonClass;

@PythonClass
public interface CephConstants {
    @PythonClass
    String CEPH_BACKUP_STORAGE_TYPE = "Ceph";
    @PythonClass
    String CEPH_PRIMARY_STORAGE_TYPE = "Ceph";

    String MON_PARAM_MON_PORT = "monPort";

    String AFTER_ADD_BACKUPSTORAGE = "afterAddBackupStorage";

    String AFTER_EXPUNGE_IMAGE = "afterExpungeImage";

    String AFTER_ADD_IMAGE = "afterAddImage";

    String CEPH_SCECRET_KEY = "ceph_secret_key";

    String CEPH_SECRECT_UUID = "ceph_secret_uuid";

    String CEPH_BS_IPTABLES_COMMENTS = "Cephb.allow.port";
    String CEPH_PS_IPTABLES_COMMENTS = "Cephp.allow.port";

    String CEPH_MANUFACTURER_XSKY = "xsky";
    String CEPH_MANUFACTURER_SANDSTONE = "sandstone";
    String CEPH_MANUFACTURER_OPENSOURCE = "open-source";
    String CEPH_MANUFACTURER_ZSTONE = "zstone";

    String CEPH_ISCSI_PATH_PREFIX = "iscsi://";
}
