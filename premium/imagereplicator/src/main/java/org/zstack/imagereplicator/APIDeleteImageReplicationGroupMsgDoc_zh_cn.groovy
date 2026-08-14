package org.zstack.imagereplicator

import org.zstack.imagereplicator.APIDeleteImageReplicationGroupEvent

doc {
	title "DeleteImageReplicationGroup"

	category "imagereplicator"

	desc """删除镜像复制组"""

	rest {
		request {
			url "DELETE /v1/image-replication-groups/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteImageReplicationGroupMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "镜像复制组的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.5"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
					since "3.5"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.5"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.5"
				}
			}
		}

		response {
			clz APIDeleteImageReplicationGroupEvent.class
		}
	}
}