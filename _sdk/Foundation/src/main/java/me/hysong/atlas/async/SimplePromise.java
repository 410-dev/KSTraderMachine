package me.hysong.atlas.async;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimplePromise {
    private final Callable<?> task;
    private SimplePromise next;
    private Runnable onFailure;

    private static final ExecutorService EXEC =
        Executors.newCachedThreadPool();

    public SimplePromise(Callable<?> task) {
        this.task = task;
    }

    /** Start with a Runnable (which we wrap as Callable<Void>). */
    public static SimplePromise runAsync(Runnable r) {
        return new SimplePromise(Executors.callable(r));
    }

    /** Chain the next Runnable to run if—and only if—this one succeeds. */
    public SimplePromise then(Runnable r) {
        this.next = new SimplePromise(Executors.callable(r));
        return this;
    }

    /** Handle any exception thrown by this or any previous step. */
    public SimplePromise onError(Runnable r) {
        this.onFailure = r;
        return this;
    }

    /** Kick everything off. Returns immediately; nothing ever blocks. */
    public void start() {
        EXEC.submit(() -> {
            try {
                task.call();
                if (next != null) {
                    next.start();
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (onFailure != null) {
                    onFailure.run();
                }
            }
        });
    }
}
