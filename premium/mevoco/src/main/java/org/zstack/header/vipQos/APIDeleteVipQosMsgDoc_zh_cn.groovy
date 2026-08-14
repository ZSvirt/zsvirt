package org.zstack.header.vipQos

import org.zstack.header.vipQos.APIDeleteVipQosEvent

doc {
	title "DeleteVipQos"

	category "VipQos"

	desc """删除VIPQos"""

	rest {
		request {
			url "DELETE /v1/vips/{uuid}/vip-qos"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteVipQosMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.2"
				}
				column {
					name "port"
					enclosedIn ""
					desc ""
					location "query"
					type "Integer"
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
			clz APIDeleteVipQosEvent.class
		}
	}
}