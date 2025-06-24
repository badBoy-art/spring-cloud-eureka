/*
package com.example.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import java.io.File;
import java.io.IOException;
import java.util.List;

*/
/**
 * @author: badBoy
 * @create: 2024-11-13 10:23
 * @Description:
 *//*

@WebServlet("/upload")
@MultipartConfig
public class FileUploadServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 检查请求是否是多部分的
        if (ServletFileUpload.isMultipartContent(request)) {
            // 创建一个DiskFileItemFactory实例
            DiskFileItemFactory factory = new DiskFileItemFactory();
            // 创建一个ServletFileUpload实例
            ServletFileUpload upload = new ServletFileUpload(factory);
            // 解析请求的内容提取文件数据
            try {
                // 使用ServletFileUpload.parseRequest()解析请求
                List<FileItem> formItems = upload.parseRequest(request);
                // 用来存储上传文件的路径
                String filePath = "uploads/";
                // 用来存储表单字段
                String description = null;
                // 遍历表单数据
                for (FileItem item : formItems) {
                    // 处理不在表单中的字段
                    if (!item.isFormField()) {
                        String fileName = new File(item.getName()).getName();
                        File storeFile = new File(filePath + fileName);
                        // 在控制台输出文件的上传路径
                        System.out.println("Uploaded file: " + storeFile.getAbsolutePath());
                        // 保存文件到硬盘
                        item.write(storeFile);
                    } else {
                        // 处理表单字段
                        String fieldName = item.getFieldName();
                        String fieldValue = item.getString();
                        if ("description".equals(fieldName)) {
                            description = fieldValue;
                        }
                    }
                }
                // 在这里可以添加对文件的处理和对description的使用
                response.getWriter().println("File is uploaded and description is: " + description);
            } catch (Exception ex) {
                throw new ServletException("Error in file upload", ex);
            }
        } else {
            response.getWriter().println("Sorry, this is not a multipart request");
        }
    }
}
*/
