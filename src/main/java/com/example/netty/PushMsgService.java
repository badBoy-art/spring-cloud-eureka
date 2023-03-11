package com.example.netty;

/**
 * @author: badBoy
 * @create: 2023-03-06 19:28
 * @Description:
 */
public interface PushMsgService {

    /**
     * 推送给指定用户
     */
    void pushMsgToOne(String userId, String msg);

    /**
     * 推送给所有用户
     */
    void pushMsgToAll(String msg);

}
