package org.zstack.allocator.domain.api

import org.zstack.allocator.domain.api.APIDeleteAllocatorDomainEvent

doc {
	title "DeleteAllocatorDomain"

	category "allocator.domain"

	desc """删除调度域。仍有虚拟机的配置值或当前值引用该域时失败；deleteMode=force 时允许清理引用后删除"""

	rest {
		request {
			url "DELETE /v1/allocator-domains/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteAllocatorDomainMsg.class

			desc """删除调度域"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "1.0.1, zsv_5.2.0"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive / Enforcing，Permissive)"
					location "query"
					type "String"
					optional true
					since "1.0.1, zsv_5.2.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "1.0.1, zsv_5.2.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "1.0.1, zsv_5.2.0"
				}
			}
		}

		response {
			clz APIDeleteAllocatorDomainEvent.class
		}
	}
}