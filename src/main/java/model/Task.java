package model;

import pub.fi.ThrowableSupplier;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@Getter
@RequiredArgsConstructor
public class Task<T> {
    private final Instant start;
    private final String taskId;
    private final ThrowableSupplier<T> taskLogic;
    private final int maxRetries;
    private volatile boolean isCancelled;

    public void cancel() {
        this.isCancelled = true;
    }
}
