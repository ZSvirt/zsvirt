package org.zstack.autoscaling.template

import org.zstack.autoscaling.template.APIQueryAutoScalingVmTemplateReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryAutoScalingVmTemplate"

	category "autoscaling"

	desc """查询伸缩组云主机模板"""

	rest {
		request {
			url "GET /v1/autoscaling/vmtemplate"
			url "GET /v1/autoscaling/vmtemplate/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryAutoScalingVmTemplateMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryAutoScalingVmTemplateReply.class
		}
	}
}