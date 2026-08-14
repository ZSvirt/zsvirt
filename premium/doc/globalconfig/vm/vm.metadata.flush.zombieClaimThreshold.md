
## Name

```
vm.metadata.flush.zombieClaimThreshold(元数据僵尸认领超时阈值)
```

### Description

```
Minutes before an uncompleted flush claim is considered zombie and released
```

### 含义

```
脏行被管理节点认领后超过该时间（分钟）仍未完成刷盘，则被判定为僵尸认领并自动释放
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
[0, 120]
```

### 取值范围补充说明

```
该值应大于单次刷盘操作的最长合理耗时，设置过小可能导致正常刷盘任务被误判为僵尸
```

### DefaultValue

```
15
```

### 默认值补充说明

```
默认15分钟，正常刷盘操作通常在秒级完成
```

### 支持的资源级配置



### 资源粒度说明

```
无
```

### 背景信息

```
僵尸认领的典型成因：管理节点在认领脏行后崩溃或刷盘任务线程阻塞。cleanupZombieClaims以60秒间隔周期性运行，读取此配置值判断超时
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
该参数实时生效，修改后下一次cleanupZombieClaims执行时立即使用新值
```
