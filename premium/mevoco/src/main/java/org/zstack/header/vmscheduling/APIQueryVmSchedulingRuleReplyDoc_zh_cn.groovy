package org.zstack.header.vmscheduling

import org.zstack.header.vmscheduling.VmSchedulingRuleInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询虚拟机调度策略的请求返回"

	ref {
		name "inventories"
		path "org.zstack.header.vmscheduling.APIQueryVmSchedulingRuleReply.inventories"
		desc "虚拟机调度策略清单列表"
		type "List"
		since "3.17.0"
		clz VmSchedulingRuleInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.17.0"
	}
	ref {
		name "error"
		path "org.zstack.header.vmscheduling.APIQueryVmSchedulingRuleReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.0"
		clz ErrorCode.class
	}
}
