package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode

doc {

    title "查询虚拟机 Vnuma 开关状态"

    ref {
        name "error"
        path "org.zstack.header.vm.APIGetVmNumaReply.error"
        desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null", false
        type "ErrorCode"
        since "2.1.2"
        clz ErrorCode.class
    }
    field {
        name "success"
        desc "请求是否成功"
        type "boolean"
        since "2.1.2"
    }
    field {
        name "enable"
        desc "虚拟机 Vnuma 开关是否开启"
        type "boolean"
        since "3.13.12"
    }
}
