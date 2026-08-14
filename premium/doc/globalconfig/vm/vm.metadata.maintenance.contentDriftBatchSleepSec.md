
## Name

```
vm.metadata.maintenance.contentDriftBatchSleepSec(元数据内容漂移检测批次休眠时间)
```

### Description

```
Sleep time in seconds between content drift detection batches to limit DB pressure
```

### 含义

```
内容漂移检测任务在批次之间的休眠时间，单位为秒，用于限制DB压力
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
[0, 31]
```

### 取值范围补充说明

```
无
```

### DefaultValue

```
1
```

### 默认值补充说明

```
默认1秒
```

### 支持的资源级配置



### 资源粒度说明

```
无
```

### 背景信息

```
配合contentDriftBatchSize使用，控制漂移检测的DB负载
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
