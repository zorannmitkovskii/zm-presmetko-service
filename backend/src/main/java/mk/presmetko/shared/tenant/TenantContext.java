package mk.presmetko.shared.tenant;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Тековна компанија (tenant) за нишката. {@link TenantAwareTransactionManager} ја запишува во
 * {@code app.company_id} ({@code SET LOCAL}) на почеток на секоја трансакција, а RLS политиките во Postgres
 * филтрираат по неа.
 *
 * <p>Вредноста СЕКОГАШ доаѓа од проверено членство (web слој) или од {@code company_id} на задачата (worker,
 * {@link TenantJobRunner}), никогаш директно од барањето (header, параметар, тело).
 *
 * <p>Без поставен контекст RLS враќа 0 редови (fail-closed).
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_COMPANY = new ThreadLocal<>();

    private TenantContext() {}

    public static Optional<UUID> currentCompanyId() {
        return Optional.ofNullable(CURRENT_COMPANY.get());
    }

    /** Ја извршува акцијата со дадената компанија како контекст и потоа го враќа претходниот контекст. */
    public static <T> T callInCompany(UUID companyId, Supplier<T> action) {
        Objects.requireNonNull(companyId, "companyId");
        UUID previous = CURRENT_COMPANY.get();
        if (!companyId.equals(previous) && TransactionSynchronizationManager.isActualTransactionActive()) {
            // app.company_id е веќе поставен на почетокот на трансакцијата; промена тука би дала несогласен контекст.
            throw new IllegalStateException("Tenant context must be set before the transaction starts");
        }
        CURRENT_COMPANY.set(companyId);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                CURRENT_COMPANY.remove();
            } else {
                CURRENT_COMPANY.set(previous);
            }
        }
    }

    public static void runInCompany(UUID companyId, Runnable action) {
        callInCompany(companyId, () -> {
            action.run();
            return null;
        });
    }
}
