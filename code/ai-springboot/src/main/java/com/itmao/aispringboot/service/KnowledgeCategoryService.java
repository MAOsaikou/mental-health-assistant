package com.itmao.aispringboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itmao.aispringboot.entity.KnowledgeCategory;
import com.itmao.aispringboot.mapper.KnowledgeCategoryMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgeCategoryService {
    @Resource
    private KnowledgeCategoryMapper knowledgeCategoryMapper;

    public List<KnowledgeCategory> tree() {
        LambdaQueryWrapper<KnowledgeCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeCategory::getStatus, 1)
                .orderByAsc(KnowledgeCategory::getSortOrder)
                .orderByAsc(KnowledgeCategory::getId);
        return knowledgeCategoryMapper.selectList(wrapper);
    }

    public KnowledgeCategory getById(Long id) {
        return knowledgeCategoryMapper.selectById(id);
    }
}
