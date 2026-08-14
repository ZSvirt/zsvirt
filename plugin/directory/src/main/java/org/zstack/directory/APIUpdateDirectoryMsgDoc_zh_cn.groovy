package org.zstack.directory

import org.zstack.directory.APIUpdateDirectoryEvent

doc {
	title "UpdateDirectory"

	category "directory"

	desc """更新目录名称"""

	rest {
		request {
			url "PUT /v1/update/directory"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateDirectoryMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateDirectory"
					desc "资源的UUID，唯一标示该资源"
					location "body"
					type "String"
					optional false
					since "3.16.0"
				}
				column {
					name "name"
					enclosedIn "updateDirectory"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "3.16.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.16.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.16.0"
				}
			}
		}

		response {
			clz APIUpdateDirectoryEvent.class
		}
	}
}