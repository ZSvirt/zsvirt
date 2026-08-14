package org.zstack.header.vm

import org.zstack.header.vm.APIGetVmEmulatorPinningReply

doc {
	title "GetVmEmulatorPinning"

	category "mevoco"

	desc """获取虚拟机EmulatorPinning绑定的Host CPU"""

	rest {
		request {
			url "GET /v1/vm-instances/{uuid}/emulator-pinning"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVmEmulatorPinningMsg.class

			desc """获取云主机EmulatorPinning绑定的Host CPU"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "云主机的UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIGetVmEmulatorPinningReply.class
		}
	}
}