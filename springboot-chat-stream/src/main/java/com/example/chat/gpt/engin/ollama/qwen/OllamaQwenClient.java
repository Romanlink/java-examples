package com.example.chat.gpt.engin.ollama.qwen;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import javax.net.ssl.SSLException;
import java.io.IOException;

/**
 * 本地 Ollama 客户端（调用 qwen:7b 模型）
 * 完全沿用 DeepSeek 客户端的结构和流式处理逻辑
 * @author liangwang
 * @date 2026-01-27
 */
@Slf4j
@Component
public class OllamaQwenClient {

    private WebClient webClient;

    @Value("${spring.profiles.active:local}")
    private String env;

    @Resource
    private OllamaQwenClientConfig clientConfig;

    /**
     * 初始化 WebClient（和 DeepSeek 一致，local 环境可走代理，prod 直连本地）
     */
    @PostConstruct
    public void init() {
        log.info("[OllamaInit]  Environment -> {}", env);
        if (env.contains("local")) {
            initDev(); // 本地环境（如需代理可保留，本地 Ollama 一般无需代理）
        } else {
            initProd(); // 生产环境（直连本地 Ollama）
        }
    }

    /**
     * 本地开发环境初始化（保留代理逻辑，和 DeepSeek 一致）
     */
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
        SslContext finalSslContext = sslContext;
        HttpClient httpClient = HttpClient.create()
                .secure(sslContextSpec -> sslContextSpec.sslContext(finalSslContext))
                // 本地 Ollama 一般无需代理，可注释这行
                .tcpConfiguration(tcpClient -> tcpClient.proxy(proxy ->
                        proxy.type(ProxyProvider.Proxy.HTTP).host("127.0.0.1").port(7890)));

        ClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);
        this.webClient = WebClient.builder().clientConnector(connector)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
    }

    /**
     * 生产环境初始化（直连本地 Ollama，无需代理）
     */
    public void initProd() {
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
    }

    // ======================== 核心方法：Ollama 本地流式调用（和 DeepSeek 方法结构一致） ========================
    /**
     * 调用本地 Ollama + qwen:7b 模型流式接口
     * @param prompt 用户提问内容
     * @return 流式响应的文本片段（纯文本，和 DeepSeek 返回格式一致）
     */
    public Flux<String> getOllamaChatResponse(String prompt) {
        // 1. 前置校验（Ollama 本地无需 Token，仅校验模型名称）
        if (clientConfig.getModel() == null || clientConfig.getModel().trim().isEmpty()) {
            log.warn("Ollama 模型名称为空，返回空流");
            return Flux.empty();
        }

        // 2. 构建 Ollama 请求参数（适配 Ollama API 格式）
        JSONObject ollamaParams = buildOllamaRequestParams(prompt);
        String ollamaApiUrl = clientConfig.obtainChatMethod(); // 本地 Ollama API 地址
        log.info("请求Ollama本地流式接口: {}, 模型: {}", ollamaApiUrl, clientConfig.getModel());
        log.info("请求参数: {}", ollamaParams.toJSONString());

        // 3. 核心调用逻辑（完全沿用 DeepSeek 的异常处理风格）
        return webClient.post()
                .uri(ollamaApiUrl)
                // Ollama 本地无需认证，移除 Bearer Token
                .header(HttpHeaders.CONTENT_TYPE, clientConfig.getContentType())
                .bodyValue(ollamaParams.toJSONString())
                .retrieve()
                // 仅处理4xx/5xx状态码（真正的API错误）
                .onStatus(HttpStatusCode::isError, response -> {
                    return response.bodyToMono(String.class)
                            .defaultIfEmpty("无错误详情")
                            .flatMap(errorBody -> {
                                String errMsg = String.format("Ollama API错误: %s, 详情: %s",
                                        response.statusCode(), errorBody);
                                log.error(errMsg);
                                return Mono.error(new RuntimeException(errMsg));
                            });
                })
                // 读取流式响应
                .bodyToFlux(String.class)
                .defaultIfEmpty("")
                // 解析 Ollama 流式响应（提取纯文本，对齐 DeepSeek 返回格式）
                .map(this::parseOllamaStreamResponse)
                // 过滤空内容
                .filter(content -> content != null && !content.isEmpty())
                // 流完成日志
                .doOnComplete(() -> log.info("Ollama 本地流式响应正常结束"))
                // 仅捕获网络IO异常
                .onErrorResume(IOException.class, ex -> {
                    log.error("Ollama 本地调用网络异常", ex);
                    return Flux.empty(); // 空流正常结束，不报错
                });
    }

    // ======================== 适配 Ollama 的解析方法（替换 DeepSeek 的解析逻辑） ========================
    /**
     * 解析 Ollama 流式响应（提取纯文本，和 DeepSeek 解析结果格式一致）
     * Ollama 流式响应格式：{"model":"qwen:7b","response":"你","done":false}
     */
    private String parseOllamaStreamResponse(String streamLine) {
        // 1. 空行/结束标记：返回空字符串（后续过滤）
        if (streamLine == null || streamLine.trim().isEmpty()) {
            return "";
        }

        // 2. 解析 Ollama JSON 响应（无 data: 前缀，直接解析）
        try {
            JSONObject responseJson = JSON.parseObject(streamLine);
            // 判断是否是结束标记
            boolean isDone = responseJson.getBooleanValue("done");
            if (isDone) {
                return "";
            }
            // 提取 Ollama 的 response 字段（对应 DeepSeek 的 delta.content）
            String content = responseJson.getString("response");
            return content == null ? "" : content;
        } catch (Exception e) {
            log.warn("解析Ollama单行文法错误（正常兼容）: {}", streamLine);
            return ""; // 解析失败返回空，不报错
        }
    }

    // ======================== 构建 Ollama 请求参数（适配 Ollama API） ========================
    /**
     * 构建 Ollama 标准请求参数
     * 参考文档：https://github.com/ollama/ollama/blob/main/docs/api.md#generate-a-completion
     */
    private JSONObject buildOllamaRequestParams(String prompt) {
        JSONObject params = new JSONObject();
        // 模型名称（必填，本地 Ollama 下载的模型名，如 qwen:7b）
        params.put("model", clientConfig.getModel());
        // 用户提问内容（Ollama 用 prompt 字段，替代 DeepSeek 的 messages）
        params.put("prompt", prompt);
        // 流式返回（必须为 true 才能流式输出）
        params.put("stream", true);
        // 温度（0~1，值越高越随机，和 DeepSeek 一致）
        params.put("temperature", 0.7);
        // 最大生成长度（和 DeepSeek 一致）
        params.put("max_tokens", 2048);
        // 可选：Ollama 额外参数（如需可添加）
        params.put("format", "json"); // 可选，指定返回格式
        return params;
    }
}