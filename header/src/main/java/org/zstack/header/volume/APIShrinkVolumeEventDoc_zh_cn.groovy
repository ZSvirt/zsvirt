package org.zstack.header.volume

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.storage.snapshot.ShrinkResult

doc {
    title "收缩卷返回"

    field {
        name "success"
        desc ""
        type "boolean"
        since "5.2.0"
    }
    ref {
        name "error"
        path "org.zstack.header.volume.APIShrinkVolumeEvent.error"
        desc "错误码，若不为null，则表示操作失败，操作成功时该字段为null", false
        type "ErrorCode"
        since "5.2.0"
        clz ErrorCode.class
    }
    ref {
        name "shrinkResult"
        path "org.zstack.header.volume.APIShrinkVolumeEvent.shrinkResult"
        desc "收缩结果"
        type "ShrinkResult"
        since "5.2.0"
        clz ShrinkResult.class
    }
}
