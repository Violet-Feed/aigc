package violet.aigc.common.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
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
import violet.aigc.common.proto_gen.push.*;
import violet.aigc.common.service.MaterialService;
import violet.aigc.common.utils.FFmpegUtil;
import violet.aigc.common.utils.OSSUtil;
import violet.aigc.common.utils.SnowFlake;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MaterialServiceImpl implements MaterialService {
    @GrpcClient("push")
    private PushServiceGrpc.PushServiceBlockingStub pushStub;
    @Autowired
    private MaterialMapper materialMapper;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Value("${minimax.api-key}")
    private String apiKey;
    private WebClient webClient;
    private final SnowFlake materialIdGenerator = new SnowFlake(0, 0);
    private final String SOURCE_OSS_PATH = "material/source/%d.png";
    private final String IMAGE_OSS_PATH = "material/image/%d.png";
    private final String VIDEO_OSS_PATH = "material/video/%d.mp4";
    private final String COVER_OSS_PATH = "material/cover/%d.png";
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
        Material material = new Material(null, materialId, req.getMaterialType(), req.getUserId(), req.getPrompt(), req.getSourceUrl(), "", req.getSourceUrl(), model, new Date(), MaterialStatus.Generating_VALUE, "");
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
                                throw new RuntimeException("图片素材生成失败，错误信息：" + errMsg);
                            }
                            JSONObject data = responseJson.getJSONObject("data");
                            if (data == null) {
                                throw new RuntimeException("响应无 data 字段");
                            }
                            List<String> imageUrls = data.getJSONArray("image_urls").toJavaList(String.class);
                            if (imageUrls == null || imageUrls.isEmpty()) {
                                throw new RuntimeException("无图片 URL 返回");
                            }
                            String tempImageUrl = imageUrls.get(0);
                            String ossPath = String.format(IMAGE_OSS_PATH, materialId);
                            String ossAccessUrl = OSSUtil.upload(tempImageUrl, ossPath);
                            log.info("素材创建成功，素材ID：{}，OSS访问URL：{}", materialId, ossAccessUrl);
                            material.setMaterialUrl(ossAccessUrl);
                            material.setCoverUrl(ossAccessUrl);
                            material.setStatus(MaterialStatus.Succeeded_VALUE);
                        } catch (Exception e) {
                            log.error("素材处理失败，素材ID：{}", materialId, e);
                            material.setStatus(MaterialStatus.Failed_VALUE);
                        } finally {
                            updateAndPushMaterial(material);
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
                                throw new RuntimeException("视频生成失败，错误信息：" + errMsg);
                            }
                            String taskId = responseJson.getString("task_id");
                            if (taskId == null) {
                                throw new RuntimeException("响应无 task_id 字段");
                            }
                            redisTemplate.opsForValue().set("video_task:" + taskId, materialId.toString());
                        } catch (Exception e) {
                            log.error("视频素材处理失败，素材ID：{}", materialId, e);
                            material.setStatus(MaterialStatus.Failed_VALUE);
                            updateAndPushMaterial(material);
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
        String materialIdStr = redisTemplate.opsForValue().get("video_task:" + req.getTaskId());
        if (materialIdStr == null) {
            log.error("未找到对应的素材ID，任务ID：{}", req.getTaskId());
            BaseResp baseResp = BaseResp.newBuilder().setStatusCode(StatusCode.Not_Found_Error).setStatusMessage("未找到对应的素材ID").build();
            return resp.setBaseResp(baseResp).build();
        }
        Long materialId = Long.valueOf(materialIdStr);
        Material material = materialMapper.selectByMaterialId(materialId);
        if (material == null) {
            log.error("数据库中未找到素材记录，素材ID：{}", materialId);
            BaseResp baseResp = BaseResp.newBuilder().setStatusCode(StatusCode.Not_Found_Error).setStatusMessage("数据库中未找到素材记录").build();
            return resp.setBaseResp(baseResp).build();
        }
        try {
            if (req.getStatusCode() != 0 || "Fail".equals(req.getStatus())) {
                throw new RuntimeException("视频生成失败，错误信息：" + req.getStatusMsg());
            }
            String jsonStr = webClient.get()
                    .uri("https://api.minimaxi.com/v1/files/retrieve?file_id={fileId}", req.getFileId())
                    .retrieve()
                    .onStatus(httpStatus -> !httpStatus.is2xxSuccessful(),
                            clientResponse -> clientResponse.bodyToMono(String.class).map(errStr -> new RuntimeException("接口返回非成功状态：" + errStr)))
                    .bodyToMono(String.class)
                    .doOnNext(res -> log.info("视频下载接口响应，素材ID：{}，响应内容：{}", materialId, res))
                    .doOnError(ex -> log.error("视频下载接口调用异常，素材ID：{}", materialId, ex))
                    .block();
            JSONObject responseJson = JSONObject.parseObject(jsonStr);
            JSONObject httpBaseResp = responseJson.getJSONObject("base_resp");
            if (httpBaseResp == null || httpBaseResp.getInteger("status_code") != 0) {
                String errMsg = httpBaseResp != null ? httpBaseResp.getString("status_msg") : "未知错误";
                throw new RuntimeException("视频下载失败：" + errMsg);
            }
            String downloadUrl = responseJson.getString("download_url");
            if (downloadUrl == null || downloadUrl.isEmpty()) {
                throw new RuntimeException("响应格式异常：无有效 download_url");
            }
            String ossPath = String.format(VIDEO_OSS_PATH, materialId);
            log.info("开始上传视频到 OSS，素材ID：{}，下载地址：{}，OSS路径：{}", materialId, downloadUrl, ossPath);
            String ossAccessUrl = OSSUtil.upload(downloadUrl, ossPath);
            log.info("素材视频处理成功，素材ID：{}，OSS访问URL：{}", materialId, ossAccessUrl);
            material.setMaterialUrl(ossAccessUrl);
            try {
                byte[] coverBytes = FFmpegUtil.fetchFirstFrame(ossAccessUrl);
                String coverOssPath = String.format(COVER_OSS_PATH, materialId);
                String coverUrl = "";
                try (ByteArrayInputStream in = new ByteArrayInputStream(coverBytes)) {
                    coverUrl = OSSUtil.upload(in, coverOssPath);
                }
                log.info("素材封面生成成功，素材ID：{}，封面URL：{}", materialId, coverUrl);
                material.setCoverUrl(coverUrl);
            } catch (Exception coverEx) {
                // 封面失败是否当成整体失败，看你业务，这里只记日志
                log.error("素材封面生成失败，素材ID：{}", materialId, coverEx);
            }
            material.setStatus(MaterialStatus.Succeeded_VALUE);
            redisTemplate.delete("video_task:" + req.getTaskId());
            BaseResp baseResp = BaseResp.newBuilder().setStatusCode(StatusCode.Success).build();
            return resp.setBaseResp(baseResp).build();
        } catch (Exception e) {
            log.error("素材处理失败，素材ID：{}", materialId, e);
            material.setStatus(MaterialStatus.Failed_VALUE);
            BaseResp baseResp = BaseResp.newBuilder().setStatusCode(StatusCode.Server_Error).setStatusMessage("素材处理失败：" + e.getMessage()).build();
            return resp.setBaseResp(baseResp).build();
        } finally {
            updateAndPushMaterial(material);
        }
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

    private void updateAndPushMaterial(Material material) {
        if (!materialMapper.updateMaterialAfterUpload(material)) {
            log.error("素材状态更新失败，素材ID：{}", material.getMaterialId());
            material.setStatus(MaterialStatus.Failed_VALUE);
        }
        MaterialPacket materialPacket = MaterialPacket.newBuilder()
                .setMaterialId(material.getMaterialId())
                .setMaterialType(material.getMaterialType())
                .setCreateTime(material.getCreateTime().getTime())
                .setMaterialUrl(material.getMaterialUrl())
                .setCoverUrl(material.getCoverUrl())
                .setStatus(material.getStatus())
                .build();
        PushRequest pushRequest = PushRequest.newBuilder()
                .setUserId(material.getUserId())
                .setPacketType(PacketType.Material)
                .setMaterialPacket(materialPacket)
                .build();
        PushResponse pushResponse = pushStub.push(pushRequest);
        if (pushResponse.getBaseResp().getStatusCode() != StatusCode.Success) {
            log.error("素材消息推送失败，素材ID：{}，错误信息：{}", material.getMaterialId(), pushResponse.getBaseResp().getStatusMessage());
        }
    }
}
