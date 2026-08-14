package org.zstack.network.service.vip

import org.zstack.network.service.vip.APIUpdateVipEvent

doc {
	title "UpdateVip"

	category "vip"

	desc """更新VIP"""

	rest {
		request {
			url "PUT /v1/vips/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateVipMsg.class

			desc """更新VIP"""

			params {

				column {
					name "uuid"
					enclosedIn "updateVip"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "name"
					enclosedIn "updateVip"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "updateVip"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIUpdateVipEvent.class
		}
	}
}