package com.example.springcloudeureka;

import com.alibaba.fastjson.JSON;
import com.example.configurer.ConditionConifg;
import com.example.configurer.JedisCacheConfig;
import com.example.configurer.LifecycleConfig;
import com.example.service.ListService;
import com.example.service.Speakable;
import com.example.service.impl.MessageDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Locale;

@ComponentScan(basePackages = {"com.example.controller",
        "com.example.service",
        "com.example.netty",
        "com.example.mqtt",
        "com.example.util",
        "com.example.configurer",
        "com.example.config",
        "com.example.aop",
        "com.example.debezium"})
//@EnableDiscoveryClient
@EnableSwagger2
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class})
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

        AnnotationConfigApplicationContext applicationContext =
                new AnnotationConfigApplicationContext(LifecycleConfig.class);
        applicationContext.start();
        applicationContext.stop();
        applicationContext.close();

        StringRedisTemplate template = ctx.getBean(StringRedisTemplate.class);
        AnnotationConfigApplicationContext delegateContext =
                new AnnotationConfigApplicationContext(JedisCacheConfig.class);
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

    /**
     * SpringBootServletInitializer 项目打包成一个war包丢入tomcat中
     *
     * @param application
     * @return
     */
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(SpringCloudEurekaApplication.class);
    }

    @Bean
    public ConfigurableServletWebServerFactory webServerFactory() {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        factory.addConnectorCustomizers(connector -> connector.setProperty("relaxedQueryChars", "|{}[]\\"));
        return factory;
    }
}
