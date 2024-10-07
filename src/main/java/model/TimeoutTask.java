package model;

import java.util.concurrent.TimeUnit;

public record TimeoutTask<T>(Task<T> task, long timeout, TimeUnit timeUnit) { }
