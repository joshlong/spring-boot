package sample.atomikos;

 import com.atomikos.jdbc.AtomikosDataSourceBean;
 import com.atomikos.jms.AtomikosConnectionFactoryBean;
import com.atomikos.jms.extra.MessageDrivenContainer;
 import org.slf4j.LoggerFactory;
 import org.springframework.boot.CommandLineRunner;
 import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

 import javax.jms.Message;
 import javax.jms.MessageListener;
 import javax.sql.DataSource;
 import javax.sql.XADataSource;
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
        SpringApplication.run(SampleAtomikosApplication .class, args);
    }


    @Bean
    MessageDrivenContainer messageDrivenContainer(AtomikosConnectionFactoryBean connectionFactoryBean) {
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



    @Bean(initMethod = "init", destroyMethod = "close")
    DataSource xaDataSource() {
        Properties properties = new Properties();
        properties.setProperty("ServerName", "127.0.0.1");
        properties.setProperty("PortNumber", "5432");
        properties.setProperty("DatabaseName", "crm");
        properties.setProperty("User", "crm");
        properties.setProperty("Password", "crm");

        AtomikosDataSourceBean xaDS = new AtomikosDataSourceBean();
        xaDS.setUniqueResourceName("xaDataSource");
        xaDS.setTestQuery("select now()");
        xaDS.setXaProperties(properties);
      ///  xaDS.setXaDataSourceClassName(Driv);
        xaDS.setMaxPoolSize(20);
        xaDS.setMinPoolSize(10);

        return xaDS;
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
