package org.zstack.header.vmscheduling

import org.zstack.header.vmscheduling.HostSchedulingRuleGroupInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "返回物理机调度策略组"

	ref {
		name "inventories"
		path "org.zstack.header.vmscheduling.APIQueryHostSchedulingRuleGroupReply.inventories"
		desc "物理机调度策略组清单列表"
		type "List"
		since "3.17.0"
		clz HostSchedulingRuleGroupInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.17.0"
	}
	ref {
		name "error"
		path "org.zstack.header.vmscheduling.APIQueryHostSchedulingRuleGroupReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.0"
		clz ErrorCode.class
	}
}
