/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/
package org.apache.james.task;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.james.util.MDCBuilder;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

public class SerialTaskManagerWorker implements TaskManagerWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger(SerialTaskManagerWorker.class);

    private final Scheduler taskExecutor;
    private final Listener listener;
    private final AtomicReference<Tuple2<TaskId, Disposable>> runningTask;
    private final Set<TaskId> cancelledTasks;
    private final Duration pollingInterval;

    public SerialTaskManagerWorker(Listener listener, Duration pollingInterval) {
        this.pollingInterval = pollingInterval;
        this.taskExecutor = Schedulers.newBoundedElastic(1, 1, "task executor");
        this.listener = listener;
        this.cancelledTasks = Sets.newConcurrentHashSet();
        this.runningTask = new AtomicReference<>();
    }

    @Override
    public Mono<Task.Result> executeTask(TaskWithId taskWithId) {
        if (!cancelledTasks.remove(taskWithId.getId())) {
            Mono<Task.Result> taskMono = Mono.fromCallable(() -> runWithMdc(taskWithId, listener)).cache();
            Disposable disposable = taskMono.subscribeOn(taskExecutor)
                .subscribe();
            runningTask.set(Tuples.of(taskWithId.getId(), disposable));

            return Mono.using(
                () -> pollAdditionalInformation(taskWithId).subscribe(),
                ignored -> taskMono
                    .onErrorResume(exception -> Mono.from(handleExecutionError(taskWithId, listener, exception))
                            .thenReturn(Task.Result.PARTIAL)),
                Disposable::dispose);
        } else {
            return Mono.from(listener.cancelled(taskWithId.getId(), taskWithId.getTask().details()))
                .then(Mono.empty());
        }
    }

    private Publisher<Void> handleExecutionError(TaskWithId taskWithId, Listener listener, Throwable exception) {
        if (exception instanceof CancellationException) {
            return listener.cancelled(taskWithId.getId(), taskWithId.getTask().details());
        } else {
            return listener.failed(taskWithId.getId(), taskWithId.getTask().details(), exception);
        }
    }

    private Flux<TaskExecutionDetails.AdditionalInformation> pollAdditionalInformation(TaskWithId taskWithId) {
        return Mono.fromCallable(() -> taskWithId.getTask().details())
            .delayElement(pollingInterval, Schedulers.elastic())
            .repeat()
            .<TaskExecutionDetails.AdditionalInformation>handle((maybeDetails, sink) -> maybeDetails.ifPresent(sink::next))
            .flatMap(information -> Mono.from(listener.updated(taskWithId.getId(), information)).thenReturn(information));
    }


    private Task.Result runWithMdc(TaskWithId taskWithId, Listener listener) {
        return MDCBuilder.withMdc(
            MDCBuilder.create()
                .addContext(Task.TASK_ID, taskWithId.getId())
                .addContext(Task.TASK_TYPE, taskWithId.getTask().type())
                .addContext(Task.TASK_DETAILS, taskWithId.getTask().details()),
            () -> run(taskWithId, listener).block());
    }

    private Mono<Task.Result> run(TaskWithId taskWithId, Listener listener) {
        return Mono.from(listener.started(taskWithId.getId()))
            .then(runTask(taskWithId, listener))
            .onErrorResume(InterruptedException.class, e -> Mono.from(listener.cancelled(taskWithId.getId(), taskWithId.getTask().details())).thenReturn(Task.Result.PARTIAL))
            .onErrorResume(Exception.class, e -> {
                LOGGER.error("Error while running task {}", taskWithId.getId(), e);
                return Mono.from(listener.failed(taskWithId.getId(), taskWithId.getTask().details(), e)).thenReturn(Task.Result.PARTIAL);
            });
    }

    private Mono<Task.Result> runTask(TaskWithId taskWithId, Listener listener) {
        return Mono.from(Task.toReactiveTask(taskWithId.getTask())
            .runReactive())
            .doOnNext(result2 -> result2.onComplete(result -> Mono.from(listener.completed(taskWithId.getId(), result, taskWithId.getTask().details())).block())
                .onFailure(() -> {
                    LOGGER.error("Task was partially performed. Check logs for more details. Taskid : " + taskWithId.getId());
                    Mono.from(listener.failed(taskWithId.getId(), taskWithId.getTask().details())).block();
                }));
    }

    @Override
    public void cancelTask(TaskId taskId) {
        cancelledTasks.add(taskId);
        Optional.ofNullable(runningTask.get())
            .filter(task -> task.getT1().equals(taskId))
            .ifPresent(task -> task.getT2().dispose());
    }

    @Override
    public Publisher<Void> fail(TaskId taskId, Optional<TaskExecutionDetails.AdditionalInformation> additionalInformation, String errorMessage, Throwable reason) {
        return listener.failed(taskId, additionalInformation, errorMessage, reason);
    }

    @Override
    public void close() {
        taskExecutor.dispose();
    }
}
