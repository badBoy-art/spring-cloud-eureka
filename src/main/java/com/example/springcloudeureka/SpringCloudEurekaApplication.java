package com.example.springcloudeureka;

import com.alibaba.fastjson.JSON;
import com.example.configurer.ConditionConifg;
import com.example.configurer.JedisCacheConfig;
import com.example.serveice.ListService;
import com.example.serveice.Speakable;
import com.example.serveice.impl.MessageDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Locale;

@SpringBootApplication
@ComponentScan(basePackages = {"com.example.controller", "com.example.serveice", "com.example.netty", "com.example.mqtt"})
//@EnableDiscoveryClient
@EnableSwagger2
public class SpringCloudEurekaApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringCloudEurekaApplication.class);

    @Autowired
    private Speakable personSpring;

    public static void main(String[] args) throws InterruptedException {
        ApplicationContext ctx = SpringApplication.run(SpringCloudEurekaApplication.class, args);

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ConditionConifg.class);

        ListService listService = context.getBean(ListService.class);

        System.out.println(context.getEnvironment().getProperty("os.name") + "系统下的列表命令为: " + listService.showListCmd());

        System.out.println(Locale.getDefault().getLanguage());
        context.close();

        StringRedisTemplate template = ctx.getBean(StringRedisTemplate.class);
        AnnotationConfigApplicationContext delegateContext = new AnnotationConfigApplicationContext(JedisCacheConfig.class);
        MessageDelegate delegate = delegateContext.getBean(MessageDelegate.class);

        while (delegate.getCount() == 0) {
            LOGGER.info("Sending message...");
            template.convertAndSend("chat", "Hello from Redis!");
            Thread.sleep(500L);
        }
        delegateContext.close();
        // System.exit(0); //终止程序
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
