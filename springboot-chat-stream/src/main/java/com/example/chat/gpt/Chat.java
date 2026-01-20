package com.example.chat.gpt;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * @Author liangwang
 * @create 10/18/23 11:34
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Builder
public class Chat implements Serializable {

    private Integer id;

    private Integer accountId;

    private Integer orgId;

    private String title;

    private Date createTime;

    private Integer lastContentId;

    public boolean isFirstQuestion(){
        return lastContentId == null;
    }

    public ChatContent buildAskContent(String question, String icon) {
        ChatContent chatContent = ChatContent.builder()
                .build();
        chatContent.buildAsk(this.getId(), question, icon);
        return chatContent;
    }

    public ChatContent buildAnsContent(String answer, String icon, ChatContent aChatContent) {
        ChatContent chatContent = ChatContent.builder()
                .build();
        chatContent.buildAns(this.getId(), answer, icon, aChatContent);
        return chatContent;
    }
}
