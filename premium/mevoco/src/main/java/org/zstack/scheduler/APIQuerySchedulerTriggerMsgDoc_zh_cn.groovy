package org.zstack.scheduler

import org.zstack.scheduler.APIQuerySchedulerTriggerReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QuerySchedulerTrigger"

	category "scheduler"

	desc """在这里填写API描述"""

	rest {
		request {
			url "GET /v1/scheduler/triggers"
			url "GET /v1/scheduler/triggers/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQuerySchedulerTriggerMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQuerySchedulerTriggerReply.class
		}
	}
}