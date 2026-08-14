package org.zstack.header.vm

import org.zstack.header.vm.APISetVmQgaEvent

doc {
	title "SetVmQga"

	category "mevoco"

	desc """在这里填写API描述"""

	rest {
		request {
			url "PUT /v1/vm-instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISetVmQgaMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "setVmQga"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "enable"
					enclosedIn "setVmQga"
					desc ""
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
			clz APISetVmQgaEvent.class
		}
	}
}