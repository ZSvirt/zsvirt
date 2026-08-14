package org.zstack.header.host

import org.zstack.header.host.APIGetHypervisorTypesReply

doc {
	title "GetHypervisorTypes"

	category "host"

	desc """获取云主机虚拟化技术类型"""

	rest {
		request {
			url "GET /v1/hosts/hypervisor-types"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetHypervisorTypesMsg.class

			desc """获取云主机的虚拟化技术类型"""

			params {

				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APIGetHypervisorTypesReply.class
		}
	}
}