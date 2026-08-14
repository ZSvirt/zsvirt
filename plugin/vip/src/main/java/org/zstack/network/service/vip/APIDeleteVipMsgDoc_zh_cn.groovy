package org.zstack.network.service.vip

import org.zstack.network.service.vip.APIDeleteVipEvent

doc {
	title "DeleteVip"

	category "vip"

	desc """删除VIP"""

	rest {
		request {
			url "DELETE /v1/vips/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteVipMsg.class

			desc """删除VIP"""

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
			clz APIDeleteVipEvent.class
		}
	}
}