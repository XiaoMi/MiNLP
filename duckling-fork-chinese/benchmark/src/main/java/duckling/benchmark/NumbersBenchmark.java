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

package duckling.benchmark;


import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import duckling.Api;
import duckling.Types;
import duckling.Types.Context;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 1, time = 20)
@Measurement(iterations = 1, time = 1, timeUnit = TimeUnit.MINUTES)
@Fork(1)
@State(Scope.Benchmark)
public class NumbersBenchmark {
    final Context context = new Context(ZonedDateTime.now(), Locale.CHINA);
    final Types.Options option = new Types.Options(Sets.newHashSet("numeral"), false);
    final Random rand = new Random();


    private List<String> queries;

    @Setup
    public void setup() throws Exception {
        URL url = Resources.getResource(NumbersBenchmark.class, "/number.txt");
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

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(".*" + NumbersBenchmark.class.getSimpleName() + ".*")
                .build();

        new Runner(opt).run();
    }
}
