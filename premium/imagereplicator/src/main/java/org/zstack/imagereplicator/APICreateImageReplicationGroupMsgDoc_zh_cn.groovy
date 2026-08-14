package org.zstack.imagereplicator

import org.zstack.imagereplicator.APICreateImageReplicationGroupEvent

doc {
	title "CreateImageReplicationGroup"

	category "imagereplicator"

	desc """创建镜像复制组"""

	rest {
		request {
			url "POST /v1/image-replication-groups"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateImageReplicationGroupMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "镜像复制组名称"
					location "body"
					type "String"
					optional false
					since "3.5"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "镜像复制组的详细描述"
					location "body"
					type "String"
					optional true
					since "3.5"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "3.5"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.5"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.5"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.5"
				}
			}
		}

		response {
			clz APICreateImageReplicationGroupEvent.class
		}
	}
}