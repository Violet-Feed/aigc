package violet.aigc.common.consumer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONReader;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.UpsertReq;
import io.milvus.v2.service.vector.response.UpsertResp;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import violet.aigc.common.mapper.CreationGraphMapper;
import violet.aigc.common.pojo.Creation;
import violet.aigc.common.utils.QwenUtil;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class CreationConsumer {
    @Autowired
    private MilvusClientV2 milvusClient;
    @Autowired
    private CreationGraphMapper creationGraphMapper;

    @KafkaListener(topics = "mysql.violet.creation", groupId = "aigc_consumer")
    public void listen(ConsumerRecord<String, String> record) {
        log.info("Received message: key = {}, value = {}, partition = {}, offset = {}", record.key(), record.value(), record.partition(), record.offset());
        JSONObject json = JSON.parseObject(record.value());
        Creation creation = JSON.parseObject(json.getString("payload"), Creation.class, JSONReader.Feature.SupportSmartMatch);
        List<Float> titleEmbedding = QwenUtil.getTextEmbedding(creation.getTitle());
        JSONObject row = new JSONObject();
        row.put("creation_id", creation.getCreationId());
        row.put("rec_embeddings", titleEmbedding);
        row.put("title", creation.getTitle());
        List<JsonObject> data = Collections.singletonList(new Gson().fromJson(row.toJSONString(), JsonObject.class));
        UpsertReq upsertReq = UpsertReq.builder()
                .collectionName("creation")
                .data(data)
                .build();
        UpsertResp upsertResp = milvusClient.upsert(upsertReq);
        log.info("Upserted creation_id {} into Milvus with cnt: {}", creation.getCreationId(), upsertResp.getUpsertCnt());

        //按理应该放在另一个消费者组
        creationGraphMapper.createCreation(creation.getUserId(), creation.getCreationId());
    }
}
