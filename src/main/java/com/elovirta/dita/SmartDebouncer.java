package com.elovirta.dita;

import java.util.Map;
import java.util.concurrent.*;

public class SmartDebouncer {
  private final ScheduledExecutorService scheduler;
  private final Map<String, ScheduledFuture<?>> pendingTasks;

  public SmartDebouncer() {
    this.scheduler = Executors.newScheduledThreadPool(1);
    this.pendingTasks = new ConcurrentHashMap<>();
  }

  public void debounce(String key, Runnable task, long delayMs) {
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
                System.err.println("Error executing debounced task for key: " + key);
                e.printStackTrace();
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
          System.err.println("Scheduler did not terminate");
        }
      }
    } catch (InterruptedException e) {
      // Re-interrupt and force shutdown
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}
