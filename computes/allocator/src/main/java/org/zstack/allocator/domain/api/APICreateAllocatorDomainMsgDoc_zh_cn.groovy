package org.zstack.allocator.domain.api

import org.zstack.allocator.domain.api.APICreateAllocatorDomainEvent

doc {
	title "CreateAllocatorDomain"

	category "allocator.domain"

	desc """创建调度域"""

	rest {
		request {
			url "POST /v1/allocator-domains"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateAllocatorDomainMsg.class

			desc """创建调度域。rules 为空则该域对任何资源类型都不约束。"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "1.0.1, zsv_5.2.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "1.0.1, zsv_5.2.0"
				}
				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID。域内规则引用的资源必须属于该区域"
					location "body"
					type "String"
					optional false
					since "1.0.1, zsv_5.2.0"
				}
				column {
					name "rules"
					enclosedIn "params"
					desc "有序规则列表，顺序即求值顺序（自上而下、先匹配先生效，类似 iptables）。落库时列表下标写入 priority，从 0 开始"
					location "body"
					type "List"
					optional true
					since "1.0.1, zsv_5.2.0"
				}
				column {
					name "scenes"
					enclosedIn "params"
					desc "规则生效场景。行存在即启用"
					location "body"
					type "List"
					optional true
					since "1.0.1, zsv_5.2.0"
					values ("NewCreate","Start","Migrate","Ha","HostMaintenance","Drs")
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "1.0.1, zsv_5.2.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "1.0.1, zsv_5.2.0"
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
			clz APICreateAllocatorDomainEvent.class
		}
	}
}