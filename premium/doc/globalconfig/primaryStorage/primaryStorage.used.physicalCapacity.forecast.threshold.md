
## Name

```
primaryStorage.used.physicalCapacity.forecast.threshold(主存储预测使用物理容量阈值)
```

### Description

```
The threshold for predicting primary storage's used physical capacity. If the predicted value exceeds this threshold, it indicates that the primary storage is expected to be full in the future.
```

### 含义

```
主存储预测使用物理容量阈值所需时间，当主存储预测使用物理容量超过阈值后，触发报警器
```

### Type

```
java.lang.Double
```

### Category

```
primaryStorage
```

### 取值范围

```
[-9223372036854775808, 9223372036854775807]
```

### 取值范围补充说明

```
无
```

### DefaultValue

```
0.9
```

### 默认值补充说明

```
无
```

### 支持的资源级配置

||
|---|
|org.zstack.header.storage.primary.PrimaryStorageVO

### 资源粒度说明

```
无
```

### 背景信息

```
无
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
