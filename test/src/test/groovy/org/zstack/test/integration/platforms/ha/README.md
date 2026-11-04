请按照 HA 的测试阶段，把 case 放到对应子目录。套件是同目录的 `HaTest`，spring 用 `ZStackTest.springSpec`。

当前只有 `config/HaNeverStopCase`：Stopped VM 设置 NeverStop 后，`inhibitionReason` 会挡住拉起；清掉 inhibition 再触发扫描才会复活。`dead`、`reboot`、`full` 还没有用例。

单测里下面两个周期任务在 `UTCapabilities/thread-facade-configs/blocked-periodic-task.list` 中，不会按生产间隔执行。需要时在当前线程触发：

```groovy
threadFacadeForTest().triggerBlockedPeriodicTask(HaScheduler.TASK_NAME_STOPPED_VM_SCANNER)
threadFacadeForTest().triggerBlockedPeriodicTask(HaScheduler.TASK_NAME_REFRESH_FENCERS)
```

`TASK_NAME_STOPPED_VM_SCANNER` 的 `run()` 会同步执行 `HaScheduler.scanHaVmNow()`。`TASK_NAME_REFRESH_FENCERS` 的 `run()` 只提交 chain，触发返回时同步流程可能还没结束。

# HA 总览（判死 vs 复活）

```
VM HA
├── [配置] 参数设置
├── [配置] Self-Fencer 部署（宿主侧监控，为判死做准备）
├── [触发] VM 进入 Unknown / NeverStop 进入 Stopped / 周期扫描 / GC 重试
├── [阶段A · 判死] Fencer / HaHostChecker 确认“可杀 / 已死 / 宿主不可达”
└── [阶段B · 复活] HaStartVmInstanceMsg → Judger → VmSchedHistoryVO → startVm
```

| 阶段 | 包 |
|------|----|
| 参数设置、Self-Fencer 部署与状态同步 | `org.zstack.test.integration.platforms.ha.config` |
| 阶段A · 判死 | `org.zstack.test.integration.platforms.ha.dead` |
| 阶段B · 复活 | `org.zstack.test.integration.platforms.ha.reboot` |
| 综合 | `org.zstack.test.integration.platforms.ha.full` |


## 配置：Self-Fencer 部署（判死能力上线）

```
Host Connected / PS Attach / Strategy 变更 / MN Ready
└── HaExtensions.afterHostConnected / setupSelfFencerOnAllHosts
    ├── HaHelper.findExecuteFencers()  ← 读 HaStrategyConditionVO(Enable)
    ├── SelfFencerStruct(fencers, host, PS)
    └── SetupStorageHeartbeatFileFencer
        └── SelfFencerKvmBackend.setup
            ├── HostHaStateVO（每个 host + primaryStorage 一行）
            │   └── None → Setuping → Done | SetupFail
            │       Cancel：Cancelling → None | CancelFail
            ├── /ha/selffencer/setup → KVM Agent
            │   ├── hostStorageState（存储心跳）
            │   ├── hostBusinessNic（业务网）
            │   ├── cephFencer / fileSystemFencer / shareblockFencer …
            └── 按 PS 调 KvmSetupSelfFencerExtensionPoint
```

`HostHaState` 不是只在 setup 时写一次。MN Ready 后 `HaScheduler.startSyncSelfFencer()` 按 `ha.selfFencer.sync.interval`（默认 60 秒）跑 `HaSyncSelfFencerTask`：

```
HaSyncSelfFencerTask
├── 向已有 HostHaStateVO 的 host 发 GetFencerStateFromHostMsg（/ha/selffencer/state）
│   ├── 宿主上报存在 → syncExist（None/SetupFail → Done；Cancelling/CancelFail + 仍存在 → CancelFail）
│   └── 宿主上报不存在 → syncNotExist（Done → SetupFail；Cancelling/CancelFail → None）
├── SetupFail → 先改成 Setuping，再发 RetrySetupSelfFencerOnHostMsg
│   └── 仍按当前挂载的主存储过滤；已卸载的主存储不会被重试 setup
└── CancelFail → 先改成 Cancelling，再发 RetryCancelSelfFencerOnHostMsg
    └── 按消息里的主存储 UUID 加载，不看集群挂载
        ├── VO 还在 → 对该主存储发 cancel
        └── VO 已删、HostHaStateVO 还在 → cancelDone（Cancelling → None）
```

策略按主存储读取：`HaStrategyHelper.getPrimaryStorageSelfFencerStrategy`。支持存储一致性的类型（如 Shared Block）固定为 Force。文件系统主存储读该主存储的 `ha.self.fencer.strategy`，没有单独配置时用全局 Force。`Permissive` 才会跳过杀 VM / 卸载，以及管理侧复活。

复活前会看根盘所在主存储在该 host 上的 `HostHaState`。没有行、没有 host 或没有根盘主存储时，`HaHelper.isSelfFencerDone` 视为已完成。已有行且不是 `Done` 时直接跳过，不发 `HaStartVmInstanceMsg`：

