package org.zstack.header.vm

import org.zstack.header.vm.APISetNicQosEvent

doc {
	title "SetNicQos"

	category "mevoco"

	desc """设置云主机网卡限速"""

	rest {
		request {
			url "PUT /v1/vm-instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISetNicQosMsg.class

			desc """设置云主机网卡限速"""

			params {

				column {
					name "uuid"
					enclosedIn "setNicQos"
					desc "云主机网卡UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "outboundBandwidth"
					enclosedIn "setNicQos"
					desc "出流量带宽限制"
					location "body"
					type "Long"
					optional true
					since "0.6"
				}
				column {
					name "inboundBandwidth"
					enclosedIn "setNicQos"
					desc "入流量带宽限制"
					location "body"
					type "Long"
					optional true
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
			clz APISetNicQosEvent.class
		}
	}
}