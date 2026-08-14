package org.zstack.header.host

import org.zstack.header.host.APIQueryHostPhysicalMemoryReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryHostPhysicalMemory"

	category "host"

	desc """查询物理机内存卡信息"""

	rest {
		request {
			url "GET /v1/hosts/physicalmemory"
			url "GET /v1/hosts/physicalmemory/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryHostPhysicalMemoryMsg.class

			desc """查询物理机内存卡信息"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryHostPhysicalMemoryReply.class
		}
	}
}