```
HaKvmWorker.startHa
VmHaService.haStartVmWithTracker
HaGcManager.submitNeverStopVmGCJob
```

## 阶段 A · 判死（Fencer / Checker）

判死有两条路径，**可并行存在**。

### 宿主 Self-Fencer 判死（物理 fence）

```
KVM Agent 检测到存储/网卡异常
├── （Force 策略）杀掉本机 VM / 断存储访问
├── HTTP POST /ha/events/vm-fenced   ← ReportVmSelfFencerCmd
│   └── HaManager
│       └── VmHaVOUpdater.VmHAFenceParameters#record
│           └── VmHaVO: 修改 lastFencerName/lastFenceHostUuid/lastFenceTime
│               ├── HostBusinessNicKvmFencer → "host business NIC is down"
│               ├── CephKvmFencer           → "ceph storage is unavailable"
│               └── FileSystemKvmFencer     …
└── （可选）KVM_REPORT_SELF_FENCER → 发 CanonicalEvent（PS/Host 状态）
```

说明：当前 `vm-fenced` 处理里有 `TODO: start HA`，**不会直接复活**；一般靠后续 VM→`Unknown` 或 NeverStop 扫描进入复活。`SelfFencerStrategy.Permissive` 时管理侧会跳过复活。

### 管理侧 HaHostChecker 判死（VM 已是 Unknown）

入口：`haOnHostFailure` → `HaKvmWorker.start`（仅当 `state==Unknown`）。

```
HaKvmWorker.start(vm=Unknown)
└── checkHost(所有 HaHostChecker 并行加权)
    ├── HaKvmHostSiblingChecker     # 兄弟 Host 代扫 /ha/scanhost
    ├── NFS / SMP / SharedBlock / Ceph / iSCSI / CBD / Block …
    │   └── HaHostStorageBasedChecker：还可带回“仍在存储上活跃的 vmUuids”
    ├── HaManagementNodeChecker     # 代码存在（arping/nmap），但 ha.xml 未装配，当前默认不参与
    └── HostCheckResult.getCheckResult
        ├── ratio = weight/weightTotal < CHECK_SUCCESS_RATIO  → Host Dead
        ├── 存储确认过、但 vmUuid 不在活跃列表               → VM not active on storage
        └── 否则                                             → Alive（noNeed HA）
```

Agent → MN 回调对照：

| 路径 / 常量 | 效果 |
|-------------|------|
| `/ha/events/vm-fenced` | `createFencedByTag`；源码有 `TODO: start HA`，不直接复活 |
| `KVM_REPORT_SELF_FENCER` | 发 `VM_HA_HOST_SELF_FENCER_TRIGGERED_PATH`（通知） |
| `KVM_REPORT_PS_STATUS` | 更新 PS-Host 连接状态；Agent 侧可能已杀 VM，HA 跟 Unknown/Stopped |
判死后进入复活前闸门：

```
tryHaWith / startHa
├── onlyCheckAlive（haLevel=None）→ noNeed
├── Strategy=Permissive           → skip
├── HostHaState 不是 Done         → skip（见上面的 isSelfFencerDone）
├── HaHostDeviceExtensionPoint    → 外设仍挂载则 skip
├── FaultTolerance                → 特殊处理（部分路径直接 return）
└── 通过 → 进入阶段 B（Judger=UnknownVmHaJudger）
```

---

## HA 触发入口 → 如何接到判死/复活

### Host 失联 → Unknown（compute 前置，不在 mevoco）

```
Host disconnect
└── HostBase.changeConnectionState(Disconnected)
    └── AfterChangeHostStatusExtensionPoint
        └── VmInstanceManagerImpl.afterChangeHostStatus
            └── VmStateChangedOnHostMsg(stateOnHost=Unknown)  // 该 Host 上各 VM
                └── （兜底）UnknownVmGC
                    └── VmInstanceBase → state=Unknown
                        └── fire VM_FULL_STATE_CHANGED_PATH
```

### 触发源总表

| 触发 | 路径 |
|------|------|
| Host 失联 | 上表 → `startHaIfNeeded` → `haOnHostFailure` |
| Agent Self-Fencer | fence + `/ha/events/vm-fenced` 打 tag；HA 仍多靠后续 Unknown/Stopped |
| NeverStop 异常 Stopped | `haOnStopVm` → 状态复核 → `doStartVm` |
| Guest ACPI 关机 | `kvmReportVmShutdownEvent` → **inhibit HA**（防误拉起） |
| API/Hypervisor Stop | `inhibitHATemporarily`（意图停机不触发 HA） |
| NeverStop 周期扫描 / GC | `HaScheduler.TASK_NAME_STOPPED_VM_SCANNER`（`NeverStopVmScanTracker`）/ `NeverStopVmGC` |
| Self-Fencer 状态同步与失败重试 | `HaScheduler.TASK_NAME_REFRESH_FENCERS` |
| 迁移失败（NeverStop） | `failedToMigrateVm` → `handleMigrationFailure` |
| VR Unknown | `afterAcquireVirtualRouter` 可能 HA 拉起 |
| 全局开关 | `HaGlobalConfig.ALL`；另受 `PAUSE_THE_WORLD` 影响 |

