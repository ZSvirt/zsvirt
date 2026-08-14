package org.zstack.header.host

import org.zstack.header.host.APIGetHostResourceAllocationEvent

doc {
	title "获取一次物理机的CPU分配"

	category "host"

	desc """null"""

	rest {
		request {
			url "POST /v1/hosts/{uuid}/resource-allocation"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetHostResourceAllocationMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "params"
					desc "物理机UUID"
					location "url"
					type "String"
					optional false
					since "3.13.12"
				}
				column {
					name "strategy"
					enclosedIn "params"
					desc "分配策略(可选值: continuous)"
					location "body"
					type "String"
					optional false
					since "3.13.12"
				}
				column {
					name "scene"
					enclosedIn "params"
					desc "分配场景(normal,performance)"
					location "body"
					type "String"
					optional false
					since "3.13.12"
				}
				column {
					name "vcpuNum"
					enclosedIn "params"
					desc "需要的vCPU数量(1-512)"
					location "body"
					type "Integer"
					optional false
					since "3.13.12"
				}
				column {
					name "memSize"
					enclosedIn "params"
					desc "需要的内存大小(1-Long.MAX_VALUE)"
					location "body"
					type "Long"
					optional true
					since "3.13.12"
				}
				column {
					name "vcpu"
					enclosedIn "params"
					desc ""
					location "body"
					type "int"
					optional false
					since "3.13.12"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.13.12"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.13.12"
				}
			}
		}

		response {
			clz APIGetHostResourceAllocationEvent.class
		}
	}
}