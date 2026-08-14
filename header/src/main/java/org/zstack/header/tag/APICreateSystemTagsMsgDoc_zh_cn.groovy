package org.zstack.header.tag

import org.zstack.header.tag.APICreateSystemTagsEvent

doc {
	title "CreateSystemTags"

	category "tag"

	desc """批量创建系统标签"""

	rest {
		request {
			url "POST /v1/system-tags/{resourceUuid}/tags"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateSystemTagsMsg.class

			desc """"""

			params {

				column {
					name "resourceType"
					enclosedIn "params"
					desc "当创建一个标签时, 用户必须制定标签所关联的资源类型"
					location "body"
					type "String"
					optional false
					since "zsv 3.17.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "用户指定的资源UUID，若指定，系统不会为该资源随机分配UUID"
					location "url"
					type "String"
					optional false
					since "zsv 3.17.0"
				}
				column {
					name "tags"
					enclosedIn "params"
					desc "标签字符串列表"
					location "body"
					type "List"
					optional false
					since "zsv 3.17.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.17.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.17.0"
				}
			}
		}

		response {
			clz APICreateSystemTagsEvent.class
		}
	}
}