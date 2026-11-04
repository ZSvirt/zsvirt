package org.zstack.header.vm

import org.zstack.header.vm.APIQueryVmInstanceReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询虚拟机(QueryVmInstance)"

	category "虚拟机"

	desc """查询虚拟机"""

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