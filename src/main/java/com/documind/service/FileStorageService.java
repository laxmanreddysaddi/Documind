package com.documind.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;

@Service
public class FileStorageService {

    private final String uploadDir = System.getProperty("user.dir") + File.separator + "uploads";
    private static final Logger log =
            LoggerFactory.getLogger(FileStorageService.class);

    public String saveFile(MultipartFile file) throws IOException {

        File directory = new File(uploadDir);

        // create folder if not exists
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File destination = new File(directory, file.getOriginalFilename());
        log.info("Saving uploaded file: {}", file.getOriginalFilename());
        file.transferTo(destination);

        return destination.getAbsolutePath();
    }
}
