package br.gov.saude.sgpur.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Migracao leve de schema executada na subida do app, idempotente.
 *
 * <p>Motivo: o Hibernate 6, no H2, mapeia enums {@code @Enumerated(STRING)} para
 * o tipo nativo {@code ENUM(...)} da coluna. Com {@code ddl-auto=update} esse
 * tipo NAO e ampliado quando o enum Java ganha novos valores, entao bancos
 * criados antes da expansao passam a rejeitar valores novos (ex.: o status
 * {@code SOLICITADO} ou o anexo {@code COMPROVANTE_SNT}) com
 * "Value not permitted for column" ao salvar — quebrando o cadastro.
 *
 * <p>Correcao: converter toda coluna {@code ENUM} para {@code VARCHAR} (no H2)
 * e remover CHECK constraints de enum obsoletas (no PostgreSQL/Neon). Assim as
 * colunas passam a aceitar qualquer valor do enum atual ou futuro; a validade e
 * garantida no nivel da aplicacao pelo proprio enum Java. Cada bloco roda em
 * try/catch para nunca impedir a subida do app.
 */
@Component
@Order(1)
public class SchemaMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaMigration.class);

    private final JdbcTemplate jdbc;

    public SchemaMigration(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        converterEnumsParaVarcharH2();
        removerChecksDeEnumObsoletasPostgres();
    }

    /** H2: converte colunas de tipo nativo ENUM em VARCHAR(255). */
    private void converterEnumsParaVarcharH2() {
        try {
            List<Map<String, Object>> colunas = jdbc.queryForList(
                "SELECT TABLE_NAME, COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS "
                    + "WHERE DATA_TYPE = 'ENUM'");
            for (Map<String, Object> col : colunas) {
                String tabela = String.valueOf(col.get("TABLE_NAME"));
                String coluna = String.valueOf(col.get("COLUMN_NAME"));
                jdbc.execute("ALTER TABLE \"" + tabela + "\" ALTER COLUMN \"" + coluna
                    + "\" SET DATA TYPE VARCHAR(255)");
                log.warn("SchemaMigration: coluna {}.{} convertida de ENUM para VARCHAR(255).",
                    tabela, coluna);
            }
        } catch (Exception e) {
            // Em bancos sem o tipo ENUM (ex.: PostgreSQL) a consulta nao retorna nada;
            // qualquer outra falha apenas e registrada e nao impede a subida.
            log.debug("SchemaMigration: etapa H2 (ENUM->VARCHAR) ignorada: {}", e.getMessage());
        }
    }

    /**
     * PostgreSQL: o Hibernate cria CHECK constraints para enums STRING. Quando o
     * enum cresce, a constraint fica obsoleta. Remove as CHECK constraints das
     * tabelas de enum (cuja unica origem e o mapeamento de enum), exceto as de
     * NOT NULL. No H2 nao ha o que remover (nenhuma CHECK nessas tabelas).
     */
    private void removerChecksDeEnumObsoletasPostgres() {
        try {
            List<Map<String, Object>> checks = jdbc.queryForList(
                "SELECT tc.table_name, tc.constraint_name "
                    + "FROM information_schema.table_constraints tc "
                    + "WHERE tc.constraint_type = 'CHECK' "
                    + "  AND lower(tc.table_name) IN ('processo','anexo','parecer','usuario') "
                    + "  AND lower(tc.constraint_name) NOT LIKE '%not_null%'");
            for (Map<String, Object> ck : checks) {
                String tabela = String.valueOf(ck.get("table_name"));
                String constraint = String.valueOf(ck.get("constraint_name"));
                try {
                    jdbc.execute("ALTER TABLE " + tabela + " DROP CONSTRAINT \"" + constraint + "\"");
                    log.warn("SchemaMigration: removida CHECK constraint de enum {} em {}.",
                        constraint, tabela);
                } catch (Exception ignore) {
                    // constraint pode ter sido removida em execucao anterior
                }
            }
        } catch (Exception e) {
            log.debug("SchemaMigration: etapa PostgreSQL (drop CHECK) ignorada: {}", e.getMessage());
        }
    }
}
