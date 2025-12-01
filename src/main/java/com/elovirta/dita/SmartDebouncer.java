package com.elovirta.dita;

import java.util.Map;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmartDebouncer {

  private static final Logger logger = LoggerFactory.getLogger(SmartDebouncer.class);

  private final ScheduledExecutorService scheduler;
  private final Map<String, ScheduledFuture<?>> pendingTasks;
  private final int delayMs;

  public SmartDebouncer(int delayMs) {
    this.delayMs = delayMs;
    this.scheduler = Executors.newScheduledThreadPool(1);
    this.pendingTasks = new ConcurrentHashMap<>();
  }

  public void debounce(String key, Runnable task) {
    ScheduledFuture<?> existing = pendingTasks.get(key);
    if (existing != null && !existing.isDone()) {
      existing.cancel(false);
    }

    ScheduledFuture<?> future =
        scheduler.schedule(
            () -> {
              try {
                pendingTasks.remove(key);
                task.run();
              } catch (Exception e) {
                logger.error("Error executing debounced task for key: {}", key, e);
              }
            },
            delayMs,
            TimeUnit.MILLISECONDS);

    pendingTasks.put(key, future);
  }

  public void shutdown() {
    // Cancel all pending tasks
    pendingTasks.values().forEach(f -> f.cancel(false));
    pendingTasks.clear();

    // Shutdown the scheduler
    scheduler.shutdown();

    try {
      // Wait for tasks to complete
      if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();

        // Wait again after forceful shutdown
        if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
          logger.error("Scheduler did not terminate");
        }
      }
    } catch (InterruptedException e) {
      // Re-interrupt and force shutdown
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}
