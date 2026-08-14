
## Name

```
bootMenuSplashTimeout(启动菜单显示超时)
```

### Description

```
splash timeout of boot menu in milliseconds
```

### 含义

```
云主机操作系统启动菜单的超时时间，单位为毫秒(ms)。
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
[0, 65535]
```

### 取值范围补充说明

```
无
```

### DefaultValue

```
3000
```

### 默认值补充说明

```
无
```

### 支持的资源级配置

||
|---|
|org.zstack.header.vm.VmInstanceVO

### 资源粒度说明

```
无
```

### 背景信息

```
因为通过CD/DVD启动时，默认等待时间为3s，需要用户手动跳回启动菜单选择对应的启动项目，并且通过CD/DVD首选项启动的时候，部分云主机操作系统需要按任意键才能进入CD/DVD安装，其他启动方式可能也有类似的问题，因此提供一个配置项目，延长自动进入CD/DVD安装的时间，保证通过VNC安装的用户能够成功的选择自己需要使用的启动项
```

### UI暴露

```
不暴露
```

### CLI手册暴露

```
不暴露
```

## 注意事项

```
无
```
