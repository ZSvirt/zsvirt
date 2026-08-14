package org.zstack.header.volume



doc {
	title "在这里填写API标题"

	desc "在这里填写API描述"

	rest {
		request {
			url "PUT /v1/volumes/{uuid}/actions"


			header (Authorization: 'OAuth the-session-uuid')

			clz APIBackupDataVolumeMsg.class

			desc ""
			
			params {

				column {
					name "uuid"
					enclosedIn "backupDataVolume"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
					
				}
				column {
					name "backupStorageUuid"
					enclosedIn "backupDataVolume"
					desc "镜像存储UUID"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
					
				}
			}
		}

		response {
			clz APIBackupDataVolumeEvent.class
		}
	}
}