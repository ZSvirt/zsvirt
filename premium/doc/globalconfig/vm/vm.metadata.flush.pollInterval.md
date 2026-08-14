
## Name

```
vm.metadata.flush.pollInterval(元数据脏行轮询间隔)
```

### Description

```
Metadata dirty poller interval in seconds
```

### 含义

```
脏行轮询器（DirtyPoller）的轮询间隔，单位为秒。Poller每隔该时间从脏行表中认领未被占有的脏行并提交刷盘任务
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
[0, 300]
```

### 取值范围补充说明

```
值越小脏行处理延迟越低但DB轮询频率越高，值越大DB压力更小但脏行堆积时间更长
```

### DefaultValue

```
5
```

### 默认值补充说明

```
默认5秒，大多数场景下可在合理延迟内收敛脏行
```

### 支持的资源级配置



### 资源粒度说明

```
无
```

### 背景信息

```
Poller是脏行批量处理的主路径。除Poller外，API拦截器也会通过triggerFlushForVm立即触发单VM刷盘，因此Poller间隔不影响实时API的元数据更新延迟
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