```
VmStateHaListener
└── 监听 VM_FULL_STATE_CHANGED_PATH (VmStateChangedData)
    ├── 前置: HA 全局开 + haLevel 启用 + 无 inhibitionReason
    ├── OnHostFailure + Unknown  → haOnHostFailure(retry=F)
    ├── NeverStop + Unknown      → haOnHostFailure(retry=T)
    ├── NeverStop + Stopped      → haOnStopVm
    └── FaultTolerance + Unknown → haOnHostFailure

haOnHostFailure
└── HaHypervisorFactory → HaKvmWorker
    └── [阶段A] Checker 判死 → [阶段B] VmHaStartMessageSender

haOnStopVm / startVmWithStateSyncBefore / doStartVm / NeverStopVmGC
└── （Stopped 路径常跳过 Checker）直接 [阶段B]
```

### 判死后的旁路清理

`HaKvmWorker.onScanResult`：当 Checker 判定 Host **Dead** 时，对该 Host 上仍为 Unknown、且 **HA 已关闭**、且策略为 **Force** 的 VM，发 `ChangeVmStateMsg(Stopped)`，避免非 HA VM 长期卡在 Unknown。
---

## 阶段 B · 复活（HaStartVmInstanceMsg + VmSchedHistoryVO）

```
VmHaStartMessageSender.send
└── HaStartVmInstanceMsg
    ├── softAvoidHostUuids = 原 Host（尽量避开）
    ├── haReason           = Fencer 文案 / Checker evidence
    └── judgerClassName    =
        ├── UnknownVmHaJudger        # 仅 Unknown（Checker 路径）
        ├── GeneralVmStateHaJudger   # Unknown|Stopped（Stopped/同步路径）
        └── GCVmHaNeverStopJudger    # NeverStop 且 Unknown|Stopped（GC）

        ↓ bus → VmInstanceConstant.SERVICE_ID

VmInstanceBase.handle(HaStartVmInstanceMsg)          ← 已在 compute
├── new Judger().whetherStartVm() 不通过 → 直接 reply（不记历史）
├── VmSchedHistoryRecorder.ofHA.begin()
│   └── INSERT VmSchedHistoryVO
│       schedType=VMHA, lastHostUuid, schedReason=haReason
├── BeforeHaStartVmInstanceExtensionPoint
├── DB: state=Stopped, hostUuid=null, lastHostUuid=原 Host
├── startVm(...)（正常启动流 + softAvoid）
└── recorder.end(destHostUuid) / withFailReason.end(null)
    └── UPDATE success, destHostUuid, failReason

成功后：
├── fire VmHaCanonicalEvents.VM_HA_STARTED_PATH
└── KvmFencerManager.afterStartVm → 删除 vmFencedBy tag
```

---

## 两条典型完整链路（tree）

### Host 挂掉 + OnHostFailure / NeverStop（Unknown 路径）

```
Host 失联
└── compute: UnknownVmGC / VmStateChangedOnHost
    └── VM.state = Unknown
        └── Event: VM_FULL_STATE_CHANGED
            └── VmStateHaListener (Event 接收)
                └── haOnHostFailure
                    └── HaKvmWorker.start                【判死】
                        ├── HaHostChecker × N（加权）
                        ├── Host Dead 或 VM not on storage
                        ├── 读 vmFencedBy（若 Agent 已 fence）
                        └── VmHaStartMessageSender       【复活】
                            └── HaStartVmInstanceMsg
                                └── VmInstanceBase + VmSchedHistoryVO(VMHA)
                                    └── startVm on other host
                                        └──（NeverStop 失败）NeverStopVmGC 重试
```

### NeverStop 异常 Stopped（常不经 Checker）

```
VM.state → Stopped（非用户意图）
└── startHaIfNeeded → haOnStopVm
    └── startVmWithStateSyncBefore
        └── CheckVmStateOnHypervisor
            ├── 仍 Running/Paused → 只纠正 DB，不 HA
            └── Stopped / 查不到
                └── doStartVm → HaStart(GeneralVmStateHaJudger)
                    └── VmSchedHistoryVO + startVm
```

### Agent 先 fence（判死在前，复活滞后）

```
Agent Self-Fencer 判死并杀 VM
└── /ha/events/vm-fenced → vmFencedBy tag          【判死·标记】
    └── （之后）VM 被标 Unknown / NeverStop 扫描
        └── 再走 Unknown 路径或 NeverStop 周期扫描 → HaStart，reason 用 Fencer 文案
```
