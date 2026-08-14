package org.zstack.header.vm

import org.zstack.header.vm.APIGetVmConsolePasswordReply

doc {
	title "获取云主机控制台密码(GetVmConsolePassword)"

	category "vmInstance"

	desc """获取一个云主机的控制台密码"""

	rest {
		request {
			url "GET /v1/vm-instances/{uuid}/console-passwords"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVmConsolePasswordMsg.class

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
			clz APIGetVmConsolePasswordReply.class
		}
	}
}