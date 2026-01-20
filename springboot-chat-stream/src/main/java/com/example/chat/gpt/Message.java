package com.example.chat.gpt;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @Author liangwang
 * @create 3/19/24 10:57
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private MessageType messageType;
    private UserType userType;
    private String message;
    private Date date;
    private ChatContent chatContent;

    public Message(MessageType messageType, UserType userType, String message, ChatContent chatContent) {
        this.messageType = messageType;
        this.userType = userType;
        this.message = message;
        this.date = new Date();
        this.chatContent = chatContent;
    }
}
