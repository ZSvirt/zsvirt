package org.zstack.directory

import org.zstack.directory.APIMoveDirectoryEvent

doc {
	title "MoveDirectory"

	category "directory"

	desc """移动目录"""

	rest {
		request {
			url "PUT /v1/move/directory"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIMoveDirectoryMsg.class

			desc """"""

			params {

				column {
					name "targetParentUuid"
					enclosedIn "moveDirectory"
					desc ""
					location "body"
					type "String"
					optional false
					since "3.16.0"
				}
				column {
					name "directoryUuid"
					enclosedIn "moveDirectory"
					desc ""
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
			clz APIMoveDirectoryEvent.class
		}
	}
}