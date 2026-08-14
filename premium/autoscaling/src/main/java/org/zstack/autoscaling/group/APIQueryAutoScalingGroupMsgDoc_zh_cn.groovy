package org.zstack.autoscaling.group

import org.zstack.autoscaling.group.APIQueryAutoScalingGroupReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryAutoScalingGroup"

	category "autoscaling"

	desc """查询伸缩组"""

	rest {
		request {
			url "GET /v1/autoscaling/groups"
			url "GET /v1/autoscaling/groups/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryAutoScalingGroupMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryAutoScalingGroupReply.class
		}
	}
}