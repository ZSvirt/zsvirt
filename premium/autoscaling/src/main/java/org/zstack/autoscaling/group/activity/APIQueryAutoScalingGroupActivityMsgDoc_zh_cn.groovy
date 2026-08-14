package org.zstack.autoscaling.group.activity

import org.zstack.autoscaling.group.activity.APIQueryAutoScalingGroupActivityReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryAutoScalingGroupActivity"

	category "autoscaling"

	desc """查询伸缩组活动列表"""

	rest {
		request {
			url "GET /v1/autoscaling/groups/activities"
			url "GET /v1/autoscaling/groups/activities/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryAutoScalingGroupActivityMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryAutoScalingGroupActivityReply.class
		}
	}
}