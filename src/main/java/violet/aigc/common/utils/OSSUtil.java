package violet.aigc.common.utils;

import com.aliyun.oss.*;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.InputStream;
import java.net.URL;

@Slf4j
@Component
public class OSSUtil {
    private static String endpoint;
    private static String region;
    private static String bucketName;
    private static String accessKeyId;
    private static String accessKeySecret;
    private static OSS ossClient;

    @Value("${aliyun.oss.endpoint}")
    public void setEndpoint(String endpoint) {
        OSSUtil.endpoint = endpoint;
    }

    @Value("${aliyun.oss.region}")
    public void setRegion(String region) {
        OSSUtil.region = region;
    }

    @Value("${aliyun.oss.bucket-name}")
    public void setBucketName(String bucketName) {
        OSSUtil.bucketName = bucketName;
    }

    @Value("${aliyun.oss.access-key-id:}")
    public void setAccessKeyId(String accessKeyId) {
        OSSUtil.accessKeyId = accessKeyId;
    }

    @Value("${aliyun.oss.access-key-secret:}")
    public void setAccessKeySecret(String accessKeySecret) {
        OSSUtil.accessKeySecret = accessKeySecret;
    }

    @PostConstruct
    public void initOssClient() {
        try {
            if (accessKeyId == null || accessKeyId.isEmpty() || accessKeySecret == null || accessKeySecret.isEmpty()) {
                throw new RuntimeException("OSS 密钥（access-key-id/access-key-secret）未设置");
            }
            if (endpoint == null || bucketName == null || region == null) {
                throw new RuntimeException("OSS 核心配置（endpoint/bucket-name/region）未设置");
            }
            ClientBuilderConfiguration clientConfig = new ClientBuilderConfiguration();
            clientConfig.setSignatureVersion(SignVersion.V4);
            ossClient = OSSClientBuilder.create()
                    .endpoint(endpoint)
                    .region(region)
                    .credentialsProvider(new DefaultCredentialProvider(accessKeyId, accessKeySecret))
                    .clientConfiguration(clientConfig)
                    .build();
            log.info("OSSClient 初始化成功");
        } catch (Exception e) {
            log.error("OSSClient 初始化失败：" + e.getMessage());
            throw new RuntimeException("OSS 客户端启动失败", e);
        }
    }

    @PreDestroy
    public void destroyOssClient() {
        if (ossClient != null) {
            ossClient.shutdown();
            log.info("OSSClient 已销毁");
        }
    }

    public static String upload(String url, String path) {
        if (ossClient == null) {
            throw new RuntimeException("OSSClient 未初始化，无法上传");
        }
        try (InputStream inputStream = new URL(url).openStream()) {
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, path, inputStream);
            PutObjectResult result = ossClient.putObject(putObjectRequest);
            String ossAccessUrl = String.format("https://%s.%s/%s",
                    bucketName,
                    endpoint.replace("https://", ""),
                    path);
            return ossAccessUrl;
        } catch (OSSException oe) {
            String errMsg = String.format("OSS服务端异常：Code=%s, Msg=%s, RequestId=%s, HostId=%s", oe.getErrorCode(), oe.getErrorMessage(), oe.getRequestId(), oe.getHostId());
            throw new RuntimeException(errMsg);
        } catch (ClientException ce) {
            throw new RuntimeException("OSS客户端异常：" + ce.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("上传失败：" + e.getMessage(), e);
        }
    }

    public static String upload(InputStream inputStream, String path) {
        if (ossClient == null) {
            throw new RuntimeException("OSSClient 未初始化，无法上传");
        }
        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, path, inputStream);
            PutObjectResult result = ossClient.putObject(putObjectRequest);
            String ossAccessUrl = String.format("https://%s.%s/%s",
                    bucketName,
                    endpoint.replace("https://", ""),
                    path);
            return ossAccessUrl;
        } catch (OSSException oe) {
            String errMsg = String.format("OSS服务端异常：Code=%s, Msg=%s, RequestId=%s, HostId=%s", oe.getErrorCode(), oe.getErrorMessage(), oe.getRequestId(), oe.getHostId());
            throw new RuntimeException(errMsg);
        } catch (ClientException ce) {
            throw new RuntimeException("OSS客户端异常：" + ce.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("上传失败：" + e.getMessage(), e);
        }
    }

}
