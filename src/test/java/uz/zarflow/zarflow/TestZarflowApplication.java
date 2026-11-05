package uz.zarflow.zarflow;

import org.springframework.boot.SpringApplication;

public class TestZarflowApplication {

    public static void main(String[] args) {
        SpringApplication.from(ZarflowApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
