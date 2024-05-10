package com.example.configurer;

import com.example.condition.LinuxCondition;
import com.example.condition.MacCondition;
import com.example.condition.WindowsCondition;
import com.example.service.ListService;
import com.example.service.impl.LinuxListServiceImpl;
import com.example.service.impl.MacListServiceImpl;
import com.example.service.impl.WindowsListServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * @author xuedui.zhao
 * @create 2019-08-22
 */
@Configuration
public class ConditionConifg {

    @Bean
    @Conditional(WindowsCondition.class) //①
    public ListService windowsListService() {
        return new WindowsListServiceImpl();
    }

    @Bean
    @Conditional(LinuxCondition.class) //②
    public ListService linuxListService() {
        return new LinuxListServiceImpl();
    }

    @Bean
    @Conditional(MacCondition.class) //②
    public ListService macListService() {
        return new MacListServiceImpl();
    }

//    @Bean
//    public ListService myService() {
//        return new MyListServiceImpl();
//    }

}
