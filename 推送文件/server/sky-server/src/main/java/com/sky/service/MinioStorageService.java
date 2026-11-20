package com.sky.service;

import com.sky.common.properties.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketPolicyArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MinioStorageService {

    private final MinioClient minioClient;
    private final MinioProperties properties;

    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        String objectName = buildObjectName(file);
        if (!properties.isEnabled()) {
            return saveToLocal(file, objectName);
        }

        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(properties.getBucketName())
                    .build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(properties.getBucketName())
                        .build());
            }
            applyPublicReadPolicy();

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucketName())
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            return properties.getPublicUrl() + "/" + properties.getBucketName() + "/" + objectName;
        } catch (Exception exception) {
            throw new IllegalStateException("文件上传失败", exception);
        }
    }

    private String buildObjectName(MultipartFile file) {
        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String extension = "";
        int dot = originalName.lastIndexOf('.');
        if (dot >= 0) {
            extension = originalName.substring(dot);
        }
        return "images/" + LocalDate.now() + "/" + UUID.randomUUID() + extension;
    }

    /**
     * 未启用 MinIO 时落本地磁盘，返回可直接访问的 URL（/uploads/** 由 WebMvcConfig 映射）。
     */
    private String saveToLocal(MultipartFile file, String objectName) {
        try {
            Path root = Paths.get(properties.getLocalDir()).toAbsolutePath().normalize();
            Path target = root.resolve(objectName).normalize();
            if (!target.startsWith(root)) {
                throw new IllegalStateException("非法的上传路径");
            }
            Files.createDirectories(target.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            String baseUrl = properties.getLocalBaseUrl();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            return baseUrl + "/" + objectName;
        } catch (Exception exception) {
            throw new IllegalStateException("文件上传失败", exception);
        }
    }

    private void applyPublicReadPolicy() throws Exception {
        String policy = "{\"Version\":\"2012-10-17\",\"Statement\":[{"
                + "\"Effect\":\"Allow\","
                + "\"Principal\":{\"AWS\":[\"*\"]},"
                + "\"Action\":[\"s3:GetObject\"],"
                + "\"Resource\":[\"arn:aws:s3:::" + properties.getBucketName() + "/*\"]"
                + "}]}";
        minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                .bucket(properties.getBucketName())
                .config(policy)
                .build());
    }
}
