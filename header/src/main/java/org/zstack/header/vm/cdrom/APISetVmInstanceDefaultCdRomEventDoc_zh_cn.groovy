package org.zstack.header.vm.cdrom

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.vm.cdrom.VmCdRomInventory

doc {

	title "设置云主机默认CDROM返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.cdrom.APISetVmInstanceDefaultCdRomEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 3.3"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.vm.cdrom.APISetVmInstanceDefaultCdRomEvent.inventory"
		desc "null"
		type "VmCdRomInventory"
		since "zsv 3.3"
		clz VmCdRomInventory.class
	}
}
