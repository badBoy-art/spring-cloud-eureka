package com.example.service;

import com.example.response.BaseWebResponse;
import com.example.vo.User;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import rx.Single;

import java.util.HashMap;
import java.util.Map;

/**
 * @author badBoy
 * @create 2019-09-12
 */
@Service
public class AddUserService {

    Map<String, User> map = new HashMap<>(3);

    public Single<String> addUser(User user) {
        return CoverUser(user);
    }

    private Single<String> CoverUser(User user) {
        return Single.create(singleSubscriber -> {
            if (StringUtils.isBlank(user.getUserName())) {
                singleSubscriber.onError(new Exception());
            } else {
                String sex = toAnotherUser(user).getSex();
                singleSubscriber.onSuccess(sex);
            }
        });
    }

    private User toAnotherUser(User user) {
        map.put(user.getSex(), user);
        return user;
    }

    public Single<BaseWebResponse> getUserDetail(String sex) {
        User user = map.get(sex);

        return Single.create(singleSubscriber -> {

            if (user == null) {
                singleSubscriber.onError(new Exception());
            } else {
                singleSubscriber.onSuccess(BaseWebResponse.successWithData(user));
            }
        });
    }

}
