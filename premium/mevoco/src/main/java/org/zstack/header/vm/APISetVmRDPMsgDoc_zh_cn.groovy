package org.zstack.header.vm

import org.zstack.header.vm.APISetVmRDPEvent

doc {
	title "SetVmRDP"

	category "mevoco"

	desc """设置云主机是否开启了RDP功能,如果开启，桌面云会使用RDP连接该云主机"""

	rest {
		request {
			url "PUT /v1/vm-instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISetVmRDPMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "setVmRDP"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "enable"
					enclosedIn "setVmRDP"
					desc "云主机是否被标识为RDP可访问"
					location "body"
					type "boolean"
					optional false
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APISetVmRDPEvent.class
		}
	}
}