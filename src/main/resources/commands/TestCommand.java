package com.example.commands;

import org.crsh.cli.Command;
import org.crsh.cli.Usage;
import org.crsh.command.BaseCommand;
import org.crsh.command.InvocationContext;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

/**
 * @author xuedui.zhao
 * @create 2019-06-21
 */
@Usage("Test Command")
public class TestCommand extends BaseCommand {

    @Command
    @Usage("Useage main Command")
    public String main(InvocationContext context) {

        DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) context.getAttributes().get("spring.beanfactory");

        for (String name : defaultListableBeanFactory.getBeanDefinitionNames()) {
            System.out.println(name);
            context.getWriter().write(name);
            context.getWriter().write("\n");
        }

        return "main command";
    }

}
