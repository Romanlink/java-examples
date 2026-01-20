package com.example.chat.gpt.engin.deepseek;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * @Author liangwang
 * @create 4/9/24 18:38
 */
@Component
@PropertySource("classpath:/gpt/${spring.profiles.active}/eno_client_config.properties")
@ConfigurationProperties(prefix = "eno")
public class EnoAiClientConfig implements Serializable {

    /**
     * 请求地址host
     */
    private String host;

    /**
     * 请求方法
     */
    private String chatMethod;

    /**
     * token
     */
    private String token;

    private String model;

    private String contentType;

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getChatMethod() {
        return chatMethod;
    }

    public void setChatMethod(String chatMethod) {
        this.chatMethod = chatMethod;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String obtainChatMethod() {
        return host + chatMethod;
    }
}
