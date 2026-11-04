package org.zstack.storage.primary.sharedblock

import org.zstack.header.errorcode.ErrorCode

doc {
    title "获取共享块存储 Volume Snapshot LAN-free 布局的结果"

    field {
        name "success"
        desc ""
        type "boolean"
        since "zsv_0.6"
    }
    ref {
        name "error"
        path "org.zstack.storage.primary.sharedblock.APIGetSharedBlockVolumeSnapshotLanFreeLayoutReply.error"
        desc "错误码，若不为null，则表示操作失败，操作成功时该字段为null", false
        type "ErrorCode"
        since "1.0.1, zsv_5.2.0"
        clz ErrorCode.class
    }
    ref {
        name "luns"
        path "org.zstack.storage.primary.sharedblock.APIGetSharedBlockVolumeSnapshotLanFreeLayoutReply.luns"
        desc "所有返回层实际引用的LUN，按WWID唯一，容量为完整裸LUN容量"
        type "List"
        since "1.0.1, zsv_5.2.0"
        clz SharedBlockLunDescriptor.class
    }
    ref {
        name "layouts"
        path "org.zstack.storage.primary.sharedblock.APIGetSharedBlockVolumeSnapshotLanFreeLayoutReply.layouts"
        desc "按snapshotUuids顺序返回的完整qcow2链布局。每层sourceInstallPath用于标识平台源位置，不能作为内容相同的证明；ranges描述该层LV字节到裸LUN字节的映射。"
        type "List"
        since "1.0.1, zsv_5.2.0"
        clz SharedBlockVolumeSnapshotLanFreeLayout.class
    }
}
