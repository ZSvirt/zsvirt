package org.zstack.network.l2.virtualSwitch.header

import org.zstack.network.l2.virtualSwitch.header.APIQueryHostKernelInterfaceReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryHostKernelInterface"

	category "network.l2"

	desc """查询Kernel适配器"""

	rest {
		request {
			url "GET /v1/l3-networks/kernel-interfaces"
			url "GET /v1/l3-networks/kernel-interfaces/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryHostKernelInterfaceMsg.class

			desc """查询Kernel适配器"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryHostKernelInterfaceReply.class
		}
	}
}