package org.zstack.header.vm

import org.zstack.header.vm.APIQueryVmInstanceReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询云主机(QueryVmInstance)"

	category "云主机"

	desc """查询云主机"""

	rest {
		request {
			url "GET /v1/vm-instances"
			url "GET /v1/vm-instances/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryVmInstanceMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryVmInstanceReply.class
		}
	}
}