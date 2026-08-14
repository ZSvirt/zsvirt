package org.zstack.header.network.service

import org.zstack.header.network.service.APIAttachNetworkServiceToL3NetworkEvent

doc {
	title "挂载网络服务到三层网络(AttachNetworkServiceToL3Network)"

	category "三层网络"

	desc """挂载网络服务到三层网络"""

	rest {
		request {
			url "POST /v1/l3-networks/{l3NetworkUuid}/network-services"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAttachNetworkServiceToL3NetworkMsg.class

			desc """"""

			params {

				column {
					name "l3NetworkUuid"
					enclosedIn "params"
					desc "三层网络UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "networkServices"
					enclosedIn "params"
					desc "网络服务，接口参数支持使用NetworkServiceProviderUuid或NetworkServiceProviderType"
					location "body"
					type "Map"
					optional false
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APIAttachNetworkServiceToL3NetworkEvent.class
		}
	}
}