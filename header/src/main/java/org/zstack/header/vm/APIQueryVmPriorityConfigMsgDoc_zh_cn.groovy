package org.zstack.header.vm

import org.zstack.header.vm.APIQueryVmPriorityConfigReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryVmPriorityConfig"

	category "vmInstance"

	desc """查询云主机优先级配置"""

	rest {
		request {
			url "GET /v1/vm-priority-config"
			url "GET /v1/vm-priority-config/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryVmPriorityConfigMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryVmPriorityConfigReply.class
		}
	}
}