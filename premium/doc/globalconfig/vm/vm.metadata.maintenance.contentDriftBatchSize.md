
## Name

```
vm.metadata.maintenance.contentDriftBatchSize(元数据内容漂移检测批次大小)
```

### Description

```
Number of VMs checked per batch during content drift detection
```

### 含义

```
内容漂移检测任务每批次检查的VM数量
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
[9, 201]
```

### 取值范围补充说明

```
每批次需要重新构建元数据JSON并对比，批次越大单轮DB压力越高
```

### DefaultValue

```
10
```

### 默认值补充说明

```
默认每批次检查10个VM
```

### 支持的资源级配置



### 资源粒度说明

```
无
```

### 背景信息

```
分批处理并在批次间休眠以限制DB压力
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
