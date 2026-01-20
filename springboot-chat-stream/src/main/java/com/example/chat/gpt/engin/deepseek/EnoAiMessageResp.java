package com.example.chat.gpt.engin.deepseek;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @Author liangwang
 * @create 4/3/24 10:02
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EnoAiMessageResp {

    private Integer id;

    private String model;

    private JSONObject usages;

    private List<EnoAiChoice> choices;

    private String object;

    private Boolean created;


}
