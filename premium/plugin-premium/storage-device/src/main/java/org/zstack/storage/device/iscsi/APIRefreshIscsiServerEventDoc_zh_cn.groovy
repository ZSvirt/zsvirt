package org.zstack.storage.device.iscsi

import org.zstack.header.errorcode.ErrorCode
import org.zstack.storage.device.iscsi.IscsiServerInventory

doc {

	title "刷新iSCSI服务器结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.storage.device.iscsi.APIRefreshIscsiServerEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.0.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.storage.device.iscsi.APIRefreshIscsiServerEvent.inventory"
		desc "null"
		type "IscsiServerInventory"
		since "3.0.0"
		clz IscsiServerInventory.class
	}
}
