package com.ncslzh.projects.placeholders;


public interface DistributedLock {
    void runWithLock(String lockKey, int lockSeconds, Runnable toRun);
}
