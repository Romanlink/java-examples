package com.example.chat.gpt.engin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * @Author liangwang
 * @create 4/4/24 17:51
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Accessors(chain = true)
public class EnoAiChatRequest implements Serializable {

    private String chatId;
    private Boolean stream;
    private Boolean detail;
    private String appId;
    private Object variables;

    private List<EnoAiMessage> messages;

    public static EnoAiChatRequest buildDefault(String question) {
        return EnoAiChatRequest.builder()
                .chatId(null)
                .stream(true)
                .detail(false)
                .messages(Arrays.asList(EnoAiMessage.buildDefault(question)))
                .build();

    }
}
