package org.zstack.header.vpc.ha

import org.zstack.header.vpc.ha.APIQueryVpcHaGroupReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryVpcHaGroup"

	category "vpcHa"

	desc """查询高可用"""

	rest {
		request {
			url "GET /v1/vpc/hagroups"
			url "GET /v1/vpc/hagroups/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryVpcHaGroupMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryVpcHaGroupReply.class
		}
	}
}