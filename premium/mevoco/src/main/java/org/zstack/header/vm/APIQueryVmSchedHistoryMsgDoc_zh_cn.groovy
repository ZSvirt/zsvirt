package org.zstack.header.vm

import org.zstack.header.vm.APIQueryVmSchedHistoryReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryVmSchedHistory"

	category "mevoco"

	desc """在这里填写API描述"""

	rest {
		request {
			url "GET /v1/vm/sched-history"
			url "GET /v1/vm/sched-history/{vmInstanceUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryVmSchedHistoryMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryVmSchedHistoryReply.class
		}
	}
}