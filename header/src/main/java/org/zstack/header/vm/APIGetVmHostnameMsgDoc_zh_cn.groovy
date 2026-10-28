package org.zstack.header.vm

import org.zstack.header.vm.APIGetVmHostnameReply

doc {
	title "获取虚拟机Hostname(GetVmHostname)"

	category "vmInstance"

	desc """获取虚拟机指定的Hostname。该Hostname是用户之前用SetVmHostname指定的。"""

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
					desc "虚拟机UUID"
					location "url"
					type "String"
					optional false
					since "zsv_0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv_0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv_0.6"
				}
			}
		}

		response {
			clz APIGetVmHostnameReply.class
		}
	}
}