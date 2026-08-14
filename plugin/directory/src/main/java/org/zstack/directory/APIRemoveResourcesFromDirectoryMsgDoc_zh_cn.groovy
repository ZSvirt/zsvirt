package org.zstack.directory

import org.zstack.directory.APIRemoveResourcesFromDirectoryEvent

doc {
	title "RemoveResourcesFromDirectory"

	category "directory"

	desc """资源从指定目录中移除"""

	rest {
		request {
			url "DELETE /v1/remove/resources/directory"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRemoveResourcesFromDirectoryMsg.class

			desc """"""

			params {

				column {
					name "resourceUuids"
					enclosedIn ""
					desc "批量资源UUID"
					location "query"
					type "List"
					optional false
					since "3.16.0"
				}
				column {
					name "directoryUuid"
					enclosedIn ""
					desc "目录UUID"
					location "query"
					type "String"
					optional false
					since "3.16.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.16.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.16.0"
				}
			}
		}

		response {
			clz APIRemoveResourcesFromDirectoryEvent.class
		}
	}
}