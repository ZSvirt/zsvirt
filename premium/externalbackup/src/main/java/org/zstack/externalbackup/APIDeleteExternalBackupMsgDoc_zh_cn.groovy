package org.zstack.externalbackup

import org.zstack.externalbackup.APIDeleteExternalBackupEvent

doc {
	title "DeleteExternalBackup"

	category "externalbackup"

	desc """删除外部备份"""

	rest {
		request {
			url "DELETE /v1/externalbackup/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteExternalBackupMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.9.0"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
					since "3.9.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.9.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.9.0"
				}
			}
		}

		response {
			clz APIDeleteExternalBackupEvent.class
		}
	}
}