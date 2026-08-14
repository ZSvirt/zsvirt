package org.zstack.header.zone

import org.zstack.header.zone.APICreateZoneEvent

doc {
	title "创建一个新的区域(CreateZone)"

	category "zone"

	desc """创建一个新的区域"""

	rest {
		request {
			url "POST /v1/zones"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateZoneMsg.class

			desc """创建一个新的区域"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "用户指定的资源uuid"
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
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "zsv 3.4.0"
				}
				column {
					name "isDefault"
					enclosedIn "params"
					desc ""
					location "body"
					type "Boolean"
					optional true
					since "zsv 3.16.31"
				}
			}
		}

		response {
			clz APICreateZoneEvent.class
		}
	}
}