package org.zstack.header.vpc.ha

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.vpc.ha.VpcHaGroupInventory

doc {

	title "高可用组清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.vpc.ha.APICreateVpcHaGroupEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.5"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.vpc.ha.APICreateVpcHaGroupEvent.inventory"
		desc "null"
		type "VpcHaGroupInventory"
		since "3.5"
		clz VpcHaGroupInventory.class
	}
}
