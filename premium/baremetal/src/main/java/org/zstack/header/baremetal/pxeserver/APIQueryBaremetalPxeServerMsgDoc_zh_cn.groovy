package org.zstack.header.baremetal.pxeserver

import org.zstack.header.baremetal.pxeserver.APIQueryBaremetalPxeServerReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryBaremetalPxeServer"

	category "baremetal.pxeserver"

	desc """查询PXE服务"""

	rest {
		request {
			url "GET /v1/baremetal/pxeservers"
			url "GET /v1/baremetal/pxeservers/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryBaremetalPxeServerMsg.class

			desc """查询PXE服务"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryBaremetalPxeServerReply.class
		}
	}
}