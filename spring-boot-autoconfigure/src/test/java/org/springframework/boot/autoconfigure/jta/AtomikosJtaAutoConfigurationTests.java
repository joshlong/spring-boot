package org.springframework.boot.autoconfigure.jta;


import com.atomikos.jdbc.AtomikosDataSourceBean;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import javax.sql.XADataSource;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;

@Configuration
@EnableAutoConfiguration
@ComponentScan
public class AtomikosJtaAutoConfigurationTests {

    public static void main(String[] args) {
        SpringApplication.run(AtomikosJtaAutoConfigurationTests.class, args);
    }

    @Bean
    DataSource xaDataSource() {
        AtomikosDataSourceBean ds = new AtomikosDataSourceBean();
        ds.setUniqueResourceName("h2");
//        ds.setXaDataSource( dataSources[0]);
        ds.setXaDataSourceClassName("oracle.jdbc.xa.client.OracleXADataSource");
        Properties p = new Properties();
        p.setProperty("user", "java");
        p.setProperty("password", "java");
        p.setProperty("URL", "jdbc:oracle:thin:@localhost-xe:1521:XE");
        ds.setXaProperties(p);
        ds.setPoolSize(5);
        return ds;
    }

    @Bean
    CommandLineRunner init(final Map<String, XADataSource> xaDataSource) {
        return new CommandLineRunner() {
            @Override
            public void run(String... args) throws Exception {
                xaDataSource.forEach(new BiConsumer<String, XADataSource>() {
                    @Override
                    public void accept(String s, XADataSource xaDataSource) {
                        System.out.println(s + '=' + xaDataSource.getClass().getName());
                    }
                });

            }
        };
    }
}
