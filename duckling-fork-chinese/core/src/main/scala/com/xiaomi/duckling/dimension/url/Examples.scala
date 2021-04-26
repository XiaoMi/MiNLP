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

package com.xiaomi.duckling.dimension.url

import com.xiaomi.duckling.dimension.DimExamples

trait Examples extends DimExamples {
  override val pairs = List(
    (UrlData("http://www.bla.com", "bla.com", Some("http")), List("http://www.bla.com")),
    (UrlData("www.bla.com:8080/path", "bla.com"), List("www.bla.com:8080/path")),
    (UrlData("https://myserver?foo=bar", "myserver", Some("https")), List("https://myserver?foo=bar")),
    (UrlData("cnn.com/info", "cnn.com"), List("cnn.com/info")),
    (
      UrlData("bla.com/path/path?ext=%23&foo=bla", "bla.com"),
      List("bla.com/path/path?ext=%23&foo=bla")
    ),
    (UrlData("localhost", "localhost"), List("localhost")),
    (UrlData("localhost:8000", "localhost"), List("localhost:8000")),
    (UrlData("http://kimchi", "kimchi", Some("http")), List("http://kimchi")),
    (UrlData("https://500px.com:443/about", "500px.com", Some("https")), List("https://500px.com:443/about")),
    (UrlData("www2.foo-bar.net?foo=bar", "foo-bar.net"), List("www2.foo-bar.net?foo=bar")),
    (
      UrlData("https://api.wit.ai/message?q=hi", "api.wit.ai", Some("https")),
      List("https://api.wit.ai/message?q=hi")
    ),
    (UrlData("aMaZon.co.uk/?page=home", "amazon.co.uk"), List("aMaZon.co.uk/?page=home")),
    (
      UrlData(
        "https://en.wikipedia.org/wiki/Uniform_Resource_Identifier#Syntax",
        "en.wikipedia.org",
        Some("https")
      ),
      List("https://en.wikipedia.org/wiki/Uniform_Resource_Identifier#Syntax")
    ),
    (
      UrlData("http://example.com/data.csv#cell=4,1-6,2", "example.com", Some("http")),
      List("http://example.com/data.csv#cell=4,1-6,2")
    ),
    (
      UrlData("http://example.com/bar.webm#t=40,80&xywh=160,120,320,240", "example.com", Some("http")),
      List("http://example.com/bar.webm#t=40,80&xywh=160,120,320,240")
    ),
    (
      UrlData("https://m.xiaomiyoupin.com/shop/detail?gid=118991", "m.xiaomiyoupin.com", Some("https")),
      List("https://m.xiaomiyoupin.com/shop/detail?gid=118991")
    )
  )
}
