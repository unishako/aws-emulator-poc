package com.example.s3poc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

@WebMvcTest(S3Controller.class)
class S3ControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    S3Service service;

    @Test
    void apiPutStoresObjectAndReturns201() throws Exception {
        mvc.perform(put("/api/objects/hello.txt")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hello"))
                .andExpect(status().isCreated());

        // MockMvc は Content-Type に charset を付与するため前方一致で検証する
        verify(service).put(eq("hello.txt"), eq("hello".getBytes()), startsWith("text/plain"));
    }

    @Test
    void apiGetReturnsObjectBodyAndContentType() throws Exception {
        given(service.get("hello.txt")).willReturn(ResponseBytes.fromByteArray(
                GetObjectResponse.builder().contentType("text/plain").build(),
                "hello".getBytes()));

        mvc.perform(get("/api/objects/hello.txt"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().bytes("hello".getBytes()));
    }

    @Test
    void apiGetReturns404WhenKeyDoesNotExist() throws Exception {
        given(service.get("nothing.txt")).willThrow(NoSuchKeyException.builder().build());

        mvc.perform(get("/api/objects/nothing.txt"))
                .andExpect(status().isNotFound());
    }

    @Test
    void downloadReturnsAttachment() throws Exception {
        given(service.get("hello.txt")).willReturn(ResponseBytes.fromByteArray(
                GetObjectResponse.builder().contentType("text/plain").build(),
                "hello".getBytes()));

        mvc.perform(get("/objects/hello.txt"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"hello.txt\""))
                .andExpect(content().bytes("hello".getBytes()));
    }

    @Test
    void uploadStoresFileAndRedirectsToIndex() throws Exception {
        var file = new MockMultipartFile("file", "a.txt", "text/plain", "hi".getBytes());

        mvc.perform(multipart("/objects").file(file))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        verify(service).put("a.txt", "hi".getBytes(), "text/plain");
    }

    @Test
    void uploadWithoutFileNameReturns400AndStoresNothing() throws Exception {
        var file = new MockMultipartFile("file", "", "text/plain", "hi".getBytes());

        mvc.perform(multipart("/objects").file(file))
                .andExpect(status().isBadRequest());

        verify(service, never()).put(any(), any(), any());
    }

    @Test
    void indexListsObjectKeys() throws Exception {
        given(service.bucket()).willReturn("poc-bucket");
        given(service.list()).willReturn(List.of("hello.txt", "ui.txt"));

        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("poc-bucket")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/objects/hello.txt")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/objects/ui.txt")));
    }
}
