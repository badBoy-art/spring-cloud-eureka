package com.example.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

/**
 * @author xuedui.zhao
 * @create 2019-06-21
 */
@Setter
@Getter
@ToString
public class User {

    private String userName;
    private String sex;
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date birthday;
    private Hobby hobby;
    private Integer age;

}
