
## Name

```
vm.metadata.flush.maxRetry(元数据刷盘最大重试次数)
```

### Description

```
Max flush retry count before marking VM metadata as stale
```

### 含义

```
单个云主机元数据刷盘失败后的最大重试次数。达到上限后该VM被标记为pendingStaleRecovery，不再进入普通重试流程
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
[0, 10]
```

### 取值范围补充说明

```
重试采用指数退避策略
```

### DefaultValue

```
5
```

### 默认值补充说明

```
默认5次重试，覆盖大多数瞬时故障场景。超过5次仍失败表明存在持续性问题，转入Stale恢复流程
```

### 支持的资源级配置



### 资源粒度说明

```
无
```

### 背景信息

```
刷盘失败的常见原因：主存储不可达、VM已停止无法获取宿主机信息、序列化异常等
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
达到最大重试次数后VM进入pendingStaleRecovery状态，由MaintenanceManager周期性处理或通过APIUpdateVmMetadataMsg手动触发
```
