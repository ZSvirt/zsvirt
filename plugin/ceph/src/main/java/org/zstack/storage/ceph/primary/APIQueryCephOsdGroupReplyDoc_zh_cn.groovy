package org.zstack.storage.ceph.primary

import org.zstack.storage.ceph.primary.CephOsdGroupInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询CephOsdGroup返回"

	ref {
		name "inventories"
		path "org.zstack.storage.ceph.primary.APIQueryCephOsdGroupReply.inventories"
		desc "null"
		type "List"
		since "0.6"
		clz CephOsdGroupInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.storage.ceph.primary.APIQueryCephOsdGroupReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
}
