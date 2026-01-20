package com.example.chat.gpt;

/**
 * @Author liangwang
 * @create 3/19/24 10:57
 */
public interface CompletedCallBack {

    /**
     * 完成回掉
     *
     * @param questions
     * @param sessionId
     * @param response
     */
    void completed(Message questions, Integer sessionId, String response, ChatContent chatContent);

    void fail(Integer sessionId, ChatContent chatContent);

}
