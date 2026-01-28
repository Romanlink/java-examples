package com.example.chat.gpt.engin;

import com.alibaba.fastjson.annotation.JSONField;
import com.example.chat.gpt.engin.EnoAiMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author liangwang
 * @create 4/3/24 10:04
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class EnoAiChoice {

    //非流式输出结果
    private EnoAiMessage message;

    @JSONField(name = "finish_reason")
    private String finishReason;

    private Integer index;

    private EnoAiMessage delta;
}
