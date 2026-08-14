package org.zstack.header.volume

doc {
	title "CreateVolumeSnapshotScheduler"

	category "snapshot.volume"

	desc """创建快照的定时任务"""

	rest {
		request {
			url "POST /v1/volumes/{volumeUuid}/schedulers/creating-volume-snapshots"

			header (Authorization: 'OAuth the-session-uuid')


			clz APICreateVolumeSnapshotSchedulerJobMsg.class

			desc """"""
			
			params {

				column {
					name "volumeUuid"
					enclosedIn "params"
					desc "云盘UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
					
				}
				column {
					name "snapShotName"
					enclosedIn "params"
					desc "定时任务名称"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
					
				}
				column {
					name "volumeSnapshotDescription"
					enclosedIn "params"
					desc "对定时创建的快照描述"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
					
				}
				column {
					name "schedulerName"
					enclosedIn "params"
					desc "定时任务名称"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
					
				}
				column {
					name "schedulerDescription"
					enclosedIn "params"
					desc "定时任务描述"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
					
				}
				column {
					name "type"
					enclosedIn "params"
					desc "定时任务类型，支持'simple'和'cron'两种类型"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
					values ("simple","cron")
				}
				column {
					name "interval"
					enclosedIn "params"
					desc "定时任务间隔，单位秒"
					location "body"
					type "Integer"
					optional true
					since "zsv 0.6"
					
				}
				column {
					name "repeatCount"
					enclosedIn "params"
					desc "定时任务重复次数，仅针对simple类型的定时任务生效"
					location "body"
					type "Integer"
					optional true
					since "zsv 0.6"
					
				}
				column {
					name "startTime"
					enclosedIn "params"
					desc "定时任务启动时间，必须遵循unix timestamp格式，0为从立刻开始"
					location "body"
					type "Long"
					optional true
					since "zsv 0.6"
					
				}
				column {
					name "cron"
					enclosedIn "params"
					desc "cron表达式，需遵循Java Quartz组件cron格式标准"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
					
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "用户可指定创建Scheduler所使用的uuid"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
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
			clz APICreateVolumeSnapshotSchedulerJobEvent.class
		}
	}
}