package com.example.serveice.impl;

import com.example.serveice.ListService;

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
