/*
 * Copyright (c) 2020, Xiaomi and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xiaomi.duckling.benchmark;


import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import com.xiaomi.duckling.Api;
import com.xiaomi.duckling.Types;
import com.xiaomi.duckling.Types.Context;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 1, time = 20)
@Measurement(iterations = 100000)
@Fork(1)
@State(Scope.Benchmark)
public class DateTimeBenchmark {
    Context context = new Context(ZonedDateTime.of(LocalDateTime.of(2016, 12, 8, 11, 30, 30), Types.ZoneCN()), Locale.CHINA);
    final Types.Options option = new Types.Options(Sets.newHashSet("time", "duration"), false);
    final Random rand = new Random();


    private List<String> queries;

    @Setup
    public void setup() throws Exception {
        URL url = Resources.getResource(DateTimeBenchmark.class, "/time.txt");
        queries = Resources.readLines(url, StandardCharsets.UTF_8);
        System.out.println(String.format("read %s examples", queries.size()));
    }

    public String query() {
        int n = rand.nextInt(queries.size());
        return queries.get(n);
    }

    @Benchmark
    public void duckling() {
        Api.analyzeJ(query(), context, option);
    }

    public static void main(String[] args) throws RunnerException, IOException {
        Options opt = new OptionsBuilder()
                .include(".*" + DateTimeBenchmark.class.getSimpleName() + ".*")
                .build();

        new Runner(opt).run();
    }
}
