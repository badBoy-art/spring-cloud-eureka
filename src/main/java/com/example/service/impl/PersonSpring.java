package com.example.service.impl;

import com.example.service.Speakable;
import org.springframework.stereotype.Service;

/**
 * @author xuedui.zhao
 * @create 2018-12-27
 */
@Service
public class PersonSpring implements Speakable {

    @Override
    public void sayHi() {

        try {
            Thread.sleep(30);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println("Hi!!");
    }

    @Override
    public void sayBye() {

        try {
            Thread.sleep(10);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println("Bye!!");
    }
}
