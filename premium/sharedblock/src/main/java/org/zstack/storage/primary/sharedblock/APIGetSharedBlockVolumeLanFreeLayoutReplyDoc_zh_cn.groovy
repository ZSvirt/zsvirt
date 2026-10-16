package org.zstack.storage.primary.sharedblock

import org.zstack.header.errorcode.ErrorCode

doc {
    title "获取共享块存储普通 Volume LAN-free 布局的结果"

    field {
        name "success"
        desc ""
        type "boolean"
        since "zsv_0.6"
    }
    ref {
        name "error"
        path "org.zstack.storage.primary.sharedblock.APIGetSharedBlockVolumeLanFreeLayoutReply.error"
        desc "错误码；成功时为空"
        type "ErrorCode"
        since "1.0.1, zsv_5.2.0"
        clz ErrorCode.class
    }
    ref {
        name "luns"
        path "org.zstack.storage.primary.sharedblock.APIGetSharedBlockVolumeLanFreeLayoutReply.luns"
        desc "各 Volume range 引用的完整裸 LUN，按 WWID 去重"
        type "List"
        since "1.0.1, zsv_5.2.0"
        clz SharedBlockLunDescriptor.class
    }
    ref {
        name "layouts"
        path "org.zstack.storage.primary.sharedblock.APIGetSharedBlockVolumeLanFreeLayoutReply.layouts"
        desc "按 volumeUuids 请求顺序返回；每项表示单个普通 Volume LV，不包含快照链"
        type "List"
        since "1.0.1, zsv_5.2.0"
        clz SharedBlockVolumeLanFreeLayout.class
    }
}
