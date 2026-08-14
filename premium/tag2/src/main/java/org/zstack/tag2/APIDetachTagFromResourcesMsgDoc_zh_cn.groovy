package org.zstack.tag2

import org.zstack.tag2.APIDetachTagFromResourcesEvent

doc {
	title "DetachTagFromResources"

	category "tag2"

	desc """从资源上卸载标签"""

	rest {
		request {
			url "DELETE /v1/tags/{tagUuid}/resources"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDetachTagFromResourcesMsg.class

			desc """"""

			params {

				column {
					name "tagUuid"
					enclosedIn ""
					desc "标签UUID"
					location "url"
					type "String"
					optional false
					since "3.2.0"
				}
				column {
					name "resourceUuids"
					enclosedIn ""
					desc "资源UUID"
					location "query"
					type "List"
					optional false
					since "3.2.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.2.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.2.0"
				}
			}
		}

		response {
			clz APIDetachTagFromResourcesEvent.class
		}
	}
}