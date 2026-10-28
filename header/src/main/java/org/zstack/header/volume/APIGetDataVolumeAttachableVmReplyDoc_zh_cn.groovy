package org.zstack.header.volume

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.vm.VmInstanceInventory

doc {

	title "虚拟机列表"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv_0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.volume.APIGetDataVolumeAttachableVmReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv_0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.volume.APIGetDataVolumeAttachableVmReply.inventories"
		desc "可加载的虚拟机列表"
		type "List"
		since "zsv_0.6"
		clz VmInstanceInventory.class
	}
}
