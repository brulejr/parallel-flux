/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 Jon Brule <brulejr@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.jrb.labs.parallelflux.service;

import io.jrb.labs.parallelflux.datafill.TodoDatafill;
import io.jrb.labs.parallelflux.model.OverallSummary;
import io.jrb.labs.parallelflux.model.Todo;
import io.jrb.labs.parallelflux.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.GroupedFlux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.Comparator;

@Service
@Slf4j
public class TodoService {

    private final TodoRemoteClient remoteClient;
    private final Scheduler parallelScheduler;
    private final int parallelSchedulerSize;

    public TodoService(
            final TodoDatafill datafill,
            final TodoRemoteClient remoteClient
    ) {
        this.remoteClient = remoteClient;

        parallelSchedulerSize = datafill.getParallelThreadPool().getSize();
        parallelScheduler = Schedulers.newParallel(
                datafill.getParallelThreadPool().getPrefix(), parallelSchedulerSize
        );
    }

    public Mono<OverallSummary> getOverallSummary(final RetrievalMode mode) {
        return getTodosByUser(mode)
                .reduce(
                        OverallSummary.builder().build(),
                        (overallSummary, user) -> OverallSummary.builder()
                                .userCount(overallSummary.getUserCount() + 1)
                                .todoCount(overallSummary.getTodoCount() + user.getTodos().stream().count())
                                .build()
                );
    }

    public Flux<User> getTodosByUser(final RetrievalMode mode) {
        switch(mode) {
            case USERID:
                return groupByUserId();
            case EVEN_ODD:
                return groupByEvenOdd();
            default:
                throw new UnsupportedOperationException("Unsupported mode [" + mode + "]");
        }
    }

    private Flux<User> groupByEvenOdd() {
        return Flux.concat(
                remoteClient.retrieveAllTodos()
                        .groupBy(todo -> todo.getUserId() % 2 == 0 ? GroupType.EVEN : GroupType.ODD)
                        .sort(Comparator.comparing(GroupedFlux::key))
                        .concatMap(group -> Flux.defer(() -> {
                            log.info("Processing group {}", group.key());
                            return group.groupBy(Todo::getUserId)
                                    .parallel(parallelSchedulerSize)
                                    .runOn(parallelScheduler)
                                    .flatMap(this::buildUser);
                        })).doOnNext(t -> log.info("t = {}", t))
        );
    }

    private Flux<User> groupByUserId() {
        return Flux.concat(
                remoteClient.retrieveAllTodos()
                        .groupBy(Todo::getUserId)
                        .parallel(parallelSchedulerSize)
                        .runOn(parallelScheduler)
                        .flatMap(this::buildUser)
                        .doOnNext(t -> log.info("t = {}", t))
        );
    }

    private Mono<User> buildUser(final GroupedFlux<Integer, Todo> g) {
        return Mono.defer(() -> {
            log.info("Getting user with todos - userId-{}", g.key());
            return remoteClient.findUser(g.key())
                    .zipWith(g.collectList())
                    .map(t -> t.getT1().withTodos(t.getT2()));
        });
    }

}
