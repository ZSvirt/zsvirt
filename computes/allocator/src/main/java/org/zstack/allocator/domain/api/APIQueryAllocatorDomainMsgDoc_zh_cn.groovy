package org.zstack.allocator.domain.api

import org.zstack.allocator.domain.api.APIQueryAllocatorDomainReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryAllocatorDomain"

	category "allocator.domain"

	desc """查询调度域。Inventory 展开 rules、scenes；虚拟机列表请用 GetAllocatorDomainVms"""

	rest {
		request {
			url "GET /v1/allocator-domains"
			url "GET /v1/allocator-domains/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryAllocatorDomainMsg.class

			desc """查询调度域"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryAllocatorDomainReply.class
		}
	}
}