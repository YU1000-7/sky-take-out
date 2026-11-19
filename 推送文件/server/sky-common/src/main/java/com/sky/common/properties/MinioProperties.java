package com.sky.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "sky.minio")
public class MinioProperties {

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;
    private String publicUrl;
    /** 是否启用 MinIO；关闭时退化为本地磁盘存储，便于无 MinIO 环境直接演示。 */
    private boolean enabled = false;
    /** 本地存储目录，仅 enabled=false 时生效。 */
    private String localDir = "./uploads";
    /** 本地文件访问前缀，需能被前端直接访问。 */
    private String localBaseUrl = "http://127.0.0.1:18080/uploads";
}
