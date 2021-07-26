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
 * FITNESS FOR A}] PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.jrb.labs.parallelflux.controller;

import io.jrb.labs.parallelflux.model.OverallSummary;
import io.jrb.labs.parallelflux.model.User;
import io.jrb.labs.parallelflux.service.RetrievalMode;
import io.jrb.labs.parallelflux.service.TodoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.MatrixVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@RestController
@Slf4j
public class TodoController {

    private final TodoService todoService;

    public TodoController(final TodoService todoService) {
        this.todoService = todoService;
    }

    @RequestMapping("/todos{mode}")
    public Flux<User> getTodosByUser(
            @MatrixVariable(pathVar = "mode", defaultValue = "EVEN_ODD") RetrievalMode mode
    ) {
        return Flux.defer(() -> {
            log.info("BEGIN - mode={}", mode);
            final long start = Instant.now().toEpochMilli();
            return todoService.getTodosByUser(mode)
                    .doOnComplete(() -> {
                        final long elapsedTime = Instant.now().toEpochMilli() - start;
                        log.info("END - mode={}, elapsedTime={}ms", mode, elapsedTime);
                    });
        });
    }

    @RequestMapping("/todos/summary{mode}")
    public Mono<OverallSummary> getTodoSummariesByUser(
            @MatrixVariable(pathVar = "mode", defaultValue = "EVEN_ODD") RetrievalMode mode
    ) {
        return Mono.defer(() -> {
            log.info("BEGIN - mode={}", mode);
            final long start = Instant.now().toEpochMilli();
            return todoService.getOverallSummary(mode)
                    .doOnNext((summary) -> {
                        final long elapsedTime = Instant.now().toEpochMilli() - start;
                        log.info("END - mode={}, elapsedTime={}ms", mode, elapsedTime);
                    });
        });
    }

}
