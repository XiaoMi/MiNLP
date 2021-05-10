package com.xiaomi.duckling.server;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Response {
    private int code;
    private String msg;
}
