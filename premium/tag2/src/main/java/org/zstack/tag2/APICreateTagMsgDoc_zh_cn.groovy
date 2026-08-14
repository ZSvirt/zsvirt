package org.zstack.tag2

import org.zstack.tag2.APICreateTagEvent

doc {
	title "CreateTag"

	category "tag2"

	desc """创建一个标签"""

	rest {
		request {
			url "POST /v1/tags"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateTagMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "3.2.0"
				}
				column {
					name "value"
					enclosedIn "params"
					desc "标签的值"
					location "body"
					type "String"
					optional false
					since "3.2.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.2.0"
				}
				column {
					name "color"
					enclosedIn "params"
					desc "标签的颜色"
					location "body"
					type "String"
					optional true
					since "3.2.0"
				}
				column {
					name "type"
					enclosedIn "params"
					desc "标签的类型"
					location "body"
					type "String"
					optional true
					since "3.2.0"
					values ("simple","withToken")
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
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
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
			}
		}

		response {
			clz APICreateTagEvent.class
		}
	}
}