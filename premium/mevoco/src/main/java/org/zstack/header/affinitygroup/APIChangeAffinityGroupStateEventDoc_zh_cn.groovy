package org.zstack.header.affinitygroup

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.affinitygroup.AffinityGroupInventory

doc {

	title "亲和组清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.affinitygroup.APIChangeAffinityGroupStateEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.3"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.affinitygroup.APIChangeAffinityGroupStateEvent.inventory"
		desc "null"
		type "AffinityGroupInventory"
		since "2.3"
		clz AffinityGroupInventory.class
	}
}

