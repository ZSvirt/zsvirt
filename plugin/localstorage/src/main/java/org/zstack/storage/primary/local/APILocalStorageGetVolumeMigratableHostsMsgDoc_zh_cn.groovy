package org.zstack.storage.primary.local

import org.zstack.storage.primary.local.APILocalStorageGetVolumeMigratableReply

doc {
	title "LocalStorageGetVolumeMigratableHosts"

	category "storage.primary"

	desc """在这里填写API描述"""

	rest {
		request {
			url "GET /v1/volumes/{volumeUuid}/migration-target-hosts"

			header (Authorization: 'OAuth the-session-uuid')

			clz APILocalStorageGetVolumeMigratableHostsMsg.class

			desc """"""

			params {

				column {
					name "volumeUuid"
					enclosedIn ""
					desc "云盘UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APILocalStorageGetVolumeMigratableReply.class
		}
	}
}