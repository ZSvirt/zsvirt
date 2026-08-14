package org.zstack.header.vm

import org.zstack.header.vm.APIQueryVmNicReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询云主机网卡(QueryVmNic)"

	category "vmInstance"

	desc """查询云主机网卡"""

	rest {
		request {
			url "GET /v1/vm-instances/nics"
			url "GET /v1/vm-instances/nics/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryVmNicMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryVmNicReply.class
		}
	}
}