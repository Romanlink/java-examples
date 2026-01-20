package com.example.chat.service;

import com.alibaba.fastjson.JSONObject;
import com.example.chat.gpt.*;
import com.example.chat.gpt.engin.deepseek.EnoAiChatRequest;
import com.example.chat.gpt.engin.deepseek.EnoAiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Date;

/**
 * 流式聊天服务
 * 模拟大模型流式生成响应
 */
@Slf4j
@Service
public class StreamChatService implements CompletedCallBack{


    @Autowired
    private EnoAiClient enoAiClient;

    /**
     * 模拟大模型流式生成响应
     *
     * @param prompt 用户问题
     * @return 按字符/词汇流式输出的响应
     */
    public Flux<String> streamResponse(String prompt) {
        log.info("收到用户提问: {}", prompt);

        // 模拟大模型生成的回复内容
        String response = mockLLMResponse(prompt);

        // 将文本拆分成小块，使用响应式延迟模拟打字效果
        int chunkSize = 2; // 每次发送 2 个字符

        return Flux.fromArray(splitIntoChunks(response, chunkSize))
                .delayElements(Duration.ofMillis(30)) // 每 30ms 发送一个块
                .doOnComplete(() -> log.info("流式响应完成"));
    }

    /**
     * 将字符串拆分成固定大小的块
     */
    private String[] splitIntoChunks(String text, int chunkSize) {
        int length = (text.length() + chunkSize - 1) / chunkSize;
        String[] chunks = new String[length];
        for (int i = 0; i < length; i++) {
            int start = i * chunkSize;
            int end = Math.min(start + chunkSize, text.length());
            chunks[i] = text.substring(start, end);
        }
        return chunks;
    }

    /**
     * 模拟大模型生成内容
     * 实际项目可接入 OpenAI/通义千问等 API
     *
     * @param prompt 用户问题
     * @return 模拟的回复内容
     */
    private String mockLLMResponse(String prompt) {
        return """
                【Spring Boot 流式响应】
                您的问题是：%s

                这是一个模拟大模型流式输出的示例。
                在实际应用中，你可以：
                1. 接入 OpenAI API 使用 GPT-4
                2. 接入阿里云通义千问 API
                3. 接入本地部署的大模型

                流式响应的核心是：
                - 使用 Spring WebFlux 的 Flux
                - 返回 text/event-stream 格式
                - 前端使用 EventSource 或 fetch 接收

                这样就能实现像 ChatGPT 一样的丝滑体验！
                """.formatted(prompt);
    }

    public Flux<String> streamResponseForEno(String prompt) {
        log.info("收到用户提问: {}", prompt);

        // 模拟大模型生成的回复内容
        String response = mockLLMResponse(prompt);

        // 将文本拆分成小块，使用响应式延迟模拟打字效果
        int chunkSize = 2; // 每次发送 2 个字符

        return Flux.fromArray(splitIntoChunks(response, chunkSize))
                .delayElements(Duration.ofMillis(30)) // 每 30ms 发送一个块
                .doOnComplete(() -> log.info("流式响应完成"));
    }

    public Flux<String> streamResponseForEno(String q, Boolean autoCreateChat) {

        Chat curChat = null;
        if (autoCreateChat) {
            curChat = createChat(q);
        }
        ChatContent chatContent = curChat.buildAskContent(q, "icon");

        Integer chatId = curChat.getId();
        EnoAiChatRequest obj = EnoAiChatRequest.buildDefault(q);
        JSONObject jsonObject = JSONObject.parseObject(JSONObject.toJSONString(obj));

        Message userMessage = new Message(MessageType.TEXT, UserType.USER, jsonObject.toJSONString(), chatContent);
        return Flux.create(it -> {
            ChatSubscriber subscriber = new ChatSubscriber(it, this, userMessage, chatId, chatContent);
            Flux<String> openAiResponse = enoAiClient.getDeepSeekChatResponse(q);
            openAiResponse.subscribe(subscriber);
            it.onDispose(() -> subscriber.cancel());
        });
    }

    private Chat createChat(String question){
        Chat build = Chat.builder().title(question.length() > 20 ? question.substring(0,20) :
                        question)
                .createTime(new Date())
                .build();
        log.info("新的对话创建完成，对话id:{}", build.getId());
        return build;
    }

    @Override
    public void completed(Message questions, Integer sessionId, String response, ChatContent chatContent) {
        log.info("AI 返回结果回调处理结束");
    }

    @Override
    public void fail(Integer sessionId, ChatContent chatContent) {
        log.info("AI 回复失败，移除content，chatId:{}, chatContentId:{}", sessionId, chatContent);
    }
}
