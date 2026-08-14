package org.zstack.header.vipQos

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.vipQos.VipQosInventory

doc {

	title "VIPQos清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.vipQos.APIGetVipQosReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.2"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.vipQos.APIGetVipQosReply.inventories"
		desc "null"
		type "List"
		since "2.2"
		clz VipQosInventory.class
	}
}
