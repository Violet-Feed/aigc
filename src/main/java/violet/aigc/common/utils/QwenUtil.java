package violet.aigc.common.utils;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import javax.annotation.PostConstruct;
import java.util.List;

@Slf4j
@Component
public class QwenUtil {
    private static String apiKey;
    private static final String API_URL = "https://dashscope.aliyuncs.com/api/v1/services/embeddings/multimodal-embedding/multimodal-embedding";
    private static final String MODEL = "multimodal-embedding-v1";
    private static WebClient webClient;

    @Value("${aliyun.dashscope.api-key:}")
    public void setApiKey(String apiKey) {
        QwenUtil.apiKey = apiKey;
    }

    @PostConstruct
    public void initWebClient() {
        try {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                throw new RuntimeException("DashScope API Key 未配置");
            }
            webClient = WebClient.builder()
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();
            log.info("DashScope WebClient 初始化成功");
        } catch (Exception e) {
            log.error("DashScope WebClient 初始化失败", e);
            throw new RuntimeException("DashScope 工具类启动失败", e);
        }
    }

    public static List<Float> getTextEmbedding(String text) {
        // 1. 校验参数和资源
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("输入文本不能为空");
        }
        if (webClient == null) {
            throw new RuntimeException("DashScope WebClient 未初始化，无法调用接口");
        }

        JSONObject requestJson = new JSONObject();
        requestJson.put("model", MODEL);
        JSONObject inputJson = new JSONObject();
        JSONArray contentsArray = new JSONArray();
        JSONObject textContent = new JSONObject();
        textContent.put("text", text);
        contentsArray.add(textContent);
        inputJson.put("contents", contentsArray);
        requestJson.put("input", inputJson);

        log.info("DashScope 接口请求参数：{}", requestJson);

        String jsonStr = webClient.post()
                .uri(API_URL)
                .bodyValue(requestJson)
                .retrieve()
                .onStatus(httpStatus -> !httpStatus.is2xxSuccessful(),
                        clientResponse -> clientResponse.bodyToMono(String.class).map(errStr -> new RuntimeException(String.format("接口返回非成功状态[%s]：%s", clientResponse.statusCode(), errStr)))
                )
                .bodyToMono(String.class)
                .doOnNext(res -> log.info("DashScope 接口响应成功：{}", res))
                .doOnError(ex -> log.error("DashScope 接口调用异常", ex))
                .block();
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            throw new RuntimeException("DashScope 接口返回空响应");
        }
        JSONObject responseJson = JSONObject.parseObject(jsonStr);
        if (responseJson.containsKey("code")) {
            String code = responseJson.getString("code");
            String message = responseJson.getString("message");
            String requestId = responseJson.getString("request_id");
            throw new RuntimeException(
                    String.format("接口调用失败：code=%s, message=%s, requestId=%s", code, message, requestId)
            );
        }

        JSONObject outputObj = responseJson.getJSONObject("output");
        if (outputObj == null) {
            throw new RuntimeException("响应格式异常：未找到 output 字段");
        }

        JSONArray embeddingsArray = outputObj.getJSONArray("embeddings");
        if (embeddingsArray == null || embeddingsArray.isEmpty()) {
            throw new RuntimeException("响应中无向量数据");
        }

        JSONObject textEmbeddingObj = embeddingsArray.getJSONObject(0);
        JSONArray embeddingArray = textEmbeddingObj.getJSONArray("embedding");
        if (embeddingArray == null || embeddingArray.isEmpty()) {
            throw new RuntimeException("向量数据为空");
        }

        return embeddingArray.toJavaList(Float.class);
    }
}
