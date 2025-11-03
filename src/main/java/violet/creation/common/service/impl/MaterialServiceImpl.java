package violet.creation.common.service.impl;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import violet.creation.common.proto_gen.creation.*;
import violet.creation.common.service.MaterialService;
import violet.creation.common.utils.OSSUtil;
import violet.creation.common.utils.SnowFlake;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class MaterialServiceImpl implements MaterialService {
    @Value("${minimax.api-key}")
    private String apiKey;
    private WebClient webClient;
    private final SnowFlake materialIdGenerator = new SnowFlake(0, 0);
    private final String OSS_PATH = "material/%d.%s";

    @PostConstruct
    public void initWebClient() {
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public CreateMaterialResponse createMaterial(CreateMaterialRequest req) {
        Long materialId = materialIdGenerator.nextId();
        Map<String, Object> requestMap = new HashMap<>(3);
        requestMap.put("model", "image-01");
        requestMap.put("prompt", req.getPrompt());
        requestMap.put("prompt_optimizer", true);
        webClient.post()
                .uri("https://api.minimaxi.com/v1/image_generation")
                .bodyValue(requestMap)
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(jsonStr -> {
                    try {
                        JSONObject responseJson = JSONObject.parseObject(jsonStr);
                        JSONObject baseResp = responseJson.getJSONObject("base_resp");
                        if (baseResp == null || baseResp.getInteger("status_code") != 0) {
                            String errMsg = baseResp != null ? baseResp.getString("status_msg") : "未知错误";
                            log.error("图片生成失败，素材ID：{}，错误信息：{}", materialId, errMsg);
                            return;
                        }
                        JSONObject data = responseJson.getJSONObject("data");
                        if (data == null) {
                            log.error("响应无 data 字段，素材ID：{}", materialId);
                            return;
                        }
                        List<String> imageUrls = data.getJSONArray("image_urls").toJavaList(String.class);
                        if (imageUrls == null || imageUrls.isEmpty()) {
                            log.error("无图片 URL 返回，素材ID：{}", materialId);
                            return;
                        }
                        String tempImageUrl = imageUrls.get(0);
                        String ossPath = String.format(OSS_PATH, materialId, "png");
                        String  ossAccessUrl = OSSUtil.upload(tempImageUrl, ossPath);
                        log.info("素材创建成功，素材ID：{}，OSS访问URL：{}", materialId, ossAccessUrl);
                    } catch (Exception e) {
                        log.error("素材处理失败，素材ID：{}", materialId, e);
                    }
                })
                .doOnError(ex -> {
                    log.error("文生图接口调用异常，素材ID：{}", materialId, ex);
                })
                .subscribe();
        return null;
    }

    @Override
    public DeleteMaterialResponse deleteMaterial(DeleteMaterialRequest req) {
        return null;
    }

    @Override
    public GetMaterialByUserResponse getMaterialByUser(GetMaterialByUserRequest req) {
        return null;
    }
}
