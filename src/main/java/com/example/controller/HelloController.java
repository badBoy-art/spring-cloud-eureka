package com.example.controller;

import cn.hutool.core.map.MapUtil;
import com.alibaba.fastjson.JSON;
import com.example.common.ContextBeanEnum;
import com.example.configurer.MyConfig;
import com.example.param.ImportParam;
import com.example.resolver.IP;
import com.example.service.PrototypeService;
import com.example.service.RequestService;
import com.example.service.SameTypeService;
import com.example.service.SessionService;
import com.example.service.Speak;
import com.example.service.Speakable;
import com.example.service.WelcomeUtil;
import com.example.util.BeanUtils;
import com.example.util.SpringContextHolder;
import com.example.vo.User;
import com.google.common.collect.ImmutableMap;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.units.qual.A;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * firstSpringBootController
 *
 * @author xuedui.zhao
 * @create 2018-07-12
 */
//@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RestController
public class HelloController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${helloworld}")
    private String str;

    @Value("https://blog.csdn.net/f641385712/article/details/91043955")
    private org.springframework.core.io.Resource resource;
    @Autowired
    private SameTypeService typeServiceOne;
    @Autowired
    private SameTypeService typeServiceTwo;
    @Value("${project.name:${spring.application.name:}}")
    private String projectName;
    @Value("${project.name}")
    private String projectName1;
    @Value("${spring.application.name}")
    private String applicationName;
    @Autowired
    private MyConfig myConfig;
    @Autowired
    private RequestService requestService;
    @Autowired
    private SessionService sessionService;
    @Autowired
    private PrototypeService prototypeService;

    //http://127.0.0.1:8080/eurekaclient/hello/3
    private volatile ImmutableMap<String, String> context = null;


    @RequestMapping(value = "hello/{param}/{param2}", method = RequestMethod.GET)
    public ResponseEntity<String> hello(@NotNull HttpServletRequest request, @PathVariable("param") String param, @PathVariable("param2") String param2,
                                        @RequestParam(required = false) String param3, @IP String ip) {
        Map<String, String[]> paramMap = request.getParameterMap();
        Object obj = request.getAttribute("org.springframework.web.servlet.HandlerMapping.uriTemplateVariables");
        //tag必须成对出现，也就是偶数个
        Counter counter = Counter.builder("counter").tag("counter", "counter").description("counter").register(new SimpleMeterRegistry());
        counter.increment();
        counter.increment(2D);
        System.out.println(counter.count());
        System.out.println(counter.measure());
        //全局静态方法
        Metrics.addRegistry(new SimpleMeterRegistry());
        counter = Metrics.counter("counter", "counter", "counter");
        counter.increment(10086D);
        counter.increment(10087D);

        System.out.println(counter.count());
        System.out.println(counter.measure());

        try {
            Speak speak = ContextBeanEnum.SPEAKABLE.apply();
            Speakable speakable = (Speakable) speak;
            speakable.sayHi();
            System.out.println(resource.exists());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok(request.getRequestURI() + str + param + param2 + param3 + ip + WelcomeUtil.getS() +
                " JavaCharset: " + BeanUtils.getDefaultJavaCharset() + " MIMECharset: " + BeanUtils.getDefaultMIMECharset() + "   " + " " +
                projectName + getMap()
                + myConfig.getMap() + "\r\n"
                + "context-bean " + SpringContextHolder.getBean(RequestService.class).getRequestScope()
                + " Autowired " + requestService.getRequestScope() + "\r\n"
                + "context-bean " + SpringContextHolder.getBean(SessionService.class).getRequestScope()
                + " Autowired " + sessionService.getRequestScope() + "\r\n"
                + "context-bean " + SpringContextHolder.getBean(PrototypeService.class).getRequestScope()
                + " Autowired " + prototypeService.getRequestScope());
    }

    private ImmutableMap<String, String> getMap() {
        if (MapUtil.isNotEmpty(context)) {
            return context;
        }
        synchronized (this) {
            context = ImmutableMap.of("key", applicationName);
        }
        return context;
    }

    @RequestMapping(value = "hello2", method = RequestMethod.GET)
    public Long hello2(@NotNull HttpServletRequest request) {
        return 1723846293258240009L;
    }

    @RequestMapping(value = "uploadUser", method = RequestMethod.POST)
    public ResponseEntity<User> uploadUser(HttpServletRequest request, @RequestBody @Validated User user) {
        String queryStr = request.getQueryString();
        System.out.println(JSON.toJSONString(user));
        return ResponseEntity.ok(user);
    }

    @RequestMapping(value = "uploadUser2", method = RequestMethod.POST)
    public String uploadUser2(HttpServletRequest request, @RequestParam(required = false) String param) {
        String body = null;
        try {
            body = getPostJsonBody(request);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String queryStr = request.getQueryString();
        return queryStr + " body = " + body + " age = " + request.getParameter("age");
    }

    private String getPostJsonBody(HttpServletRequest httpServletRequest) throws IOException {
        ContentCachingRequestWrapper requestWrapper;
        if (httpServletRequest instanceof ContentCachingRequestWrapper) {
            requestWrapper = (ContentCachingRequestWrapper) httpServletRequest;
        } else {
            requestWrapper = new ContentCachingRequestWrapper(httpServletRequest);
        }
        String body2 = new String(requestWrapper.getContentAsByteArray());
        if (StringUtils.isBlank(body2)) {
            String characterEncoding = httpServletRequest.getCharacterEncoding();
            if (StringUtils.isBlank(characterEncoding)) {
                characterEncoding = httpServletRequest.getHeader("characterEncoding");
            }
            body2 = IOUtils.toString(requestWrapper.getInputStream(), characterEncoding);
        }
        return body2;
    }

    @InitBinder("user")
    public void initBinder(WebDataBinder binder) {
        System.out.println("------binder");
        binder.addValidators(new ParamValidor());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setLenient(false);
        binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, true));
    }

    @PostMapping("/uploadFile")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, @RequestParam("description") String description) {

        if (!file.isEmpty()) {
            try {
                // 获取文件的字节
                byte[] bytes = file.getBytes();
                // 保存文件到服务器的某个目录
                System.out.println("File size: " + bytes.length);
            } catch (IOException e) {
                return "Failed to save the file: " + e.getMessage();
            }
        } else {
            return "Please upload a file.";
        }

        return "uploadStatus";
    }

    @PostMapping("/externalData")
    public String externalData(ImportParam importParam) {
        return JSON.toJSONString(importParam);
    }

    @GetMapping("/download-zip")
    public void downloadZipFile(HttpServletResponse response) throws IOException {
        // 设置响应头
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"example.zip\"");
        response.setHeader("Content-Transfer-Encoding", "binary");

        // 创建ZIP文件
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // 添加文件到ZIP
            String fileName = "example.txt";
            ZipEntry zipEntry = new ZipEntry(fileName);
            zos.putNextEntry(zipEntry);
            zos.write("Hello, World!".getBytes());
            zos.closeEntry();
        }

        // 将ZIP文件写入响应输出流
        response.getOutputStream().write(baos.toByteArray());
        response.getOutputStream().flush();
    }
}

class ParamValidor implements Validator {

    @Override
    public boolean supports(Class<?> aClass) {
        return User.class.equals(aClass);
    }

    @Override
    public void validate(Object target, Errors errors) {
        User user = (User) target;
        if (user.getAge() < 10) {
            errors.rejectValue("age", "age < 10");
        }
    }
}