package com.xiaomi.duckling.server;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import com.xiaomi.duckling.Api;
import com.xiaomi.duckling.PlaceQuery;
import com.xiaomi.duckling.TokenVisualization;
import com.xiaomi.duckling.Types.Answer;
import com.xiaomi.duckling.Types.Context;
import com.xiaomi.duckling.Types.Options;
import com.xiaomi.duckling.dimension.EnumeratedDimension;
import com.xiaomi.duckling.dimension.numeral.NumeralOptions;

@Slf4j
@RestController
public class DucklingController {
    private List<EnumeratedDimension> DEFAULT_DIMENSION_LIST = Arrays.asList(EnumeratedDimension.values().clone());

    @PostMapping("/duckling/extract")
    public Mono<Response> demo(@RequestParam("sentence") String sentence, @RequestParam("dims") String dims) {
        log.info("demo - {}: {}", dims, sentence);
        sentence = StringEscapeUtils.escapeHtml4(sentence); //对输入进行转义处理，防止xss攻击
        String res = TokenVisualization.toHtml(sentence, parse(sentence, dims));
        return Mono.just(new Response(0, res));
    }

    List<Answer> parse(String text, String dims) {
        Options options;
        if (StringUtils.isNotBlank(dims)) {
            Set<String> set = Arrays.stream(dims.split(",")).map(String::toLowerCase).collect(Collectors.toSet());
            options = new Options(set, false);
        } else {
            options = new Options(DEFAULT_DIMENSION_LIST, false);
        }
        return Api.analyzeJ(text, new Context(ZonedDateTime.now(), Locale.CHINA), options);
    }

    @GetMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
    public String api(@RequestParam("query") String query, @RequestParam("dim") String dim) {
        log.info("api - {}: {}", dim, query);
        return DuckJavaHelper.answerAsJson(parse(query, dim));
    }

    @GetMapping("/api/place")
    public String place(@RequestParam("query") String query) {
        log.info("place: {}", query);
        return DuckJavaHelper.orElse(PlaceQuery.extract(query), "");
    }
}
