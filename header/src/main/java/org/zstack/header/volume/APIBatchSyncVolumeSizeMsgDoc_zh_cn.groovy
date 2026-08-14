package org.zstack.header.volume

import org.zstack.header.volume.APIBatchSyncVolumeSizeReply

doc {
	title "BatchSyncVolumeSize"

	category "volume"

	desc """批量刷新云盘容量"""

	rest {
		request {
			url "POST /v1/volumes/batch-sync-volumes"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIBatchSyncVolumeSizeMsg.class

			desc """"""

			params {

				column {
					name "clusterUuid"
					enclosedIn ""
					desc "集群UUID"
					location "body"
					type "String"
					optional false
					since "zsv 3.16.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.16.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.16.0"
				}
			}
		}

		response {
			clz APIBatchSyncVolumeSizeReply.class
		}
	}
}