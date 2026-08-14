package org.zstack.network.l2.vxlan.vxlanNetworkPool

import org.zstack.network.l2.vxlan.vxlanNetworkPool.APIUpdateVniRangeEvent

doc {
	title "修改VNI范围(UpdateVniRange)"

	category "network.l2"

	desc """修改VNI范围"""

	rest {
		request {
			url "PUT /v1/l2-networks/vxlan-pool/vni-ranges/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateVniRangeMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateVniRange"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.3.0"
				}
				column {
					name "name"
					enclosedIn "updateVniRange"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "3.3.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.3.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.3.0"
				}
			}
		}

		response {
			clz APIUpdateVniRangeEvent.class
		}
	}
}