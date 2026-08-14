package org.zstack.header.vm


import org.zstack.header.errorcode.ErrorCode

doc {

	title "从元数据注册云主机返回"

	ref {
		name "inventory"
		path "org.zstack.header.vm.APIRegisterVmInstanceFromMetadataEvent.inventory"
		desc "云主机详情"
		type "VmInstanceInventory"
		since "zsv 5.0.0"
		clz VmInstanceInventory.class
	}
	field {
		name "success"
		desc "操作是否成功"
		type "boolean"
		since "zsv 5.0.0"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APIRegisterVmInstanceFromMetadataEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 5.0.0"
		clz ErrorCode.class
	}
}
