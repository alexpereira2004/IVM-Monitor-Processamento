package br.com.lunacom.ivmmonitorprocessamento;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "br.com.lunacom.ivmmonitorprocessamento",
        "br.com.lunacom.comum"
})
public class IvmMonitorProcessamentoApplication {

    public static void main(String[] args) {
        SpringApplication.run(IvmMonitorProcessamentoApplication.class, args);
    }

}
