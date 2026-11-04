package org.zstack.externalStorage.primary;

import org.zstack.header.configuration.PythonClass;

/**
 * @author Xingwei Yu
 * @date 2024/8/16 11:07
 */
@PythonClass
public class ExternalStorageConstant {
    private ExternalStorageConstant() {}

    public static final String ISCSI_PROTOCOL = "iSCSI";
    public static final String CBD_PROTOCOL = "CBD";
}
