package org.zstack.network.service.eip

import org.zstack.network.service.eip.APIUpdateEipEvent

doc {
	title "更新弹性IP(UpdateEip)"

	category "弹性IP"

	desc """更新弹性IP"""

	rest {
		request {
			url "PUT /v1/eips/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateEipMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateEip"
					desc "弹性IP的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "name"
					enclosedIn "updateEip"
					desc "弹性IP名称"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "updateEip"
					desc "弹性IP的详细描述"
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
			clz APIUpdateEipEvent.class
		}
	}
}