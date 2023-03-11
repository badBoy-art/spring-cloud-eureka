package com.example.springcloudeureka;

import com.alibaba.fastjson.JSON;
import com.example.configurer.ConditionConifg;
import com.example.serveice.ListService;
import com.example.serveice.Speakable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Locale;

@SpringBootApplication
@ComponentScan(basePackages = {"com.example.controller", "com.example.serveice", "com.example.netty", "com.example.mqtt"})
//@EnableDiscoveryClient
@EnableSwagger2
public class SpringCloudEurekaApplication {

    @Autowired
    private Speakable personSpring;

    public static void main(String[] args) {
        SpringApplication.run(SpringCloudEurekaApplication.class, args);

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ConditionConifg.class);

        ListService listService = context.getBean(ListService.class);

        System.out.println(context.getEnvironment().getProperty("os.name") + "系统下的列表命令为: " + listService.showListCmd());

        System.out.println(Locale.getDefault().getLanguage());
        context.close();

    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        Environment environment = ctx.getEnvironment();
        System.out.println("environment====" + JSON.toJSON(environment.getDefaultProfiles()));
        return args -> {
            // spring aop
            System.out.println("******** spring aop ******** ");
            personSpring.sayHi();
            personSpring.sayBye();
            //System.exit(0);
        };
    }
}
