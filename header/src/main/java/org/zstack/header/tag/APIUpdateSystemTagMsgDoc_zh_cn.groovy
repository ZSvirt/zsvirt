package org.zstack.header.tag

import org.zstack.header.tag.APIUpdateSystemTagEvent

doc {
	title "更新系统标签（UpdateSystemTag)"

	category "tag"

	desc """更新系统标签"""

	rest {
		request {
			url "PUT /v1/system-tags/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateSystemTagMsg.class

			desc """更新系统标签"""

			params {

				column {
					name "uuid"
					enclosedIn "updateSystemTag"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "tag"
					enclosedIn "updateSystemTag"
					desc "标签字符串"
					location "body"
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
			}
		}

		response {
			clz APIUpdateSystemTagEvent.class
		}
	}
}