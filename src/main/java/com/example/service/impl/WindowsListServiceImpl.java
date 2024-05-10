package com.example.service.impl;

import com.example.service.ListService;

/**
 * @author xuedui.zhao
 * @create 2019-08-22
 */
public class WindowsListServiceImpl implements ListService {
    @Override
    public String showListCmd() {
        return "dir";
    }
}
