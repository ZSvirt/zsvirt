package org.zstack.header.network.l3

import org.zstack.header.network.l3.APIUpdateIpRangeEvent

doc {
	title "更新IP地址范围(UpdateIpRange)"

	category "三层网络"

	desc """更新IP地址范围"""

	rest {
		request {
			url "PUT /v1/l3-networks/ip-ranges/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateIpRangeMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateIpRange"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "name"
					enclosedIn "updateIpRange"
					desc "三层网络的名称"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "description"
					enclosedIn "updateIpRange"
					desc "三层网络的详细描述"
					location "body"
					type "String"
					optional true
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
			clz APIUpdateIpRangeEvent.class
		}
	}
}