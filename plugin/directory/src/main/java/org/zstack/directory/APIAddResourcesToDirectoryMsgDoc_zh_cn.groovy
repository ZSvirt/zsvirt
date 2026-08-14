package org.zstack.directory

import org.zstack.directory.APIAddResourcesToDirectoryEvent

doc {
	title "AddResourcesToDirectory"

	category "directory"

	desc """资源加入指定目录"""

	rest {
		request {
			url "POST /v1/add/resources/directory"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddResourcesToDirectoryMsg.class

			desc """"""

			params {

				column {
					name "resourceUuids"
					enclosedIn "params"
					desc "批量资源UUID"
					location "body"
					type "List"
					optional false
					since "3.16.0"
				}
				column {
					name "directoryUuid"
					enclosedIn "params"
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
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIAddResourcesToDirectoryEvent.class
		}
	}
}