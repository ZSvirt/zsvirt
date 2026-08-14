package org.zstack.header.vmscheduling

import org.zstack.header.vmscheduling.VmSchedulingRuleGroupInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "更新虚拟机调度组返回"

	ref {
		name "inventory"
		path "org.zstack.header.vmscheduling.APIUpdateVmSchedulingRuleGroupEvent.inventory"
		desc "虚拟机调度策略组清单"
		type "VmSchedulingRuleGroupInventory"
		since "3.16.0"
		clz VmSchedulingRuleGroupInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.16.0"
	}
	ref {
		name "error"
		path "org.zstack.header.vmscheduling.APIUpdateVmSchedulingRuleGroupEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.16.0"
		clz ErrorCode.class
	}
}
