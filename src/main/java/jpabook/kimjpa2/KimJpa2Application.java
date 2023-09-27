package jpabook.kimjpa2;

import com.fasterxml.jackson.datatype.hibernate5.jakarta.Hibernate5JakartaModule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class KimJpa2Application {

    public static void main(String[] args) {
        SpringApplication.run(KimJpa2Application.class, args);
    }

    @Bean
    Hibernate5JakartaModule hibernate5JakartaModule() {
        // 기본적으로 초기화 된 프록시 객체만 노출
        Hibernate5JakartaModule hibernate5JakartaModule = new Hibernate5JakartaModule();
        // V1. 강제 지연 로딩 설정
        // hibernate5JakartaModule.configure(Hibernate5JakartaModule.Feature.FORCE_LAZY_LOADING, true);
        return hibernate5JakartaModule;
    }
}
