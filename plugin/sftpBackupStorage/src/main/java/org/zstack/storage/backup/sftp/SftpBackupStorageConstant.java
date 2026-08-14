package org.zstack.storage.backup.sftp;

import org.zstack.header.configuration.PythonClass;

@PythonClass
public interface SftpBackupStorageConstant {
    public static final String SERVICE_ID = "storage.backup.sftp";

    @PythonClass
    public static final String SFTP_BACKUP_STORAGE_TYPE = "SftpBackupStorage";

    public static final String CONNECT_PATH = "/sftpbackupstorage/connect";
    public static final String DOWNLOAD_IMAGE_PATH = "/sftpbackupstorage/download";
    public static final String DELETE_PATH = "/sftpbackupstorage/delete";
    public static final String PING_PATH = "/sftpbackupstorage/ping";
    public static final String ECHO_PATH = "/sftpbackupstorage/echo";
    public static final String GET_SSHKEY_PATH = "/sftpbackupstorage/sshkey";
    public static final String WRITE_IMAGE_METADATA= "/sftpbackupstorage/writeimagemetadata";
    public static final String GENERATE_IMAGE_METADATA_FILE = "/sftpbackupstorage/generateimagemetadatafile";
    public static final String CHECK_IMAGE_METADATA_FILE_EXIST = "/sftpbackupstorage/checkimagemetadatafileexist";
    public static final String DUMP_IMAGE_METADATA_TO_FILE = "/sftpbackupstorage/dumpimagemetadatatofile";
    public static final String GET_IMAGES_METADATA = "/sftpbackupstorage/getimagesmetadata";
    public static final String DELETE_IMAGES_METADATA = "/sftpbackupstorage/deleteimagesmetadata";
    public static final String ANSIBLE_PLAYBOOK_NAME = "sftpbackupstorage.py";
    public static final String ANSIBLE_MODULE_PATH = "ansible/sftpbackupstorage";
    public static final String GET_IMAGE_SIZE = "/sftpbackupstorage/getimagesize";
    public static final String GET_LOCAL_FILE_SIZE = "/sftpbackupstorage/getlocalfilesize/";
    public static final String GET_IMAGE_HASH = "/sftpbackupstorage/gethash";
    public static final String IPTABLES_COMMENTS = "sftpbs.allow.port";
}
