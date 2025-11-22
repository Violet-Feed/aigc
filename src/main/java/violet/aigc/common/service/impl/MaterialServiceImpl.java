package violet.aigc.common.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import violet.aigc.common.mapper.MaterialMapper;
import violet.aigc.common.pojo.Material;
import violet.aigc.common.proto_gen.aigc.*;
import violet.aigc.common.proto_gen.common.BaseResp;
import violet.aigc.common.proto_gen.common.StatusCode;
import violet.aigc.common.service.MaterialService;
import violet.aigc.common.utils.OSSUtil;
import violet.aigc.common.utils.SnowFlake;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MaterialServiceImpl implements MaterialService {
    @Autowired
    private MaterialMapper materialMapper;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Value("${minimax.api-key}")
    private String apiKey;
    private WebClient webClient;
    private final SnowFlake materialIdGenerator = new SnowFlake(0, 0);
    private final String OSS_PATH = "material/%d.%s";
    private final Integer PAGE_SIZE = 20;

    @PostConstruct
    public void initWebClient() {
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public CreateMaterialResponse createMaterial(CreateMaterialRequest req) {
        CreateMaterialResponse.Builder resp = CreateMaterialResponse.newBuilder();
        Long materialId = materialIdGenerator.nextId();
        String model = req.getMaterialType() == MaterialType.Image_VALUE ? "image-01" : "MiniMax-Hailuo-02";
        Material material = new Material(null, materialId, req.getMaterialType(), req.getUserId(), req.getPrompt(), req.getSourceUrl(), "", model, new Date(), MaterialStatus.Generating_VALUE, "");
        if (!materialMapper.insertMaterial(material)) {
            log.error("素材入库失败，素材ID：{}", materialId);
            BaseResp baseResp = BaseResp.newBuilder().setStatusCode(StatusCode.Server_Error).build();
            return resp.setBaseResp(baseResp).build();
        }
        JSONObject requestJson = new JSONObject();
        requestJson.put("model", model);
        requestJson.put("prompt", req.getPrompt());
        requestJson.put("prompt_optimizer", true);
        if (req.getMaterialType() == MaterialType.Image_VALUE) {
            requestJson.put("response_format", "url");
            requestJson.put("n", 1);
            if (!req.getSourceUrl().isEmpty()) {
                JSONArray subjectReference = new JSONArray();
                JSONObject referenceObj = new JSONObject();
                referenceObj.put("type", "character");
                referenceObj.put("image_file", req.getSourceUrl());
                subjectReference.add(referenceObj);
                requestJson.put("subject_reference", subjectReference);
            }
            webClient.post()
                    .uri("https://api.minimaxi.com/v1/image_generation")
                    .bodyValue(requestJson)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnNext(jsonStr -> {
                        try {
                            log.info("图片生成接口响应，素材ID：{}，响应内容：{}", materialId, jsonStr);
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
                            String ossAccessUrl = OSSUtil.upload(tempImageUrl, ossPath);
                            log.info("素材创建成功，素材ID：{}，OSS访问URL：{}", materialId, ossAccessUrl);
                            material.setMaterialUrl(ossAccessUrl);
                            material.setStatus(MaterialStatus.Succeeded_VALUE);
                            if (!materialMapper.updateMaterialAfterUpload(material)) {
                                log.error("素材更新失败，素材ID：{}", materialId);
                                return;
                            }
                            //todo:发送消息通知用户素材生成成功
                        } catch (Exception e) {
                            log.error("素材处理失败，素材ID：{}", materialId, e);
                        }
                    })
                    .doOnError(ex -> {
                        log.error("图片生成接口调用异常，素材ID：{}", materialId, ex);
                    })
                    .subscribe();
        } else if (req.getMaterialType() == MaterialType.Video_VALUE) {
            requestJson.put("duration", 6);
            requestJson.put("resolution", "768P");
            requestJson.put("callback_url", "/api/aigc/video_material_callback");
            if (!req.getSourceUrl().isEmpty()) {
                requestJson.put("resolution", "512P");
                requestJson.put("first_frame_image", req.getSourceUrl());
            }
            webClient.post()
                    .uri("https://api.minimaxi.com/v1/video_generation")
                    .bodyValue(requestJson)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnNext(jsonStr -> {
                        try {
                            log.info("视频生成接口响应，素材ID：{}，响应内容：{}", materialId, jsonStr);
                            JSONObject responseJson = JSONObject.parseObject(jsonStr);
                            JSONObject baseResp = responseJson.getJSONObject("base_resp");
                            if (baseResp == null || baseResp.getInteger("status_code") != 0) {
                                String errMsg = baseResp != null ? baseResp.getString("status_msg") : "未知错误";
                                log.error("视频生成失败，素材ID：{}，错误信息：{}", materialId, errMsg);
                                return;
                            }
                            String taskId = responseJson.getString("task_id");
                            if (taskId == null) {
                                log.error("响应无 task_id 字段，素材ID：{}", materialId);
                                return;
                            }
                            redisTemplate.opsForValue().set("video_task:" + taskId, materialId.toString());
                        } catch (Exception e) {
                            log.error("素材处理失败，素材ID：{}", materialId, e);
                        }
                    })
                    .doOnError(ex -> {
                        log.error("视频生成接口调用异常，素材ID：{}", materialId, ex);
                    })
                    .subscribe();
        }
        BaseResp baseResp = BaseResp.newBuilder().setStatusCode(StatusCode.Success).build();
        return resp.setBaseResp(baseResp).setMaterialId(materialId).build();
    }

    @Override
    public VideoMaterialCallbackResponse videoMaterialCallback(VideoMaterialCallbackRequest req) {
        VideoMaterialCallbackResponse.Builder resp = VideoMaterialCallbackResponse.newBuilder();
        Long materialId = Long.valueOf(redisTemplate.opsForValue().get("video_task:" + req.getTaskId()));
        if (materialId == null) {
            log.error("未找到对应的素材ID，任务ID：{}", req.getTaskId());
            BaseResp baseResp = BaseResp.newBuilder().setStatusCode(StatusCode.Not_Found_Error).setStatusMessage("未找到对应的素材ID").build();
            return resp.setBaseResp(baseResp).build();
        }
        if (req.getStatusCode() != 0 || "Fail".equals(req.getStatus())) {
            log.error("视频生成失败，素材ID：{}，错误信息：{}", materialId, req.getStatusMsg());
            Material material = new Material();
            material.setMaterialId(materialId);
            material.setMaterialUrl("");
            material.setStatus(MaterialStatus.Failed_VALUE);
            if (!materialMapper.updateMaterialAfterUpload(material)) {
                log.error("素材更新失败，素材ID：{}", materialId);
            }
            BaseResp baseResp = BaseResp.newBuilder().setStatusCode(StatusCode.Success).build();
            return resp.setBaseResp(baseResp).build();
        }
        String jsonStr = webClient.get()
                .uri("https://api.minimaxi.com/v1/files/retrieve?file_id={fileId}", req.getFileId())
                .retrieve()
                .onStatus(httpStatus -> !httpStatus.is2xxSuccessful(),
                        clientResponse -> clientResponse.bodyToMono(String.class).map(errStr -> new RuntimeException("接口返回非成功状态：" + errStr)))
                .bodyToMono(String.class)
                .doOnNext(res -> log.info("视频下载接口响应，素材ID：{}，响应内容：{}", materialId, res))
                .doOnError(ex -> log.error("视频下载接口调用异常，素材ID：{}", materialId, ex))
                // 阻塞获取结果（转同步），超时可添加：.block(Duration.ofSeconds(30))
                .block();
        try {
            JSONObject responseJson = JSONObject.parseObject(jsonStr);
            JSONObject httpBaseResp = responseJson.getJSONObject("base_resp");
            if (httpBaseResp == null || httpBaseResp.getInteger("status_code") != 0) {
                String errMsg = httpBaseResp != null ? httpBaseResp.getString("status_msg") : "未知错误";
                log.error("视频下载失败，素材ID：{}，错误信息：{}", materialId, errMsg);
                throw new RuntimeException("视频下载失败：" + errMsg);
            }
            String downloadUrl = responseJson.getString("download_url");
            if (downloadUrl == null || downloadUrl.isEmpty()) {
                log.error("响应无有效 download_url，素材ID：{}", materialId);
                throw new RuntimeException("响应格式异常：无有效 download_url");
            }
            String ossPath = String.format(OSS_PATH, materialId, "mp4");
            log.info("开始上传视频到 OSS，素材ID：{}，下载地址：{}，OSS路径：{}", materialId, downloadUrl, ossPath);
            String ossAccessUrl = OSSUtil.upload(downloadUrl, ossPath);
            log.info("素材处理成功，素材ID：{}，OSS访问URL：{}", materialId, ossAccessUrl);
            Material material = new Material();
            material.setMaterialId(materialId);
            material.setMaterialUrl(ossAccessUrl);
            material.setStatus(MaterialStatus.Succeeded_VALUE);
            if (!materialMapper.updateMaterialAfterUpload(material)) {
                log.error("素材状态更新失败，素材ID：{}，OSS访问URL：{}", materialId, ossAccessUrl);
                throw new RuntimeException("素材状态更新失败");
            }
            // todo: 发送消息通知用户素材生成成功（同步场景下可直接调用）
        } catch (Exception e) {
            log.error("素材处理失败，素材ID：{}", materialId, e);
            BaseResp baseResp = BaseResp.newBuilder().setStatusCode(StatusCode.Server_Error).setStatusMessage("素材处理失败：" + e.getMessage()).build();
            return resp.setBaseResp(baseResp).build();
        }
        redisTemplate.delete("video_task:" + req.getTaskId());
        BaseResp baseResp = BaseResp.newBuilder().setStatusCode(StatusCode.Success).build();
        return resp.setBaseResp(baseResp).build();
    }

    @Override
    public DeleteMaterialResponse deleteMaterial(DeleteMaterialRequest req) {
        DeleteMaterialResponse.Builder resp = DeleteMaterialResponse.newBuilder();
        if (!materialMapper.deleteMaterial(req.getMaterialId())) {
            log.error("素材删除失败，素材ID：{}", req.getMaterialId());
            BaseResp baseResp = BaseResp.newBuilder().setStatusCode(StatusCode.Server_Error).build();
            return resp.setBaseResp(baseResp).build();
        }
        BaseResp baseResp = BaseResp.newBuilder().setStatusCode(StatusCode.Success).build();
        return resp.setBaseResp(baseResp).build();
    }

    @Override
    public GetMaterialByUserResponse getMaterialByUser(GetMaterialByUserRequest req) {
        GetMaterialByUserResponse.Builder resp = GetMaterialByUserResponse.newBuilder();
        List<Material> materials = materialMapper.selectByUserId(req.getUserId(), (req.getPage() - 1) * PAGE_SIZE, PAGE_SIZE);
        List<violet.aigc.common.proto_gen.aigc.Material> materialDto = materials.stream().map(Material::toProto).collect(Collectors.toList());
        BaseResp baseResp = BaseResp.newBuilder().setStatusCode(StatusCode.Success).build();
        return resp.setBaseResp(baseResp).addAllMaterial(materialDto).build();
    }
}
