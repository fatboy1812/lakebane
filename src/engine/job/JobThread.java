package engine.job;

import engine.server.world.WorldServer;
import org.pmw.tinylog.Logger;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class JobThread implements Runnable {
    private final AbstractJob currentJob;
    private final ReentrantLock lock = new ReentrantLock();

    private static Long nextThreadPrint;

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

       if(JobThread.nextThreadPrint == null){
           JobThread.nextThreadPrint = System.currentTimeMillis();
       }else{
           if(JobThread.nextThreadPrint < System.currentTimeMillis()){
               JobThread.tryPrintThreads();
               JobThread.nextThreadPrint = System.currentTimeMillis() + 10000L;
           }
       }
    }

    public static void tryPrintThreads(){
        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        while (rootGroup.getParent() != null) {
            rootGroup = rootGroup.getParent();
        }

        // Estimate the number of threads
        int activeThreads = rootGroup.activeCount();

        // Create an array to hold the threads
        Thread[] threads = new Thread[activeThreads];

        // Get the active threads
        rootGroup.enumerate(threads, true);

        int availableThreads = Runtime.getRuntime().availableProcessors();

        // Print the count
        if(threads.length > 30)
            Logger.info("Total threads in application: " + threads.length + " / " + availableThreads);
    }
}
