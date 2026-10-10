请按照 HA 的测试阶段，自觉把 case 分到对应的子目录里面。

# HA 总览（判死 vs 复活）

```
VM HA
├── [配置] 参数设置
├── [配置] Self-Fencer 部署（宿主侧监控，为判死做准备）
├── [触发] VM 进入 Unknown / NeverStop 进入 Stopped / 周期扫描 / GC 重试
├── [阶段A · 判死] Fencer / HaHostChecker 确认“可杀 / 已死 / 宿主不可达”
└── [阶段B · 复活] HaStartVmInstanceMsg → Judger → VmSchedHistoryVO → startVm
```

| 阶段             | 子目录                    |
|----------------|------------------------|
| 参数设置           | `.platforms.ha.config` |
| Self-Fencer 部署 | `.platforms.ha.config` |
| 阶段A · 判死       | `.platforms.ha.dead`   |
| 阶段B · 复活       | `.platforms.ha.reboot` |
| 综合             | `.platforms.ha.full`   |


## 配置：Self-Fencer 部署（判死能力上线）

```
Host Connected / PS Attach / Strategy 变更 / MN Ready
└── HaExtensions.afterHostConnected / setupSelfFencerOnAllHosts
    ├── HaHelper.findExecuteFencers()  ← 读 HaStrategyConditionVO(Enable)
    ├── SelfFencerStruct(fencers, host, PS)
    └── SetupStorageHeartbeatFileFencer
        └── SelfFencerKvmBackend.setup
            ├── HostHaStateVO → Setuping → Done|Fail
            ├── /ha/selffencer/setup → KVM Agent
            │   ├── hostStorageState（存储心跳）
            │   ├── hostBusinessNic（业务网）
            │   ├── cephFencer / fileSystemFencer / shareblockFencer …
            └── 按 PS 调 KvmSetupSelfFencerExtensionPoint
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
| NeverStop 周期扫描 / GC | `NeverStopVmScanTracker` / `NeverStopVmGC` |
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
        └── 再走 7.1 / 周期扫描 → HaStart，reason 用 Fencer 文案
```
