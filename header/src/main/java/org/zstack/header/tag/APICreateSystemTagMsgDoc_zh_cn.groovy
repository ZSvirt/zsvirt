package org.zstack.header.tag

import org.zstack.header.tag.APICreateSystemTagEvent

doc {
	title "CreateSystemTag"

	category "tag"

	desc """创建系统标签"""

	rest {
		request {
			url "POST /v1/system-tags"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateSystemTagMsg.class

			desc """创建系统标签"""

			params {

				column {
					name "resourceType"
					enclosedIn "params"
					desc "当创建一个标签时, 用户必须制定标签所关联的资源类型"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "用户指定的资源UUID，若指定，系统不会为该资源随机分配UUID"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "tag"
					enclosedIn "params"
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
			clz APICreateSystemTagEvent.class
		}
	}
}