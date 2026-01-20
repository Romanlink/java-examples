package com.example.chat.gpt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @Author liangwang
 * @create 3/19/24 10:57
 */
@Getter
@RequiredArgsConstructor
public enum UserType {
    USER("Q:%s\n"), BOT("A: %s\n\n\n");
    private final String code;
}
