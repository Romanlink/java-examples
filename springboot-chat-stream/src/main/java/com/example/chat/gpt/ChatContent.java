package com.example.chat.gpt;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * @Author liangwang
 * @create 3/19/24 10:57
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Builder
public class ChatContent implements Serializable {

    private static final Integer QUESTION = 1;
    private static final Integer ANSWER = 2;

    private Integer id;

    private Integer chatId;

    private String icon;

    private Date createTime;

    private String content;

    /**
     * 1:question  2:answer
     */
    private Integer type;

    private Integer questionId;

    private String question;

    public void buildAsk(Integer chatId, String question, String icon) {
        build(chatId, question, icon, QUESTION, null);
    }

    public void buildAns(Integer chatId, String question, String icon, ChatContent chatContent) {
        build(chatId, question, icon, ANSWER, chatContent);
    }

    private void build(Integer chatId, String question, String icon, Integer type, ChatContent chatContent) {
        this.setChatId(chatId);
        this.setContent(question);
        this.setIcon(icon);
        this.setCreateTime(new Date());
        this.setType(type);
        this.setQuestionId(chatContent == null ? null : chatContent.getId());
        this.setQuestion(chatContent == null ? null : chatContent.getContent());
    }


}
