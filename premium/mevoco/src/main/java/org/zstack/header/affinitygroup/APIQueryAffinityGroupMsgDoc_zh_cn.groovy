package org.zstack.header.affinitygroup

import org.zstack.header.affinitygroup.APIQueryAffinityGroupReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryAffinityGroup"

	category "affinityGroup"

	desc """获取亲和组清单"""

	rest {
		request {
			url "GET /v1/affinity-groups"
			url "GET /v1/affinity-groups/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryAffinityGroupMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryAffinityGroupReply.class
		}
	}
}