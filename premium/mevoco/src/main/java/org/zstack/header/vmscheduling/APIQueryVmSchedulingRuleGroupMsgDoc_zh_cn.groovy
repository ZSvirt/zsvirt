package org.zstack.header.vmscheduling

import org.zstack.header.vmscheduling.APIQueryVmSchedulingRuleGroupReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryVmSchedulingRuleGroup"

	category "vmSchedulingRule"

	desc """查询云主机调度策略组"""

	rest {
		request {
			url "GET /v1/query/vm/schedulingRule/group"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryVmSchedulingRuleGroupMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryVmSchedulingRuleGroupReply.class
		}
	}
}