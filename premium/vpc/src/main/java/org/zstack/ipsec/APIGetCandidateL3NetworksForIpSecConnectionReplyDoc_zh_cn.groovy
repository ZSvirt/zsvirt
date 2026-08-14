package org.zstack.ipsec

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.network.l3.L3NetworkInventory

doc {

	title "获取ipsec可用L3网络"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.ipsec.APIGetCandidateL3NetworksForIpSecConnectionReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.ipsec.APIGetCandidateL3NetworksForIpSecConnectionReply.inventories"
		desc "null"
		type "List"
		since "4.0"
		clz L3NetworkInventory.class
	}
}
