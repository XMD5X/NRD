package ru.support.adminpanel.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.support.adminpanel.entity.UploadedFile;
import ru.support.adminpanel.entity.UploadedFileType;
import ru.support.adminpanel.security.CurrentUserUtil;
import ru.support.adminpanel.service.UploadService;

import java.util.List;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping
    public UploadedFile upload(@RequestParam("file") MultipartFile file,
                                @RequestParam UploadedFileType fileType) {
        return uploadService.upload(file, fileType, CurrentUserUtil.get());
    }

    @GetMapping("/mine")
    public List<UploadedFile> mine() {
        return uploadService.myUploads(CurrentUserUtil.get().uuid());
    }
}
