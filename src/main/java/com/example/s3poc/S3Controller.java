package com.example.s3poc;

import java.io.IOException;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

@Controller
public class S3Controller {

    private final S3Service service;

    S3Controller(S3Service service) {
        this.service = service;
    }

    /** 画面: オブジェクト一覧とアップロードフォーム。 */
    @GetMapping("/")
    String index(Model model) {
        model.addAttribute("bucket", service.bucket());
        model.addAttribute("keys", service.list());
        return "index";
    }

    /** 画面: ファイルを S3 へ PUT する。 */
    @PostMapping("/objects")
    String upload(@RequestParam("file") MultipartFile file) throws IOException {
        String key = file.getOriginalFilename();
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("file name is required");
        }
        service.put(key, file.getBytes(), file.getContentType());
        return "redirect:/";
    }

    /** 画面: S3 から GET してダウンロードする。 */
    @GetMapping("/objects/{key}")
    ResponseEntity<byte[]> download(@PathVariable String key) {
        return toResponse(key, service.get(key));
    }

    /** REST: PUT /api/objects/{key} (本文をそのまま保存)。 */
    @PutMapping("/api/objects/{key}")
    @ResponseBody
    ResponseEntity<Void> apiPut(@PathVariable String key,
                                @RequestBody byte[] body,
                                @RequestHeader(value = HttpHeaders.CONTENT_TYPE,
                                        defaultValue = MediaType.APPLICATION_OCTET_STREAM_VALUE) String contentType) {
        service.put(key, body, contentType);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /** REST: GET /api/objects/{key}。 */
    @GetMapping("/api/objects/{key}")
    @ResponseBody
    ResponseEntity<byte[]> apiGet(@PathVariable String key) {
        return toResponse(key, service.get(key));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<String> badRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(NoSuchKeyException.class)
    ResponseEntity<String> notFound(NoSuchKeyException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("not found");
    }

    private static ResponseEntity<byte[]> toResponse(String key, ResponseBytes<GetObjectResponse> object) {
        String contentType = object.response().contentType();
        return ResponseEntity.ok()
                .contentType(contentType == null
                        ? MediaType.APPLICATION_OCTET_STREAM
                        : MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(key).build().toString())
                .body(object.asByteArray());
    }
}
