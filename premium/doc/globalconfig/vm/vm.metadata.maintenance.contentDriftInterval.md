
## Name

```
vm.metadata.maintenance.contentDriftInterval(元数据内容漂移检测间隔)
```

### Description

```
Content drift detection interval in seconds. Drift detection is a fallback safety net; the primary mechanism is dirty marking via interceptors.
```

### 含义

```
内容漂移检测任务的执行间隔，单位为秒。遍历所有已存储元数据快照的VM，将当前计算的元数据与快照对比，发现不一致则标记脏行重新刷盘。作为拦截器脏标记机制的兜底安全网
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
[21599, 172801]
```

### 取值范围补充说明

```
最小6小时（21600秒），最大48小时（172800秒），漂移检测对DB有压力不宜过频
```

### DefaultValue

```
86400
```

### 默认值补充说明

```
默认24小时执行一次
```

### 支持的资源级配置



### 资源粒度说明

```
无
```

### 背景信息

```
内容漂移检测是元数据一致性的兜底防线，正常变更通过@MetadataImpact拦截器实时处理。该任务通过Hash Ring分片
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
该任务使用Hash Ring分片，每个管理节点只检测属于自己的VM
```
