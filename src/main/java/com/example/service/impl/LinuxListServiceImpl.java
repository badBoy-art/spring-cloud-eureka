package com.example.service.impl;

import com.example.service.ListService;

/**
 * @author xuedui.zhao
 * @create 2019-08-22
 */
public class LinuxListServiceImpl implements ListService {

    @Override
    public String showListCmd() {
        return "ls";
    }
}
