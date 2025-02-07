package engine.job;

import org.pmw.tinylog.Logger;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class JobThread implements Runnable {
    private final AbstractJob currentJob;
    private final ReentrantLock lock = new ReentrantLock();

    public JobThread(AbstractJob job){
        this.currentJob = job;
    }

    @Override
    public void run() {
        try {
            if (this.currentJob != null) {
                if (lock.tryLock(5, TimeUnit.SECONDS)) { // Timeout to prevent deadlock
                    try {
                        this.currentJob.doJob();
                    } finally {
                        lock.unlock();
                    }
                } else {
                    Logger.warn("JobThread could not acquire lock in time, skipping job.");
                }
            }
        } catch (Exception e) {
            Logger.error(e);
        }
    }

    public static void startJobThread(AbstractJob job){
        JobThread jobThread = new JobThread(job);
        Thread thread = new Thread(jobThread);
        thread.setName("JOB THREAD: " + job.getWorkerID());
        thread.start();
    }
}
