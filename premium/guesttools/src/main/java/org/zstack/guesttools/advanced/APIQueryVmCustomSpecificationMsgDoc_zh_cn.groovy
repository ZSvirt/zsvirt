package org.zstack.guesttools.advanced

import org.zstack.guesttools.advanced.APIQueryVmCustomSpecificationReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryVmCustomSpecification"

	category "guest.tools"

	desc """查询虚拟机自定义操作系统规范"""

	rest {
		request {
			url "GET /v1/vm-custom-specifications"
			url "GET /v1/vm-custom-specifications/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryVmCustomSpecificationMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryVmCustomSpecificationReply.class
		}
	}
}