package org.zstack.zwatch.thirdparty.api

import org.zstack.zwatch.thirdparty.api.APIAddThirdpartyPlatformEvent

doc {
	title "AddThirdpartyPlatform"

	category "zwatch"

	desc """添加第三方报警源"""

	rest {
		request {
			url "POST /v1/zwatch/third-party/platforms"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddThirdpartyPlatformMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "平台名称"
					location "body"
					type "String"
					optional false
					since "3.10"
				}
				column {
					name "type"
					enclosedIn "params"
					desc "平台类型"
					location "body"
					type "String"
					optional false
					since "3.10"
				}
				column {
					name "url"
					enclosedIn "params"
					desc "平台地址"
					location "body"
					type "String"
					optional false
					since "3.10"
				}
				column {
					name "template"
					enclosedIn "params"
					desc "报警转换模板"
					location "body"
					type "String"
					optional false
					since "3.10"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.10"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "3.10"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
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
			clz APIAddThirdpartyPlatformEvent.class
		}
	}
}