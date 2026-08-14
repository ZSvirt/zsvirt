
## Name

```
vm.metadata.maintenance.staleRecoveryBatchSize(元数据Stale恢复批次大小)
```

### Description

```
Max stale VMs processed per recovery cycle
```

### 含义

```
每轮Stale恢复任务最多处理的VM数量
```

### Type

```
java.lang.Integer
```

### Category

```
vm
```

### 取值范围

```
[0, 20]
```

### 取值范围补充说明

```
批次过大时可能产生大量并发刷盘任务
```

### DefaultValue

```
10
```

### 默认值补充说明

```
默认每轮处理10个Stale VM
```

### 支持的资源级配置



### 资源粒度说明

```
无
```

### 背景信息

```
分批处理避免一次性产生过多刷盘任务
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
无
```
