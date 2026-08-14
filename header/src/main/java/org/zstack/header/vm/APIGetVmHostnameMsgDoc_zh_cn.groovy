package org.zstack.header.vm

import org.zstack.header.vm.APIGetVmHostnameReply

doc {
	title "获取云主机Hostname(GetVmHostname)"

	category "vmInstance"

	desc """获取云主机指定的Hostname。该Hostname是用户之前用SetVmHostname指定的。"""

	rest {
		request {
			url "GET /v1/vm-instances/{uuid}/hostnames"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVmHostnameMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
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
			clz APIGetVmHostnameReply.class
		}
	}
}