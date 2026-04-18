package za.co.urbaneye.reporthole;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ReportholeBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReportholeBeApplication.class, args);
    }

}
