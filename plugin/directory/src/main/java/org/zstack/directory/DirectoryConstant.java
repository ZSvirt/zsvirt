package org.zstack.directory;

import org.zstack.header.configuration.PythonClass;

/**
 * @author shenjin
 * @date 2022/12/20 14:20
 */
@PythonClass
public interface DirectoryConstant {
    String DEFAULT_DIRECTORY = "default";
    String VCENTER_DIRECTORY = "vcenter";
    String OPERATE_DIRECTORY_THREAD_NAME = "create-update-delete-move-directory";
}
