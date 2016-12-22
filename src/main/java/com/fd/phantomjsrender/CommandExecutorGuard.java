package com.fd.phantomjsrender;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.fd.phantomjsrender.utils.TimeUtils;

/**
 * 命令执行哨兵
 * 用于监视执行命令卡死或者超时
 * @author caoliuyi
 *
 */
public class CommandExecutorGuard {
    private AtomicBoolean cmdExecuting;
    private AtomicLong cmdStartTimestamp;
    
    public CommandExecutorGuard() {
        cmdExecuting = new AtomicBoolean(false);
        cmdStartTimestamp = new AtomicLong(TimeUtils.monotonicNow());
    }
    
    public void cmdStart() {
        cmdStartTimestamp.getAndSet(TimeUtils.monotonicNow());
        cmdExecuting.getAndSet(true);
    }
    
    public void cmdFinished() {
        cmdExecuting.getAndSet(false);
    }
    
    public boolean isExecuting() {
        return cmdExecuting.get();
    }
    
    public long getCmdStartTimestamp() {
        return cmdStartTimestamp.get();
    }
}
