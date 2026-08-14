package org.zstack.autoscaling.group.instance

import org.zstack.autoscaling.group.instance.APIQueryAutoScalingGroupInstanceReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryAutoScalingGroupInstance"

	category "autoscaling"

	desc """查询伸缩组组内云主机列表"""

	rest {
		request {
			url "GET /v1/autoscaling/groups/instances"
			url "GET /v1/autoscaling/groups/instances/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryAutoScalingGroupInstanceMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryAutoScalingGroupInstanceReply.class
		}
	}
}