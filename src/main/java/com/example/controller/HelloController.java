package com.example.controller;

import com.alibaba.fastjson.JSON;
import com.example.vo.User;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * firstSpringBootController
 *
 * @author xuedui.zhao
 * @create 2018-07-12
 */
@RestController
public class HelloController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${helloworld}")
    private String str;

    @Value("https://blog.csdn.net/f641385712/article/details/91043955")
    private org.springframework.core.io.Resource resource;

    //http://127.0.0.1:8080/eurekaclient/hello/3

    @RequestMapping(value = "hello/{param}", method = RequestMethod.GET)
    @ApiOperation(value = "查询信息", notes = "根据param得到返回")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "param", value = "请求参数", required = true, dataType = "String", paramType = "path")})
    public ResponseEntity<String> hello(
            @PathVariable("param") String param) {
        //tag必须成对出现，也就是偶数个
        Counter counter = Counter.builder("counter").tag("counter", "counter").description("counter")
                .register(new SimpleMeterRegistry());
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
            System.out.println(resource.exists());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok(str + param);
    }

    @RequestMapping(value = "uploadUser", method = RequestMethod.POST)
    @ApiOperation(value = "上传用户信息", notes = "将上传信息以json形势返回")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "user", value = "请求参数", required = true, dataType = "User", paramType = "object")})
    public ResponseEntity<User> uploadUser(
            @RequestBody @Validated User user) {
        System.out.println(JSON.toJSONString(user));
        return ResponseEntity.ok(user);
    }

    @InitBinder("user")
    public void initBinder(WebDataBinder binder) {
        System.out.println("------binder");
        binder.addValidators(new ParamValidor());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setLenient(false);
        binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, true));
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