package org.zstack.header.storage.backup

doc {
	title "添加镜像服务器(AddBackupStorage)"

	category "storage.backup"

	desc "添加镜像服务器"

	rest {
		request {
			url "POST /v1/backup-storage"


			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddBackupStorageMsg.class

			desc ""
			
			params {

				column {
					name "url"
					enclosedIn "params"
					desc "镜像服务器地址"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
					
				}
				column {
					name "name"
					enclosedIn "params"
					desc "镜像服务器名字"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
					
				}
				column {
					name "description"
					enclosedIn "params"
					desc "镜像服务器的详细描述"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
					
				}
				column {
					name "type"
					enclosedIn "params"
					desc "镜像服务器的类型"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
					
				}
				column {
					name "importImages"
					enclosedIn "params"
					desc "标记是否要导入镜像服务器中的镜像"
					location "body"
					type "boolean"
					optional true
					since "zsv 1.9"
					
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID。若指定，镜像服务器会使用该字段值作为UUID。"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "镜像服务器系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "镜像服务器用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
					
				}
			}
		}

		response {
			clz APIAddBackupStorageEvent.class
		}
	}
}
