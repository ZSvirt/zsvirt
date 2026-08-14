package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.vm.VirtualizerInfoInventory

doc {

	title "获取资源虚拟化软件信息结果"

	field {
		name "success"
		desc "操作是否成功"
		type "boolean"
		since "3.16.21"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APIGetVirtualizerInfoReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.16.21"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.vm.APIGetVirtualizerInfoReply.inventories"
		desc "虚拟机 / 主机的虚拟化软件基础设施信息查询结果列表"
		type "List"
		since "3.16.21"
		clz VirtualizerInfoInventory.class
	}
}
