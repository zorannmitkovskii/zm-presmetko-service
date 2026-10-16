package mk.presmetko.shared.tenant;

import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Помошник за worker задачи: го поставува tenant контекстот од {@code company_id} на задачата и ја извршува
 * работата во нова трансакција.
 *
 * <pre>{@code
 * jobRunner.inCompany(job.companyId(), tx -> sender.send(job.invoiceId()));
 * }</pre>
 */
@Component
public class TenantJobRunner {

    private final TransactionTemplate transactionTemplate;

    public TenantJobRunner(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public <T> T inCompany(UUID jobCompanyId, TransactionCallback<T> work) {
        return TenantContext.callInCompany(jobCompanyId, () -> transactionTemplate.execute(work));
    }
}
