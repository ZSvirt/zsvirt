package org.zstack.vpc

import org.zstack.vpc.APIQueryVpcSnatStateReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryVpcSnatState"

	category "vpc"

	desc """在这里填写API描述"""

	rest {
		request {
			url "GET /v1/vpc/virtual-routers/networkservicestate/snat"
			url "GET /v1/vpc/virtual-routers/networkservicestate/snat/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryVpcSnatStateMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryVpcSnatStateReply.class
		}
	}
}