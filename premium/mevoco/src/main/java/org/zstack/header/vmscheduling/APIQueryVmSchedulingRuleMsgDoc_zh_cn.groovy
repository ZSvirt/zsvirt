package org.zstack.header.vmscheduling

import org.zstack.header.vmscheduling.APIQueryVmSchedulingRuleReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryVmSchedulingRule"

	category "vmSchedulingRule"

	desc """查询云主机调度策略"""

	rest {
		request {
			url "GET /v1/query/vm/schedulingRule"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryVmSchedulingRuleMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryVmSchedulingRuleReply.class
		}
	}
}