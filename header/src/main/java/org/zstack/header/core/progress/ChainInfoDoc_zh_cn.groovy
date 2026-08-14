package org.zstack.header.core.progress

import org.zstack.header.core.progress.RunningTaskInfo
import org.zstack.header.core.progress.PendingTaskInfo

doc {

	title "任务队列信息"

	ref {
		name "runningTask"
		path "org.zstack.header.core.progress.ChainInfo.runningTask"
		desc "正在运行的任务"
		type "List"
		since "zsv 3.6.0"
		clz RunningTaskInfo.class
	}
	ref {
		name "pendingTask"
		path "org.zstack.header.core.progress.ChainInfo.pendingTask"
		desc "等待运行的任务"
		type "List"
		since "zsv 3.6.0"
		clz PendingTaskInfo.class
	}
}
