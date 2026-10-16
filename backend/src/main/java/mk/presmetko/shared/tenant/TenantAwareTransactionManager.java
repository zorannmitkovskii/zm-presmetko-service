package mk.presmetko.shared.tenant;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.TransactionDefinition;

/**
 * Пред првиот SQL во секоја трансакција извршува {@code set_config('app.company_id', ?, true)}, што е исто како
 * {@code SET LOCAL app.company_id = ?}, но со параметар. Вредноста важи само до крајот на трансакцијата, па не
 * се пренесува на следниот корисник на конекцијата од pool-от.
 */
public class TenantAwareTransactionManager extends JdbcTransactionManager {

    private static final long serialVersionUID = 1L;

    static final String SET_COMPANY_SQL = "select set_config('app.company_id', ?, true)";

    public TenantAwareTransactionManager(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    protected void prepareTransactionalConnection(Connection con, TransactionDefinition definition)
            throws SQLException {
        super.prepareTransactionalConnection(con, definition);
        String companyId = TenantContext.currentCompanyId().map(UUID::toString).orElse("");
        try (PreparedStatement statement = con.prepareStatement(SET_COMPANY_SQL)) {
            statement.setString(1, companyId);
            statement.execute();
        }
    }
}
