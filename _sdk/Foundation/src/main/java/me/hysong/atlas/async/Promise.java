package me.hysong.atlas.async;

import java.util.concurrent.*;
import java.util.function.*;

public class Promise<T> {
    private static final ExecutorService DEFAULT_EXECUTOR =
        Executors.newCachedThreadPool();

    private final CompletableFuture<T> future;

    private Promise(CompletableFuture<T> future) {
        this.future = future;
    }

    /** Start a promise that runs supplier asynchronously. */
    public static <U> Promise<U> supplyAsync(Supplier<U> supplier) {
        CompletableFuture<U> cf = CompletableFuture.supplyAsync(supplier, DEFAULT_EXECUTOR);
        return new Promise<>(cf);
    }

    /** Chain a success callback that returns a new value. */
    public <U> Promise<U> then(Function<? super T, ? extends U> onSuccess) {
        CompletableFuture<U> next =
            future.thenApplyAsync(onSuccess, DEFAULT_EXECUTOR);
        return new Promise<>(next);
    }

    /** Chain an action that returns another Promise. */
    public <U> Promise<U> thenCompose(Function<? super T, Promise<U>> fn) {
        CompletableFuture<U> next =
            future.thenComposeAsync(t -> fn.apply(t).future, DEFAULT_EXECUTOR);
        return new Promise<>(next);
    }

    /** Handle exceptions in the chain. */
    public Promise<T> onError(Function<Throwable, ? extends T> onError) {
        CompletableFuture<T> handled =
            future.exceptionally(onError);
        return new Promise<>(handled);
    }

    /** Register a final callback (no return value). */
    public void finallyDo(Consumer<? super T> action, Consumer<Throwable> errorHandler) {
        future.whenCompleteAsync((result, ex) -> {
            if (ex != null) errorHandler.accept(ex);
            else action.accept(result);
        }, DEFAULT_EXECUTOR);
    }

    /** (Optional) block and get the result. Use sparingly off critical threads. */
    public T get() throws InterruptedException, ExecutionException {
        return future.get();
    }
}
