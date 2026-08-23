package org.zstack.allocator.domain.api

import org.zstack.allocator.domain.api.APIGetAllocatorDomainVmsReply

doc {
	title "GetAllocatorDomainVms"

	category "allocator.domain"

	desc """按调度域查询关联虚拟机。返回当前在域中的列表，以及下次启动会进入该域且不在前一列表中的虚拟机"""

	rest {
		request {
			url "GET /v1/allocator-domains/{uuid}/vms"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetAllocatorDomainVmsMsg.class

			desc """按调度域查询关联虚拟机"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "调度域UUID"
					location "url"
					type "String"
					optional false
					since "1.0.1, zsv 5.2.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "1.0.1, zsv 5.2.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "1.0.1, zsv 5.2.0"
				}
			}
		}

		response {
			clz APIGetAllocatorDomainVmsReply.class
		}
	}
}