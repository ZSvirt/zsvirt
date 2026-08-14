package org.zstack.header.host

import org.zstack.header.host.APIQueryHostPhysicalCpuReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryHostPhysicalCpu"

	category "host"

	desc """查询物理机CPU信息"""

	rest {
		request {
			url "GET /v1/hosts/physical-cpu"
			url "GET /v1/hosts/physical-cpu/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryHostPhysicalCpuMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryHostPhysicalCpuReply.class
		}
	}
}