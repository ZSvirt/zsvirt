package org.zstack.header.network.l2

import org.zstack.header.network.l2.APIUpdateL2NetworkVirtualNetworkIdEvent

doc {
	title "UpdateL2NetworkVirtualNetworkId"

	category "network.l2"

	desc """更新二层网络虚拟网络ID"""

	rest {
		request {
			url "PUT /v1/l2-networks/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateL2NetworkVirtualNetworkIdMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateL2NetworkVirtualNetworkId"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 4.10.0"
				}
				column {
					name "virtualNetworkId"
					enclosedIn "updateL2NetworkVirtualNetworkId"
					desc "虚拟网络ID，vlanId或vni"
					location "body"
					type "Integer"
					optional false
					since "zsv 4.10.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 4.10.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 4.10.0"
				}
			}
		}

		response {
			clz APIUpdateL2NetworkVirtualNetworkIdEvent.class
		}
	}
}