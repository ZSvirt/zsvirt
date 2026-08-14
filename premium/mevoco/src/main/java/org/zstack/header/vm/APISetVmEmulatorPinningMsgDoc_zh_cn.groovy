package org.zstack.header.vm

import org.zstack.header.vm.APISetVmEmulatorPinningEvent

doc {
	title "SetVmEmulatorPinning"

	category "mevoco"

	desc """设置云主机EmulatorPinning的Host CPU"""

	rest {
		request {
			url "PUT /v1/vm-instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISetVmEmulatorPinningMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "setVmEmulatorPinning"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "emulatorPinning"
					enclosedIn "setVmEmulatorPinning"
					desc ""
					location "body"
					type "String"
					optional false
					since "3.13.12"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.13.12"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.13.12"
				}
			}
		}

		response {
			clz APISetVmEmulatorPinningEvent.class
		}
	}
}