package org.zstack.allocator.domain.api

import org.zstack.allocator.domain.api.APIGetAllocatorDomainCandidatesReply

doc {
	title "GetAllocatorDomainCandidates"

	category "allocator.domain"

	desc """按区域返回可加入调度域的 host / cluster / 主存储 / 镜像存储 / L3 候选（已挂到该区域、状态可用）。L3 候选含 PortGroup"""

	rest {
		request {
			url "GET /v1/allocator-domains/candidates"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetAllocatorDomainCandidatesMsg.class

			desc """获取可加入调度域的候选资源"""

			params {

				column {
					name "zoneUuid"
					enclosedIn ""
					desc "区域UUID"
					location "query"
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
			clz APIGetAllocatorDomainCandidatesReply.class
		}
	}
}