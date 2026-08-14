package org.zstack.autoscaling.group.rule

import org.zstack.autoscaling.group.rule.APIQueryAutoScalingRuleReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryAutoScalingRule"

	category "autoscaling"

	desc """查询伸缩规则"""

	rest {
		request {
			url "GET /v1/autoscaling/groups/rules"
			url "GET /v1/autoscaling/groups/rules/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryAutoScalingRuleMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryAutoScalingRuleReply.class
		}
	}
}