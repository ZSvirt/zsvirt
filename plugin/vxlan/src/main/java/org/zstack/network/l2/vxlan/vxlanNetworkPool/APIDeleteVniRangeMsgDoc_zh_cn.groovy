package org.zstack.network.l2.vxlan.vxlanNetworkPool

import org.zstack.network.l2.vxlan.vxlanNetworkPool.APIDeleteVniRangeEvent

doc {
	title "删除VNI范围(DeleteVniRange)"

	category "network.l2"

	desc """删除VNI范围"""

	rest {
		request {
			url "DELETE /v1/l2-networks/vxlan-pool/vni-ranges/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteVniRangeMsg.class

			desc """"""

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
					name "deleteMode"
					enclosedIn ""
					desc "删除模式"
					location "query"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIDeleteVniRangeEvent.class
		}
	}
}