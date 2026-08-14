package org.zstack.network.service.eip

import org.zstack.network.service.eip.EipInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取虚拟机网卡可挂载弹性 IP 的返回"

	ref {
		name "inventories"
		path "org.zstack.network.service.eip.APIGetVmNicAttachableEipsReply.inventories"
		desc "虚拟机弹性 IP 清单列表"
		type "List"
		since "3.13.18"
		clz EipInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.13.18"
	}
	ref {
		name "error"
		path "org.zstack.network.service.eip.APIGetVmNicAttachableEipsReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.13.18"
		clz ErrorCode.class
	}
}
