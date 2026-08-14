package org.zstack.appliancevm

import org.zstack.appliancevm.APIQueryApplianceVmReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询系统云主机(QueryApplianceVm)"

	category "系统云主机"

	desc """查询系统云主机"""

	rest {
		request {
			url "GET /v1/vm-instances/appliances"
			url "GET /v1/vm-instances/appliances/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryApplianceVmMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryApplianceVmReply.class
		}
	}
}