package org.zstack.header.vmscheduling

import org.zstack.header.vmscheduling.APIQueryHostSchedulingRuleGroupReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryHostSchedulingRuleGroup"

	category "vmSchedulingRule"

	desc """查询物理机调度策略组"""

	rest {
		request {
			url "GET /v1/query/host/schedulingRule/group"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryHostSchedulingRuleGroupMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryHostSchedulingRuleGroupReply.class
		}
	}
}