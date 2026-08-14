package org.zstack.header.configuration

import org.zstack.header.errorcode.ErrorCode

doc {

	title "云主机规格清单列表"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.configuration.APIQueryInstanceOfferingReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null", false
		type "ErrorCode"
		since "zsv 0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.configuration.APIQueryInstanceOfferingReply.inventories"
		desc "云主机规格清单列表"
		type "List"
		since "zsv 0.6"
		clz InstanceOfferingInventory.class
	}
}
