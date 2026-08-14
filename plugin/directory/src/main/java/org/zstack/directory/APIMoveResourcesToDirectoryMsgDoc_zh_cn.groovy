package org.zstack.directory

import org.zstack.directory.APIMoveResourcesToDirectoryEvent

doc {
	title "MoveResourcesToDirectory"

	category "directory"

	desc """移动资源到指定目录"""

	rest {
		request {
			url "PUT /v1/move/resources/directory"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIMoveResourcesToDirectoryMsg.class

			desc """"""

			params {

				column {
					name "resourceUuids"
					enclosedIn "moveResourcesToDirectory"
					desc "批量资源UUID"
					location "body"
					type "List"
					optional false
					since "3.16.0"
				}
				column {
					name "directoryUuid"
					enclosedIn "moveResourcesToDirectory"
					desc "目录UUID"
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
			clz APIMoveResourcesToDirectoryEvent.class
		}
	}
}