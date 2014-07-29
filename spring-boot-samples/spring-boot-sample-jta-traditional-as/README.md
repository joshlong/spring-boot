# Deployment

This application is intended to run inside of a Java EE application server like JBoss' Wildfly. It demonstrates
Spring Boot's auto-configuration defaulting to the container-managed `TransactionManager`. This example
unfortunately requires a fully configured Wildfly installation. You'll need to configure a PostgreSQL XA connection and an
ActiveMQ XA connection factory in the Java EE application server's JNDI machinery. This configuration usually lives
inside of an `.xml` configuration file inside the application server itself. This is my attempt to describe the
copy-and-paste surgery required to get that machinery working with this application.

The `$JBOSS_HOME/standalone/configuration/standalone-full.xml` file:

You need to register a PostgreSQL XA `Driver` and then configure an `xa-datasource`.

Here's a complete listing of the `xa-datasource` contribution to the `datasources` element, and the `driver` contribution
to the `drivers` element to configure a PostgreSQL DB connection to localhost.
[You can learn more from the documentation](https://access.redhat.com/documentation/en-US/JBoss_Enterprise_Application_Platform/6/html-single/Administration_and_Configuration_Guide/index.html#Install_a_JDBC_Driver_with_the_Management_Console)

```
            <datasources>
                ...
                <xa-datasource jndi-name="java:jboss/datasources/CrmXADS" pool-name="CrmXADS" enabled="true">
                    <xa-datasource-property name="url">
                        jdbc:postgresql://localhost:5432/crm
                    </xa-datasource-property>
                    <driver>postgres</driver>
                    <xa-pool>
                        <min-pool-size>10</min-pool-size>
                        <max-pool-size>20</max-pool-size>
                        <prefill>true</prefill>
                    </xa-pool>
                    <security>
                        <user-name>crm</user-name>
                        <password>crm</password>
                    </security>
                </xa-datasource>
                <drivers>
                    ...
                    <driver name="postgres" module="org.postgres">
                        <xa-datasource-class>org.postgresql.xa.PGXADataSource</xa-datasource-class>
                    </driver>
                </drivers>
            </datasources>
            ...
```

I also configured a `javax.jms.Destination` on my Wildfly installation by contributing the following to the `hornetq-server` element in `standalone-full.xml`:

```
    <jms-destinations>

        <jms-queue name="accounts">
            <entry name="java:/jms/queue/accounts"/>
        </jms-queue>

        ...

    </jms-destinations>
    ...
</hornetq-server>

```

Run Wildfly with the following command: `$JBOSS_HOME/bin/standalone.sh -c standalone-full.xml`.