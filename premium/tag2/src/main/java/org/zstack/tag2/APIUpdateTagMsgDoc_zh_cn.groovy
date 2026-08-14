package org.zstack.tag2

import org.zstack.tag2.APIUpdateTagEvent

doc {
	title "UpdateTag"

	category "tag2"

	desc """在这里填写API描述"""

	rest {
		request {
			url "PUT /v1/tags/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateTagMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateTag"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.2.0"
				}
				column {
					name "name"
					enclosedIn "updateTag"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "3.2.0"
				}
				column {
					name "value"
					enclosedIn "updateTag"
					desc "标签的值"
					location "body"
					type "String"
					optional true
					since "3.2.0"
				}
				column {
					name "description"
					enclosedIn "updateTag"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.2.0"
				}
				column {
					name "color"
					enclosedIn "updateTag"
					desc "标签的颜色"
					location "body"
					type "String"
					optional true
					since "3.2.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.2.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.2.0"
				}
			}
		}

		response {
			clz APIUpdateTagEvent.class
		}
	}
}