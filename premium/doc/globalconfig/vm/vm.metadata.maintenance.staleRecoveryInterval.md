
## Name

```
vm.metadata.maintenance.staleRecoveryInterval(元数据Stale恢复间隔)
```

### Description

```
Stale VM metadata recovery interval in seconds
```

### 含义

```
Stale恢复任务的执行间隔，单位为秒。查找所有pendingStaleRecovery=true的VM重新排入刷盘队列
```

### Type

```
java.lang.Long
```

### Category

```
vm
```

### 取值范围

```
[0, 86400]
```

### 取值范围补充说明

```
根据环境中Stale VM数量和主存储恢复速度调整
```

### DefaultValue

```
600
```

### 默认值补充说明

```
默认600秒（10分钟）
```

### 支持的资源级配置



### 资源粒度说明

```
无
```

### 背景信息

```
VM元数据刷盘连续失败超过flush.maxRetry次后进入Stale状态，该任务周期性尝试恢复。任务通过Hash Ring分片
```

### UI暴露

```
暴露
```

### CLI手册暴露

```
暴露
```

## 注意事项

```
该任务使用Hash Ring分片。pendingStaleRecovery由实际刷盘成功路径清除，而非该任务自身清除
```
