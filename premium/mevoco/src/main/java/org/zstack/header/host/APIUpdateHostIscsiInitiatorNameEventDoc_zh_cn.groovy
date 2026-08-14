package org.zstack.header.host

import org.zstack.header.errorcode.ErrorCode
import org.zstack.kvm.KVMHostInventory

doc {

	title "更新主机iscsiInitiatorName消息回复"

	ref {
		name "inventory"
		path "org.zstack.header.host.APIUpdateHostIscsiInitiatorNameEvent.inventory"
		desc "null"
		type "KVMHostInventory"
		since "4.10.6"
		clz KVMHostInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "4.10.6"
	}
	ref {
		name "error"
		path "org.zstack.header.host.APIUpdateHostIscsiInitiatorNameEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.10.6"
		clz ErrorCode.class
	}
}
