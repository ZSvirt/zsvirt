package org.zstack.kvm.hypervisor.message

import org.zstack.kvm.hypervisor.datatype.KvmHypervisorInfoInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询资源的虚拟化软件信息结果"

	ref {
		name "inventories"
		path "org.zstack.kvm.hypervisor.message.APIQueryKvmHypervisorInfoReply.inventories"
		desc "资源的虚拟化软件清单列表"
		type "List"
		since "3.16.21"
		clz KvmHypervisorInfoInventory.class
	}
	field {
		name "success"
		desc "查询是否成功"
		type "boolean"
		since "3.16.21"
	}
	ref {
		name "error"
		path "org.zstack.kvm.hypervisor.message.APIQueryKvmHypervisorInfoReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.16.21"
		clz ErrorCode.class
	}
}
