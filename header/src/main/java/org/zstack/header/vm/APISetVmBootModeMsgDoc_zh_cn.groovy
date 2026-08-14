package org.zstack.header.vm

import org.zstack.header.vm.APISetVmBootModeEvent

doc {
	title "SetVmBootMode"

	category "vmInstance"

	desc """设置云主机启动模式"""

	rest {
		request {
			url "PUT /v1/vm-instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISetVmBootModeMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "setVmBootMode"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "bootMode"
					enclosedIn "setVmBootMode"
					desc ""
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APISetVmBootModeEvent.class
		}
	}
}