package org.zstack.header.vm

import org.zstack.header.vm.APIGetVmConsoleAddressReply

doc {
	title "获取云主机控制台地址(GetVmConsoleAddress)"

	category "vmInstance"

	desc """获取云主机控制台地址和访问协议

>该API应该仅仅用于调试目的
"""

	rest {
		request {
			url "GET /v1/vm-instances/{uuid}/console-addresses"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVmConsoleAddressMsg.class

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
			clz APIGetVmConsoleAddressReply.class
		}
	}
}