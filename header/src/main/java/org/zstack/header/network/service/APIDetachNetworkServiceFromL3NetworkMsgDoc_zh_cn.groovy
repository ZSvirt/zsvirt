package org.zstack.header.network.service

import org.zstack.header.network.service.APIDetachNetworkServiceFromL3NetworkEvent

doc {
	title "从三层网络卸载网络服务(DetachNetworkServiceFromL3Network)"

	category "三层网络"

	desc """从三层网络卸载网络服务"""

	rest {
		request {
			url "DELETE /v1/l3-networks/{l3NetworkUuid}/network-services"
			url "DELETE /v1/l3-networks/{l3NetworkUuid}/network-services/{service}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDetachNetworkServiceFromL3NetworkMsg.class

			desc """"""

			params {

				column {
					name "l3NetworkUuid"
					enclosedIn ""
					desc "三层网络UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "networkServices"
					enclosedIn ""
					desc "网络服务"
					location "query"
					type "Map"
					optional true
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "service"
					enclosedIn ""
					desc "服务类型"
					location "query"
					type "String"
					optional true
					since "zsv 4.10.16"
				}
			}
		}

		response {
			clz APIDetachNetworkServiceFromL3NetworkEvent.class
		}
	}
}