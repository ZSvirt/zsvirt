package org.zstack.storage.ceph.primary

import org.zstack.header.errorcode.ErrorCode

doc {

	title "Ceph 主存储清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.storage.ceph.primary.APIAddMonToCephPrimaryStorageEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.storage.ceph.primary.APIAddMonToCephPrimaryStorageEvent.inventory"
		desc "null"
		type "CephPrimaryStorageInventory"
		since "0.6"
		clz CephPrimaryStorageInventory.class
	}
}
