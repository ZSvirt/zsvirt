package org.zstack.vpc

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.network.l3.L3NetworkInventory

doc {

	title "三层网络清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.vpc.APIGetAttachableVpcL3NetworkReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.3"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.vpc.APIGetAttachableVpcL3NetworkReply.inventories"
		desc "null"
		type "List"
		since "2.3"
		clz L3NetworkInventory.class
	}
}
