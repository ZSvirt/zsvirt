package org.zstack.guesttools.advanced


import org.zstack.header.errorcode.ErrorCode

doc {

	title "更新虚拟机自定义操作系统规范返回"

	ref {
		name "inventory"
		path "org.zstack.guesttools.advanced.APIUpdateVmCustomSpecificationEvent.inventory"
		desc "null"
		type "VmCustomSpecificationInventory"
		since "4.10.18"
		clz VmCustomSpecificationInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "4.10.18"
	}
	ref {
		name "error"
		path "org.zstack.guesttools.advanced.APIUpdateVmCustomSpecificationEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.10.18"
		clz ErrorCode.class
	}
}
