package com.example.springcloudeureka;

import com.example.controller.HelloController;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
// properties：工程里的 spring-ai-alibaba 自动配置要求必须有 api-key，否则上下文起不来；
// 本测试不解码上下文，给占位值即可（要用真实 DashScope 时删掉这行、由环境变量传入）
@SpringBootTest(properties = "spring.ai.dashscope.api-key=sk-local-placeholder")
@WebAppConfiguration
public class SpringCloudEurekaApplicationTests {

  private MockMvc mockMvc;

  @Test
  public void contextLoads() {}

  @Before
  public void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new HelloController()).build();
  }

  @Test
  public void testHello() throws Exception {
    // 说明：HelloController 并没有 "/hello" 这个映射（真实映射是 hello/{param}/{param2}，
    // 返回的是一长串拼接内容，且依赖 @Value/@Autowired 注入的字段），
    // standaloneSetup 下这些字段为 null，注定 NPE；因此这里改为断言确实存在且无外部依赖的 /hello2。
    mockMvc
        .perform(MockMvcRequestBuilders.get("/hello2").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().string("1723846293258240009"));
  }
}
