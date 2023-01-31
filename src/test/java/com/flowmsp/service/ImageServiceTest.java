package com.flowmsp.service;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.flowmsp.service.image.ImageService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Optional;

@RunWith(JUnit4.class)
public class ImageServiceTest {
    @Test
    public void test() throws FileNotFoundException {
        AmazonS3 s3 = AmazonS3ClientBuilder.standard()
                .withRegion(Regions.fromName(Optional.ofNullable(System.getenv("S3_REGION")).orElse("us-west-2")))
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .build();

        ImageService service = new ImageService(s3, "httpw://s3.amazonaws.com","flowmsp-test-image-bucket2", null);
        InputStream iStream = new FileInputStream(new File("C:/Users/mike/Downloads/image.jpg"));
        service.uploadImage(iStream, "cust", "loc", "image.jpg", "test user");
    }
}
