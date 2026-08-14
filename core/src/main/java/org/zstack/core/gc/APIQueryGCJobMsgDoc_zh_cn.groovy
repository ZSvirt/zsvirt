package org.zstack.core.gc

import org.zstack.core.gc.APIQueryGCJobReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryGCJob"

	category "gc"

	desc """在这里填写API描述"""

	rest {
		request {
			url "GET /v1/gc-jobs"
			url "GET /v1/gc-jobs/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryGCJobMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryGCJobReply.class
		}
	}
}