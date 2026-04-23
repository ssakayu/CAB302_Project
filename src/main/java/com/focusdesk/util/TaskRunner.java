package com.focusdesk.util;

import javafx.application.Platform;
import javafx.concurrent.Task;

import java.util.function.Consumer;

public final class TaskRunner {
    private TaskRunner() {}

    public static <T> void run(
            java.util.concurrent.Callable<T> work,
            Consumer<T> onSuccess,
            Consumer<Throwable> onError
    ) {
        Task<T> task = new Task<>() {
            @Override protected T call() throws Exception {
                return work.call();
            }
        };

        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            Platform.runLater(() -> onError.accept(ex));
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }
}