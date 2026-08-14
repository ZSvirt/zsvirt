package org.zstack.header.baremetal.instance

import org.zstack.header.baremetal.instance.APIQueryBaremetalInstanceReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryBaremetalInstance"

	category "baremetal.instance"

	desc """查询裸机实例"""

	rest {
		request {
			url "GET /v1/baremetal/instances"
			url "GET /v1/baremetal/instances/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryBaremetalInstanceMsg.class

			desc """查询裸机实例"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryBaremetalInstanceReply.class
		}
	}
}