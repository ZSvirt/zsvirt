package org.zstack.header.affinitygroup

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.affinitygroup.AffinityGroupInventory

doc {

	title "添加虚拟机到亲和组的请求返回"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "2.2"
	}
	ref {
		name "error"
		path "org.zstack.header.affinitygroup.APIAddVmToAffinityGroupEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "2.2"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.affinitygroup.APIAddVmToAffinityGroupEvent.inventory"
		desc "亲和组清单"
		type "AffinityGroupInventory"
		since "2.2"
		clz AffinityGroupInventory.class
	}
}
