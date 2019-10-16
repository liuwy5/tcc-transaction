package org.mengyun.tcctransaction.spring.recover;

import org.mengyun.tcctransaction.OptimisticLockException;
import org.mengyun.tcctransaction.recover.RecoverConfig;

import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by changming.xie on 6/1/16.
 */
public class DefaultRecoverConfig implements RecoverConfig {

    public static final RecoverConfig INSTANCE = new DefaultRecoverConfig();

    private int maxRetryCount = 30;

    /**
     * 恢复间隔时间，单位：秒
     */
    private int recoverDuration = 120; //120 seconds

    private String cronExpression = "0 */1 * * * ?";

    private int asyncTerminateThreadPoolSize = 1024;

    /**
     * 延迟取消异常集合
     */
    private Set<Class<? extends Exception>> delayCancelExceptions = new HashSet<Class<? extends Exception>>();

    /**
     * 针对SocketTimeoutException:
     *  try阶段，本地参与者调用远程参与者，远程参与者try阶段的方法逻辑执行时间过长，超过Socket等待时间，产生SocketTimeoutException
     *  如果立刻执行事务回滚，远程参与者try的方法未执行完成，可能导致cancel的方法实际未执行
     *      (try的方法未执行完成，数据库事务未提交【非TCC事务】，cancel方法读取数据时发现未变更，导致方法实际未执行，最终try方法执行完成后，提交数据库事务【非TCC事务】，较为极端)
     *  最终引起数据不一致
     *  在事务恢复时，会对这种情况的事务进行取消回滚，如果此时远程参与者的try方法还未结束，可能发生数据不一致
     * 针对OptimisticLockException：
     *  事务恢复间隔小于Socket超时时间，此时事务恢复调用远程参与者取消回滚事务，远程参与者下次更新事务时，会因为乐观锁更新失败，抛出OptimisticLockException
     *  如果CompensableTransactionInterceptor此时立刻取消回滚，可能会和定时任务的取消回滚冲突，因此统一交给定时任务处理
     */
    public DefaultRecoverConfig() {
        delayCancelExceptions.add(OptimisticLockException.class);
        delayCancelExceptions.add(SocketTimeoutException.class);
    }

    @Override
    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    @Override
    public int getRecoverDuration() {
        return recoverDuration;
    }

    @Override
    public String getCronExpression() {
        return cronExpression;
    }


    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public void setRecoverDuration(int recoverDuration) {
        this.recoverDuration = recoverDuration;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    @Override
    public void setDelayCancelExceptions(Set<Class<? extends Exception>> delayCancelExceptions) {
        this.delayCancelExceptions.addAll(delayCancelExceptions);
    }

    @Override
    public Set<Class<? extends Exception>> getDelayCancelExceptions() {
        return this.delayCancelExceptions;
    }

    public int getAsyncTerminateThreadPoolSize() {
        return asyncTerminateThreadPoolSize;
    }

    public void setAsyncTerminateThreadPoolSize(int asyncTerminateThreadPoolSize) {
        this.asyncTerminateThreadPoolSize = asyncTerminateThreadPoolSize;
    }
}
