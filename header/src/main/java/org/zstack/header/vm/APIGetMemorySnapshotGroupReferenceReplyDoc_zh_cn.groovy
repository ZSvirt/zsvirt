package org.zstack.header.vm

import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取资源引用快照组列表返回"

	ref {
		name "inventories"
		path "org.zstack.header.vm.APIGetMemorySnapshotGroupReferenceReply.inventories"
		desc "被引用的内存快照组列表"
		type "List"
		since "zsv 3.14.24"
		clz VolumeSnapshotGroupInventory.class
	}
	field {
		name "resourceUuid"
		desc "资源 UUID"
		type "String"
		since "zsv 3.14.24"
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "zsv 3.14.24"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APIGetMemorySnapshotGroupReferenceReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 3.14.24"
		clz ErrorCode.class
	}
}
