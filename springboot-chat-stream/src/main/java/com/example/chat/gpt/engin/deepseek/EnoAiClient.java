package com.example.chat.gpt.engin.deepseek;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.chat.gpt.ApiConstant;
import com.example.chat.gpt.HttpUtils;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.util.HashMap;

/**
 * @author liangwang
 * @date 2026-01-19 11:51
 */
@Slf4j
@Component
public class EnoAiClient {


    private WebClient webClient;

    @Value("${spring.profiles.active:local}")
    private String env;

    @Resource
    EnoAiClientConfig clientConfig;
    /**
     * dev采用代理访问
     */
    @PostConstruct
    public void init() {
        log.info("[CustomInit]  Environment -> {}", env);
        if (env.contains("local")) {
            initDev();
        } else {
            //如果你不需要代理的话，使用配置这个就可以了
            initProd();
        }
    }

    public void initDev() {
        SslContext sslContext = null;
        try {
            sslContext = SslContextBuilder
                    .forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
        // 创建HttpClient对象，并设置代理
        SslContext finalSslContext = sslContext;
        HttpClient httpClient = HttpClient.create()
                .secure(sslContextSpec -> sslContextSpec.sslContext(finalSslContext))
                .tcpConfiguration(tcpClient -> tcpClient.proxy(proxy ->
                        proxy.type(ProxyProvider.Proxy.HTTP).host("127.0.0.1").port(7890)));

        ClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);
        this.webClient = WebClient.builder().clientConnector(connector)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
    }


    public void initProd() {
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
    }



    // ======================== 新增 DeepSeek 流式调用方法 ========================
    /**
     * 调用 DeepSeek 大模型流式接口
     * @param prompt 用户提问内容
     * @return 流式响应的文本片段（解析后纯文本，非原始JSON）
     */
    public Flux<String> getDeepSeekChatResponse(String prompt) {
        // 1. 前置校验
        if (clientConfig.getToken() == null || clientConfig.getToken().trim().isEmpty()) {
            log.warn("DeepSeek API Key为空，返回空流");
            return Flux.empty();
        }

        // 2. 构建DeepSeek参数（不变）
        JSONObject deepSeekParams = buildDeepSeekRequestParams(prompt);
        String deepSeekApiUrl = clientConfig.obtainChatMethod();
        String chatToken = clientConfig.getToken();
        log.info("请求DeepSeek流式接口: {}, chatToken: {}", deepSeekApiUrl, chatToken);
        log.info("请求参数: {}", deepSeekParams.toJSONString());

        // 3. 核心调用逻辑（仅处理真正的API错误）
        return webClient.post()
                .uri(deepSeekApiUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + chatToken)
                .header(HttpHeaders.CONTENT_TYPE, clientConfig.getContentType())
                .bodyValue(deepSeekParams.toJSONString())
                .retrieve()
                // 仅处理4xx/5xx状态码（真正的API错误）
                .onStatus(HttpStatusCode::isError, response -> {
                    return response.bodyToMono(String.class)
                            .defaultIfEmpty("无错误详情")
                            .flatMap(errorBody -> {
                                String errMsg = String.format("DeepSeek API错误: %s, 详情: %s",
                                        response.statusCode(), errorBody);
                                log.error(errMsg);
                                return Mono.error(new RuntimeException(errMsg));
                            });
                })
                // 读取流式响应
                .bodyToFlux(String.class)
                .defaultIfEmpty("")
                // 解析DeepSeek的流式响应（提取纯文本）
                .map(this::parseDeepSeekStreamResponse)
                // 过滤空内容
                .filter(content -> content != null && !content.isEmpty())
                // 流完成日志
                .doOnComplete(() -> log.info("DeepSeek流式响应正常结束"))
                // 仅捕获网络IO异常
                .onErrorResume(IOException.class, ex -> {
                    log.error("DeepSeek网络调用异常", ex);
                    return Flux.empty(); // 空流正常结束，不报错
                });
    }

    // 解析方法：兼容空行/仅[DONE]的正常场景
    private String parseDeepSeekStreamResponse(String streamLine) {
        // 1. 正常空行/结束标记：返回空字符串（后续过滤），不报错
        if (streamLine == null || streamLine.trim().isEmpty() || streamLine.contains("[DONE]")) {
            return "";
        }

        // 2. 移除 DeepSeek 流式前缀（data: ）
        String jsonStr = streamLine.trim();
        if (jsonStr.startsWith("data: ")) {
            jsonStr = jsonStr.substring(6).trim();
            if (jsonStr.equals("[DONE]") || jsonStr.isEmpty()) {
                return "";
            }
        }

        // 3. 解析失败：返回空字符串（不报错），仅记录日志
        try {
            JSONObject responseJson = JSON.parseObject(jsonStr);
            if (responseJson.containsKey("choices") && !responseJson.getJSONArray("choices").isEmpty()) {
                JSONObject choice = responseJson.getJSONArray("choices").getJSONObject(0);
                JSONObject delta = choice.getJSONObject("delta");
                return delta != null ? delta.getString("content") : "";
            }
            return "";
        } catch (Exception e) {
            log.warn("解析单行文法错误（正常兼容）: {}", streamLine);
            return ""; // 解析失败也返回空，不报错
        }
    }

    /**
     * 构建 DeepSeek 标准请求参数
     * 参考文档：https://platform.deepseek.com/docs/api/chat
     */
    private JSONObject buildDeepSeekRequestParams(String prompt) {
        JSONObject params = new JSONObject();
        // 模型名称（必填）
        params.put("model", clientConfig.getModel());
        // 流式返回（必须为 true 才能流式输出）
        params.put("stream", true);
        // 温度（0~1，值越高越随机）
        params.put("temperature", 0.7);
        // 最大生成长度
        params.put("max_tokens", 2048);
        // 对话消息（必填，格式为数组，包含用户消息）
        JSONObject message = new JSONObject();
        message.put("role", "user"); // 角色：user/assistant/system
        message.put("content", prompt); // 用户提问内容
        params.put("messages", new JSONObject[]{message});
        return params;
    }

}
