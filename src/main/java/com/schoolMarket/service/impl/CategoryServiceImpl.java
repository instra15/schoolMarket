package com.schoolMarket.service.impl;

import com.schoolMarket.mapper.CategoryMapper;
import com.schoolMarket.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;



}
