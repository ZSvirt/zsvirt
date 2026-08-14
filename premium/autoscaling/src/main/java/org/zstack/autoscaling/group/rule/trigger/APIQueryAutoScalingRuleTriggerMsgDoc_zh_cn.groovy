package org.zstack.autoscaling.group.rule.trigger

import org.zstack.autoscaling.group.rule.trigger.APIQueryAutoScalingRuleTriggerReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询伸缩规则触发器列表"

	category "autoscaling"

	desc """在这里填写API描述"""

	rest {
		request {
			url "GET /v1/autoscaling/groups/rules/trigger"
			url "GET /v1/autoscaling/groups/rules/trigger/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryAutoScalingRuleTriggerMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryAutoScalingRuleTriggerReply.class
		}
	}
}