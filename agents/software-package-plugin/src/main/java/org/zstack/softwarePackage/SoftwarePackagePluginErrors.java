package org.zstack.softwarePackage;

public enum SoftwarePackagePluginErrors {
    GENERAL_ERROR(1000),

    UNSUPPORTED_SOFTWARE_TYPE(1101),
    INSUFFICIENT_CAPACITY_FOR_BACKUP_STORAGE(1121),
    INVALID_BACKUP_STORAGE_FOR_PACKAGE(1122),
    INVALID_INSTALL_PATH(1131),
    INVALID_SOFTWARE_PACKAGE(1132),
    INVALID_UPLOAD_SESSION(1133),

    SHELL_ERRORS(1300),

    UPLOAD_SOFTWARE_PACKAGE_INTERRUPTED(1901),
    ;

    private String code;

    SoftwarePackagePluginErrors(int id) {
        code = String.format("SoftwarePackage.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
