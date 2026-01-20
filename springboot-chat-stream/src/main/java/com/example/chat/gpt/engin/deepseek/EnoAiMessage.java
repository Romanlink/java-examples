package com.example.chat.gpt.engin.deepseek;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @Author liangwang
 * @create 4/3/24 10:24
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Accessors(chain = true)
public class EnoAiMessage implements Serializable {

    private String role;

    private String content;

    public static EnoAiMessage buildDefault(String question) {
        return EnoAiMessage.builder().role("user").content(question).build();

    }
}
