package org.zstack.zwatch.thirdparty.api

import org.zstack.zwatch.thirdparty.api.APIUpdateThirdpartyPlatformEvent

doc {
	title "UpdateThirdpartyPlatform"

	category "zwatch"

	desc """修改第三方报警源"""

	rest {
		request {
			url "PUT /v1/zwatch/third-party/platforms/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateThirdpartyPlatformMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateThirdpartyPlatform"
					desc "平台UUID"
					location "url"
					type "String"
					optional false
					since "3.10"
				}
				column {
					name "name"
					enclosedIn "updateThirdpartyPlatform"
					desc "平台名称"
					location "body"
					type "String"
					optional true
					since "3.10"
				}
				column {
					name "description"
					enclosedIn "updateThirdpartyPlatform"
					desc "平台详细描述"
					location "body"
					type "String"
					optional true
					since "3.10"
				}
				column {
					name "template"
					enclosedIn "updateThirdpartyPlatform"
					desc "消息转换模板"
					location "body"
					type "String"
					optional true
					since "3.10"
				}
				column {
					name "url"
					enclosedIn "updateThirdpartyPlatform"
					desc "平台地址"
					location "body"
					type "String"
					optional true
					since "3.10"
				}
				column {
					name "stateEvent"
					enclosedIn "updateThirdpartyPlatform"
					desc "平台启用状态"
					location "body"
					type "String"
					optional true
					since "3.10"
					values ("enable","disable")
				}
				column {
					name "lastSyncDateMills"
					enclosedIn "updateThirdpartyPlatform"
					desc "上一次同步消息时间"
					location "body"
					type "Long"
					optional true
					since "3.10"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.10"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.10"
				}
			}
		}

		response {
			clz APIUpdateThirdpartyPlatformEvent.class
		}
	}
}