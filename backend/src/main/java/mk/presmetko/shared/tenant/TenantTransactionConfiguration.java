package mk.presmetko.shared.tenant;

import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Го заменува стандардниот JDBC transaction manager на Spring Boot со {@link TenantAwareTransactionManager}. */
@Configuration(proxyBeanMethods = false)
class TenantTransactionConfiguration {

    @Bean
    TenantAwareTransactionManager transactionManager(
            DataSource dataSource, ObjectProvider<TransactionManagerCustomizers> customizers) {
        TenantAwareTransactionManager transactionManager = new TenantAwareTransactionManager(dataSource);
        customizers.ifAvailable(c -> c.customize(transactionManager));
        return transactionManager;
    }
}
