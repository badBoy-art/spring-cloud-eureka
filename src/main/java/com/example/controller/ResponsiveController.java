package com.example.controller;

import com.example.response.BaseWebResponse;
import com.example.service.AddUserService;
import com.example.vo.User;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import rx.Single;
import rx.schedulers.Schedulers;

import javax.annotation.Resource;
import java.net.URI;

/**
 * rxjava 构建响应式api
 *
 * @author badBoy
 * @create 2019-09-12
 * @see <a href="https://github.com/axellageraldinc/reactive-web-api"></a>
 */
@Controller("res")
public class ResponsiveController {

    @Resource
    private AddUserService addUserService;

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Single<ResponseEntity<BaseWebResponse>> postUser(@RequestBody User user) {
        return addUserService.addUser(user)
                .subscribeOn(Schedulers.io())
                .map(s -> ResponseEntity.created(URI.create("/res/" + s)).body(BaseWebResponse.successNoData()));
    }


    @GetMapping(
            value = "/{sex}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Single<ResponseEntity<BaseWebResponse<BaseWebResponse>>> getBookDetail(@PathVariable(value = "sex") String sex) {
        return addUserService.getUserDetail(sex)
                .subscribeOn(Schedulers.io())
                .map(bookResponse -> ResponseEntity.ok(BaseWebResponse.successWithData(bookResponse)));
    }
}
