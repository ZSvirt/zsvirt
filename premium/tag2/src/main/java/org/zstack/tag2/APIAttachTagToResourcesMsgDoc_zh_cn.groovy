package org.zstack.tag2

import org.zstack.tag2.APIAttachTagToResourcesEvent

doc {
	title "AttachTagToResources"

	category "tag2"

	desc """加载标签到资源上"""

	rest {
		request {
			url "POST /v1/tags/{tagUuid}/resources"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAttachTagToResourcesMsg.class

			desc """"""

			params {

				column {
					name "tagUuid"
					enclosedIn "params"
					desc "标签UUID"
					location "url"
					type "String"
					optional false
					since "3.2.0"
				}
				column {
					name "resourceUuids"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "List"
					optional false
					since "3.2.0"
				}
				column {
					name "tokens"
					enclosedIn "params"
					desc "通过标签存放的键和值"
					location "body"
					type "Map"
					optional true
					since "3.2.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.2.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.2.0"
				}
			}
		}

		response {
			clz APIAttachTagToResourcesEvent.class
		}
	}
}