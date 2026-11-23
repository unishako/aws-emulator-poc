package com.example.s3poc;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketLocationConstraint;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

@Service
public class S3Service implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(S3Service.class);

    private final S3Client s3;
    private final String bucket;
    private final String region;

    S3Service(S3Client s3,
              @Value("${app.s3.bucket}") String bucket,
              @Value("${app.s3.region}") String region) {
        this.s3 = s3;
        this.bucket = bucket;
        this.region = region;
    }

    /** 起動時にバケットが無ければ作成する。 */
    @Override
    public void run(ApplicationArguments args) {
        try {
            s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException e) {
            createBucket();
        } catch (S3Exception e) {
            if (e.statusCode() != 404) {
                throw e;
            }
            createBucket();
        }
    }

    private void createBucket() {
        s3.createBucket(CreateBucketRequest.builder()
                .bucket(bucket)
                .createBucketConfiguration(CreateBucketConfiguration.builder()
                        .locationConstraint(BucketLocationConstraint.fromValue(region))
                        .build())
                .build());
        log.info("created bucket {} in {}", bucket, region);
    }

    public void put(String key, byte[] data, String contentType) {
        s3.putObject(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(data));
    }

    public ResponseBytes<GetObjectResponse> get(String key) {
        return s3.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build());
    }

    public List<String> list() {
        return s3.listObjectsV2(ListObjectsV2Request.builder().bucket(bucket).build())
                .contents().stream().map(S3Object::key).toList();
    }

    public String bucket() {
        return bucket;
    }
}
