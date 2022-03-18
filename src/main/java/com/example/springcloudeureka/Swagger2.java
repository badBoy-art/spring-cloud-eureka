package com.example.springcloudeureka;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

/**
 * 文档描述
 *
 * @author xuedui.zhao
 * @create 2018-07-13
 */
@Component
public class Swagger2 {
      @Bean
      public Docket createRestApi() {
            return new Docket(DocumentationType.SWAGGER_2)
                           .apiInfo(apiInfo())
                           .select()
                           .apis(RequestHandlerSelectors.basePackage("com.example.controller"))
                           .paths(PathSelectors.any())
                           .build();
      }
      
      private ApiInfo apiInfo() {
            return new ApiInfoBuilder()
                           .title("springboot利用swagger构建api文档")
                           .description("简单优雅的restful风格，")
                           .termsOfServiceUrl("http://blog.csdn.net/saytime")
                           .version("1.0")
                           .build();
      }
}
