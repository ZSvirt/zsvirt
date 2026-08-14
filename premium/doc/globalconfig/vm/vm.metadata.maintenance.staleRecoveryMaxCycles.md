
## Name

```
vm.metadata.maintenance.staleRecoveryMaxCycles(元数据Stale恢复最大轮次)
```

### Description

```
Max stale recovery cycles before entering permanent-stale state
```

### 含义

```
Stale恢复的最大尝试轮次。达到上限时系统将pendingStaleRecovery和staleRecoveryCount均重置为0，进入permanent-stale状态不再自动重试
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
[0, 1000]
```

### 取值范围补充说明

```
实际最大恢复时长 ≈ 该值 × staleRecoveryInterval
```

### DefaultValue

```
10
```

### 默认值补充说明

```
默认10轮，超过后需管理员手动处理
```

### 支持的资源级配置



### 资源粒度说明

```
无
```

### 背景信息

```
熔断机制，防止系统对同一VM无限重试Stale恢复
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
进入permanent-stale后仅可通过APIUpdateVmMetadataMsg手动触发恢复
```
