package sample.atomikos;

import com.atomikos.jdbc.AtomikosDataSourceBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import javax.sql.XADataSource;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;

/**
 * Demonstrates how to use Atomikos and JTA
 *
 * @author Josh Long
 */
@Configuration
@ComponentScan
@EnableAutoConfiguration
public class SampleAtomikosApplication {

    public static void main(String[] args) {
        SpringApplication.run(SampleAtomikosApplication.class, args);
    }

    /*
    @Bean
    public MessageDrivenContainer messageDrivenContainer(AtomikosConnectionFactoryBean connectionFactoryBean) {
        MessageDrivenContainer mdc = new MessageDrivenContainer();
        mdc.setAtomikosConnectionFactoryBean(connectionFactoryBean);
        mdc.setMessageListener(new MessageListener() {
            @Override
            public void onMessage(Message message) {
                LoggerFactory.getLogger(getClass().getName()).info("Received: " + message.toString());
            }
        });
        return mdc;
    }
    */

    @Bean(initMethod = "init", destroyMethod = "close")
    public AtomikosDataSourceBean xaDataSource() throws UnknownHostException {
        Properties properties = new Properties();
        properties.setProperty("ServerName", "127.0.0.1");
        properties.setProperty("DatabaseName", "crm");
        properties.setProperty("User", "crm");
        properties.setProperty("Password", "crm");

        AtomikosDataSourceBean xaDS = new AtomikosDataSourceBean();
        xaDS.setUniqueResourceName("xaDataSource");
        xaDS.setTestQuery("select now()");
        xaDS.setXaProperties(properties);
        Class<? extends XADataSource> aClass = org.postgresql.xa.PGXADataSource.class;
        xaDS.setXaDataSourceClassName(aClass.getName());
        xaDS.setMaxPoolSize(20);
        xaDS.setMinPoolSize(10);
        return xaDS;
    }


    @Bean
    public CommandLineRunner init(
            final JdbcTemplate jdbcTemplate,
            final Map<String, DataSource> xaDataSource) {
        return new CommandLineRunner() {
            @Override
            public void run(String... args) throws Exception {

                log().info("jdbcTemplate: " + jdbcTemplate.toString());

                xaDataSource.forEach(new BiConsumer<String, DataSource>() {
                    @Override
                    public void accept(String s, DataSource xaDataSource) {
                        log().info(s + '=' + xaDataSource.getClass().getName());
                    }
                });

            }
        };
    }

    private static Logger log() {
        return LoggerFactory.getLogger(SampleAtomikosApplication.class);
    }
}
