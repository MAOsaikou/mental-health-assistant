package com.itmao.aispringboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itmao.aispringboot.DTO.command.EmotionDiaryCreateDTO;
import com.itmao.aispringboot.DTO.query.EmotionDiaryQueryDTO;
import com.itmao.aispringboot.DTO.response.EmotionDiaryResponseDTO;
import com.itmao.aispringboot.common.Result;
import com.itmao.aispringboot.entity.EmotionDiary;
import com.itmao.aispringboot.service.EmotionDiaryService;
import com.itmao.aispringboot.util.JwtTokenUtil;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/emotion-diary")
public class EmotionDiaryController {

    private final EmotionDiaryService emotionDiaryService;

    public EmotionDiaryController(EmotionDiaryService emotionDiaryService) {
        this.emotionDiaryService = emotionDiaryService;
    }

    @PostMapping
    public Result<EmotionDiary> add(@Valid @RequestBody EmotionDiaryCreateDTO dto) {
        return Result.success(emotionDiaryService.add(JwtTokenUtil.getCurrentUserId(), dto));
    }

    @GetMapping("/mine")
    public Result<List<EmotionDiaryResponseDTO>> mine() {
        return Result.success(emotionDiaryService.listMine(JwtTokenUtil.getCurrentUserId()));
    }

    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> trend(@RequestParam(defaultValue = "7") int days) {
        return Result.success(emotionDiaryService.trend(JwtTokenUtil.getCurrentUserId(), days));
    }

    @GetMapping("/{id}")
    public Result<EmotionDiaryResponseDTO> mineDetail(@PathVariable Long id) {
        return Result.success(emotionDiaryService.getMine(JwtTokenUtil.getCurrentUserId(), id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/page")
    public Result<Page<Map<String, Object>>> adminPage(
            EmotionDiaryQueryDTO query,
            @RequestParam(defaultValue = "false") boolean reveal) {
        return Result.success(emotionDiaryService.adminPage(query, reveal));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/{id}")
    public Result<Map<String, Object>> adminDetail(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean reveal) {
        return Result.success(emotionDiaryService.adminDetail(id, reveal));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        emotionDiaryService.delete(id);
        return Result.success();
    }
}
