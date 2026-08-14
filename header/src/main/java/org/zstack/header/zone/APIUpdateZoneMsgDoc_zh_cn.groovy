package org.zstack.header.zone

import org.zstack.header.zone.APIUpdateZoneEvent

doc {
	title "更新区域（UpdateZone）"

	category "zone"

	desc """更新区域的名称、描述、系统标签或者用户标签"""

	rest {
		request {
			url "PUT /v1/zones/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateZoneMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "updateZone"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "description"
					enclosedIn "updateZone"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "uuid"
					enclosedIn "updateZone"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
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
				column {
					name "isDefault"
					enclosedIn "updateZone"
					desc ""
					location "body"
					type "Boolean"
					optional true
					since "zsv 3.16.31"
				}
			}
		}

		response {
			clz APIUpdateZoneEvent.class
		}
	}
}