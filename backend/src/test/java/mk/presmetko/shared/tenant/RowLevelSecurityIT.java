package mk.presmetko.shared.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import mk.presmetko.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Тест-шаблон за RLS: со контекст на компанија А, редовите на компанија Б не се видливи. */
class RowLevelSecurityIT extends PostgresIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    TenantJobRunner jobRunner;

    TransactionTemplate tx;
    UUID companyA;
    UUID companyB;

    @BeforeEach
    void seedTwoCompanies() {
        tx = new TransactionTemplate(transactionManager);
        companyA = createCompanyWithMember();
        companyB = createCompanyWithMember();
    }

    @Test
    void runtimeRoleCannotBypassRls() {
        Boolean bypass = jdbc.queryForObject(
                "select rolsuper or rolbypassrls from pg_roles where rolname = current_user", Boolean.class);
        assertThat(bypass).isFalse();
    }

    @Test
    void companyAContextDoesNotSeeCompanyBRows() {
        TenantContext.runInCompany(
                companyA,
                () -> tx.executeWithoutResult(s -> {
                    assertThat(count("select count(*) from companies where id = ?", companyB))
                            .isZero();
                    assertThat(count("select count(*) from memberships where company_id = ?", companyB))
                            .isZero();
                    assertThat(count("select count(*) from companies where id = ?", companyA))
                            .isOne();
                    assertThat(count("select count(*) from memberships", null)).isOne();
                }));
    }

    @Test
    void noContextSeesNothing() {
        tx.executeWithoutResult(
                s -> assertThat(count("select count(*) from companies", null)).isZero());
    }

    @Test
    void cannotWriteRowForAnotherCompany() {
        UUID user = createUser();
        assertThatThrownBy(() -> TenantContext.runInCompany(
                        companyA,
                        () -> tx.executeWithoutResult(s -> jdbc.update(
                                "insert into memberships (company_id, user_id, role) values (?, ?, 'MEMBER')",
                                companyB,
                                user))))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("row-level security");
    }

    @Test
    void workerHelperSetsContextFromJobCompany() {
        Long visible = jobRunner.inCompany(companyB, s -> count("select count(*) from companies", null));
        UUID seen = jobRunner.inCompany(companyB, s -> jdbc.queryForObject("select id from companies", UUID.class));

        assertThat(visible).isOne();
        assertThat(seen).isEqualTo(companyB);
        assertThat(TenantContext.currentCompanyId()).isEmpty();
    }

    @Test
    void contextCannotChangeInsideTransaction() {
        TenantContext.runInCompany(
                companyA,
                () -> tx.executeWithoutResult(
                        s -> assertThatThrownBy(() -> TenantContext.runInCompany(companyB, () -> {}))
                                .isInstanceOf(IllegalStateException.class)));
    }

    private UUID createCompanyWithMember() {
        UUID company = UUID.randomUUID();
        UUID user = createUser();
        TenantContext.runInCompany(
                company,
                () -> tx.executeWithoutResult(s -> {
                    jdbc.update(
                            "insert into companies (id, name, edb) values (?, ?, ?)",
                            company,
                            "Тест " + company,
                            company.toString().replace("-", "").substring(0, 13));
                    jdbc.update(
                            "insert into memberships (company_id, user_id, role) values (?, ?, 'OWNER')",
                            company,
                            user);
                }));
        return company;
    }

    private UUID createUser() {
        UUID user = UUID.randomUUID();
        tx.executeWithoutResult(s -> jdbc.update(
                "insert into users (id, idp_subject, email, display_name) values (?, ?, ?, ?)",
                user,
                user.toString(),
                user + "@presmetko.test",
                "Тест корисник"));
        return user;
    }

    private Long count(String sql, UUID arg) {
        return arg == null ? jdbc.queryForObject(sql, Long.class) : jdbc.queryForObject(sql, Long.class, arg);
    }
}
