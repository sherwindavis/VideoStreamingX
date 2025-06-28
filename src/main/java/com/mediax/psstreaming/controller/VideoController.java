package com.mediax.psstreaming.controller;


import com.mediax.psstreaming.controller.util.LimitedInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api")
public class VideoController {

    @Value("${upload.dir}")
    private String uploadDir;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadVideo(@RequestParam("file") MultipartFile file) throws IOException {
        Path path = Paths.get(uploadDir, file.getOriginalFilename());
        Files.createDirectories(path.getParent());
        file.transferTo(path);
        return ResponseEntity.ok("Video uploaded: " + file.getOriginalFilename());
    }

    @GetMapping("/stream/{filename}")
    public ResponseEntity<Resource> streamVideo(
            @PathVariable String filename,
            @RequestHeader(value = "Range", required = false) String rangeHeader
    ) throws IOException {
        System.out.println("Request recieved");
        File videoFile = new File(uploadDir + "/" + filename);
        if (!videoFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        long fileLength = videoFile.length();
        long start = 0;
        long end = fileLength - 1;

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            String[] ranges = rangeHeader.substring("bytes=".length()).split("-");
            try {
                start = Long.parseLong(ranges[0]);
                if (ranges.length > 1 && !ranges[1].isEmpty()) {
                    end = Long.parseLong(ranges[1]);
                }
            } catch (NumberFormatException e) {
                // Invalid range; fallback to default full content
            }
        }

        if (start > end || end >= fileLength) {
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).build();
        }

        long contentLength = end - start + 1;
        InputStream inputStream = new BufferedInputStream(new FileInputStream(videoFile));
        inputStream.skip(start);
        InputStreamResource resource = new InputStreamResource(new LimitedInputStream(inputStream, contentLength));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "video/mp4");
        headers.set("Accept-Ranges", "bytes");
        headers.set("Content-Length", String.valueOf(contentLength));
        headers.set("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);

        return ResponseEntity
                .status(HttpStatus.PARTIAL_CONTENT)
                .headers(headers)
                .body(resource);
    }


    @GetMapping("/videos")
    public List<String> listVideos() {
        File folder = new File(uploadDir);
        String[] files = folder.list((dir, name) -> name.endsWith(".mp4"));
        return files != null ? List.of(files) : List.of();
    }
}
