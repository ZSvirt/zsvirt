package org.zstack.header.host

import org.zstack.header.host.APIAllocateHostResourceEvent

doc {
	title "AllocateHostResource"

	category "host"

	desc """分配物理机计算资源"""

	rest {
		request {
			url "POST /v1/hosts/{uuid}/allocate-resource"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAllocateHostResourceMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "params"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.17.11"
				}
				column {
					name "strategy"
					enclosedIn "params"
					desc "分配策略"
					location "body"
					type "String"
					optional false
					since "3.17.11"
					values ("continuous")
				}
				column {
					name "scene"
					enclosedIn "params"
					desc "分配场景"
					location "body"
					type "String"
					optional false
					since "3.17.11"
					values ("normal","performance")
				}
				column {
					name "vcpu"
					enclosedIn "params"
					desc "需要的vCPU数量"
					location "body"
					type "int"
					optional false
					since "3.17.11"
				}
				column {
					name "memSize"
					enclosedIn "params"
					desc "需要的内存大小"
					location "body"
					type "Long"
					optional true
					since "3.17.11"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.17.11"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.17.11"
				}
			}
		}

		response {
			clz APIAllocateHostResourceEvent.class
		}
	}
}