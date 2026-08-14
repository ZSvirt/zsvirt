package org.zstack.directory

import org.zstack.directory.APICreateDirectoryEvent

doc {
	title "CreateDirectory"

	category "directory"

	desc """创建目录"""

	rest {
		request {
			url "POST /v1/create/directory"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateDirectoryMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "3.16.0"
				}
				column {
					name "parentUuid"
					enclosedIn "params"
					desc "父级目录UUID"
					location "body"
					type "String"
					optional true
					since "3.16.0"
				}
				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID"
					location "body"
					type "String"
					optional false
					since "3.16.0"
				}
				column {
					name "type"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "3.16.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "3.16.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
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
			clz APICreateDirectoryEvent.class
		}
	}
}