package org.zstack.allocator.domain.api

import org.zstack.allocator.domain.api.APIUpdateAllocatorDomainEvent

doc {
	title "UpdateAllocatorDomain"

	category "allocator.domain"

	desc """更新调度域。带 rules 或 scenes 时整表重写对应子表。不支持修改 zoneUuid"""

	rest {
		request {
			url "PUT /v1/allocator-domains/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateAllocatorDomainMsg.class

			desc """更新调度域"""

			params {

				column {
					name "uuid"
					enclosedIn "updateAllocatorDomain"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "1.0.1, zsv_5.2.0"
				}
				column {
					name "name"
					enclosedIn "updateAllocatorDomain"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "1.0.1, zsv_5.2.0"
				}
				column {
					name "description"
					enclosedIn "updateAllocatorDomain"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "1.0.1, zsv_5.2.0"
				}
				column {
					name "state"
					enclosedIn "updateAllocatorDomain"
					desc "Enabled 时对已关联虚拟机应用规则；Disabled 时本次不约束"
					location "body"
					type "String"
					optional true
					since "1.0.1, zsv_5.2.0"
					values ("Enabled","Disabled")
				}
				column {
					name "rules"
					enclosedIn "updateAllocatorDomain"
					desc "有序规则列表，顺序即求值顺序。设置该字段时整表重写，列表下标写入 priority，从 0 开始"
					location "body"
					type "List"
					optional true
					since "1.0.1, zsv_5.2.0"
				}
				column {
					name "scenes"
					enclosedIn "updateAllocatorDomain"
					desc "规则生效场景。设置该字段时整表重写"
					location "body"
					type "List"
					optional true
					since "1.0.1, zsv_5.2.0"
					values ("NewCreate","Start","Migrate","Ha","HostMaintenance","Drs")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "1.0.1, zsv_5.2.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "1.0.1, zsv_5.2.0"
				}
			}
		}

		response {
			clz APIUpdateAllocatorDomainEvent.class
		}
	}
}