package org.zstack.header.storage.primary

doc {
	title "添加主存储(AddPrimaryStorage)"

	category "storage.primary"

	desc "添加主存储"

	rest {
		request {
			url "POST /v1/primary-storage"


			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddPrimaryStorageMsg.class

			desc ""
			
			params {

				column {
					name "url"
					enclosedIn "params"
					desc "主存储路径"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
					
				}
				column {
					name "name"
					enclosedIn "params"
					desc "主存储名称"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
					
				}
				column {
					name "description"
					enclosedIn "params"
					desc "主存储的详细描述"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
					
				}
				column {
					name "type"
					enclosedIn "params"
					desc "主存储类型"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
					
				}
				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
					
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID。若指定，主存储会使用该字段值作为UUID。"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
					
				}
				column {
					name "systemTags"
					enclosedIn "params"
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
					
				}
				column {
					name "userTags"
					enclosedIn "params"
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
					
				}
			}
		}

		response {
			clz APIAddPrimaryStorageEvent.class
		}
	}
}