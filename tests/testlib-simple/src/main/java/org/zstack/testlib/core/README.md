ThreadFacadeForTest 是在 test 域里面工作的

> ZSV-13383 引入

问题:
1. 控制定时任务的触发时间，而不是 sleep 等它触发
2. 配置某些特定名称的定时任务，不要按生产代码写的时间间隔执行，因为无法控制触发的时间————
   有可能在 env.clean 的时候触发了，导致数据库异步变动，最终导致 case 清理失败。

引入 ThreadFacadeForTest 用于主动控制 ThreadFacade#submitPeriodicTask(PeriodicTask, long) 中任务的触发时机。

## 需求

Ha 的 Scheduler，它会每隔一段时间扫描停止的虚拟机。

* 我在测试他们停止的行为，他们居然自动被 HaScheduler 拉起来了，出现了不稳定 case!
* 不稳定 case 跑 100 遍，开头几遍没问题，但是 case 在 env.delete 的时候突然触发了，VM 启动了! 
  它带着 TPM 居然删除失败, 导致清理失败，出现了不稳定 case!

> 虽然这些不稳定 case 是需要你去修的，但是这不是让正常 case 莫名其妙不稳定的原因。
> 你应该写一些并发 case 来测试并发能力，而不是养一堆根本没办法看原因的不稳定 case 来撞运气。

## 怎么写

重要文件 ``test/src/test/resources/UTCapabilities/thread-facade-configs/blocked-periodic-task.list``
这个文件里面写的就是，在测试时不让触发的 PeriodicTask。

如果你有 PeriodicTask 需要让它在测试域手动触发，把 PeriodicTask.name 写到这个文件里面。
它支持 ``*`` 这个通配符。

比如你需要手动触发这个 "test-p-task"：

```java
threadFacade.submitPeriodicTask(new PeriodicTask() {
    @Override
    @ExceptionSafe
    public void run() {
        handleSomething();
    }

    @Override
    public TimeUnit getTimeUnit() {
        return TimeUnit.SECONDS;
    }

    @Override
    public long getInterval() {
        return 2000L;
    }

    @Override
    public String getName() {
        return "test-p-task";
    }
});
```

那你就在 ``blocked-periodic-task.list`` 加一行，写入 "test-p-task"

然后你在 case 里面这么写

```groovy
ErrorCode errorCode = threadFacadeForTest().triggerBlockedPeriodicTask("test-p-task")
assert errorCode == null
```

或者

```groovy
threadFacadeForTest().triggerBlockedPeriodicTaskNoThrow("test-p-task")
```
