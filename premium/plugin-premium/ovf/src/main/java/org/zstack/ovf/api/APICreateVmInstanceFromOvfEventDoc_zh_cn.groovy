package org.zstack.ovf.api

import org.zstack.header.vm.VmInstanceInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "从 OVF 模板导入虚拟机返回"

	ref {
		name "inventory"
		path "org.zstack.ovf.api.APICreateVmInstanceFromOvfEvent.inventory"
		desc "从 OVF 模板中导入虚拟机数据"
		type "VmInstanceInventory"
		since "3.14.6"
		clz VmInstanceInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.14.6"
	}
	ref {
		name "error"
		path "org.zstack.ovf.api.APICreateVmInstanceFromOvfEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.14.6"
		clz ErrorCode.class
	}
}
