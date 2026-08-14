package org.zstack.header.vm.devices


import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询虚拟机设备地址组的返回"

	ref {
		name "inventories"
		path "org.zstack.header.vm.devices.APIQueryVmInstanceResourceMetadataGroupReply.inventories"
		desc "虚拟机设备地址组清单列表"
		type "List"
		since "zsv 4.10.16"
		clz VmInstanceResourceMetadataGroupInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "zsv 4.10.16"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.devices.APIQueryVmInstanceResourceMetadataGroupReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 4.10.16"
		clz ErrorCode.class
	}
}
