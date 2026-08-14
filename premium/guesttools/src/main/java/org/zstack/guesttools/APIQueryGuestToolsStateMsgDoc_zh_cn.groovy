package org.zstack.guesttools

import org.zstack.guesttools.APIQueryGuestToolsStateReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryGuestToolsState"

	category "guest.tools"

	desc """获取云主机GuestTools状态"""

	rest {
		request {
			url "GET /v1/guesttools"
			url "GET /v1/guesttools/state"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryGuestToolsStateMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryGuestToolsStateReply.class
		}
	}
}