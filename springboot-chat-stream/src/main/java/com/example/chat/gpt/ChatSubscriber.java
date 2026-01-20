package com.example.chat.gpt;

import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.FluxSink;

/**
 * @Author liangwang
 * @create 4/2/24 12:15
 */
@Slf4j
public class ChatSubscriber implements Subscriber<String> {
    private final FluxSink<String> sink; // 对接SSE输出的FluxSink
    private final CompletedCallBack callBack;
    private final Message userMessage;
    private final Integer chatId;
    private final ChatContent chatContent;
    private Subscription subscription;

    // 构造函数（保留原有参数）
    public ChatSubscriber(FluxSink<String> sink, CompletedCallBack callBack,
                          Message userMessage, Integer chatId, ChatContent chatContent) {
        this.sink = sink;
        this.callBack = callBack;
        this.userMessage = userMessage;
        this.chatId = chatId;
        this.chatContent = chatContent;
    }

    @Override
    public void onSubscribe(Subscription s) {
        this.subscription = s;
        s.request(Long.MAX_VALUE); // 按需请求所有流式数据
    }

    @Override
    public void onNext(String content) {
        try {
            // 核心修复：直接转发纯文本，删除JSON解析
            if (content != null && !content.isEmpty()) {
                log.info("DeepSeek返回流式数据：{}", content);
                sink.next(content); // 把文本片段发送到SSE响应
            }
        } catch (Exception e) {
            log.warn("处理单条流式数据异常", e);
            // 仅记录日志，不中断整个流
        }
    }

    @Override
    public void onError(Throwable t) {
        log.error("DeepSeek流式响应异常", t);
        // 回调失败逻辑（保留原有）
        callBack.fail(chatId, chatContent);
        sink.error(t); // 向上传递异常（可选，也可返回空流）
    }

    @Override
    public void onComplete() {
        log.info("DeepSeek流式响应完成");
        // 回调成功逻辑（保留原有）
        callBack.completed(userMessage, chatId, "", chatContent);
        sink.complete(); // 正常结束SSE流
    }

    // 取消订阅（保留原有）
    public void cancel() {
        if (subscription != null) {
            subscription.cancel();
        }
    }
}
