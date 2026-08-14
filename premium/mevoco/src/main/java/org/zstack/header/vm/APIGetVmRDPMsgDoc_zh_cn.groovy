package org.zstack.header.vm

import org.zstack.header.vm.APIGetVmRDPReply

doc {
	title "GetVmRDP"

	category "mevoco"

	desc """获取VM RDP开关状态,返回值为true或者false"""

	rest {
		request {
			url "GET /v1/vm-instances/{uuid}/rdp"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVmRDPMsg.class

			desc """获取VM RDP开关状态,返回值为true或者false"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
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
			clz APIGetVmRDPReply.class
		}
	}
}