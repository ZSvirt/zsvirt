package org.zstack.header.vipQos

import org.zstack.header.vipQos.APIGetVipQosReply

doc {
	title "GetVipQos"

	category "VipQos"

	desc """查询VIPQos"""

	rest {
		request {
			url "GET /v1/vip/{uuid}/vip-qos"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVipQosMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional true
					since "2.2"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "2.2"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "2.2"
				}
			}
		}

		response {
			clz APIGetVipQosReply.class
		}
	}
}