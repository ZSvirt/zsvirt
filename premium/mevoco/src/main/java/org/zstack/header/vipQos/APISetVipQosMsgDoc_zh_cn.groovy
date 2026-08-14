package org.zstack.header.vipQos

import org.zstack.header.vipQos.APISetVipQosEvent

doc {
	title "SetVipQos"

	category "VipQos"

	desc """添加VipQos"""

	rest {
		request {
			url "PUT /v1/vips/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISetVipQosMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "setVipQos"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.2"
				}
				column {
					name "port"
					enclosedIn "setVipQos"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "2.2"
				}
				column {
					name "outboundBandwidth"
					enclosedIn "setVipQos"
					desc ""
					location "body"
					type "Long"
					optional true
					since "2.2"
				}
				column {
					name "inboundBandwidth"
					enclosedIn "setVipQos"
					desc ""
					location "body"
					type "Long"
					optional true
					since "2.2"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "2.2"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "2.2"
				}
			}
		}

		response {
			clz APISetVipQosEvent.class
		}
	}
}