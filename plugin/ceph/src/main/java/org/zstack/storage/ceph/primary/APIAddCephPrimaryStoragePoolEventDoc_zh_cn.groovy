package org.zstack.storage.ceph.primary

import org.zstack.header.errorcode.ErrorCode
import org.zstack.storage.ceph.primary.CephPrimaryStoragePoolInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "在这里输入结构的名称"

	ref {
		name "error"
		path "org.zstack.storage.ceph.primary.APIAddCephPrimaryStoragePoolEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.storage.ceph.primary.APIAddCephPrimaryStoragePoolEvent.inventory"
		desc "null"
		type "CephPrimaryStoragePoolInventory"
		since "0.6"
		clz CephPrimaryStoragePoolInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
}
