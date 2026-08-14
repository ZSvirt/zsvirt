package org.zstack.softwarePackage.header

import org.zstack.header.errorcode.ErrorCode

doc {
    title "上传软件包到虚拟机返回"

    field {
        name "uploadTaskUuid"
        desc "上传任务UUID"
        type "String"
        since "5.1.0"
    }
    field {
        name "uploadUrl"
        desc "upload协议的直传地址，HTTP和HTTPS上传时为空"
        type "String"
        since "5.1.0"
    }
    field {
        name "success"
        desc "操作成功时为true，否则为false"
        type "boolean"
        since "5.1.0"
    }
    ref {
        name "error"
        path "org.zstack.softwarePackage.header.APIUploadSoftwarePackageToVmEvent.error"
        desc "错误码，若不为null，则表示操作失败"
        type "ErrorCode"
        since "5.1.0"
        clz ErrorCode.class
    }
}
