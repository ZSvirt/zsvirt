package org.zstack.header.affinitygroup

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.vm.VmInstanceInventory

doc {

	title "获取可绑定亲和组的虚拟机"

	ref {
		name "error"
		path "org.zstack.header.affinitygroup.APIGetCandidateVMForAttachingAffinityGroupReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.10.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.affinitygroup.APIGetCandidateVMForAttachingAffinityGroupReply.inventories"
		desc "虚拟机清单列表"
		type "List"
		since "3.10.0"
		clz VmInstanceInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.10.0"
	}
}
