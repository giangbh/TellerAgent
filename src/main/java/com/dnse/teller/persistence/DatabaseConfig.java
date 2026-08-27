package com.dnse.teller.persistence;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.io.File;

@Configuration
public class DatabaseConfig {

    private static final String DB_DIR = "data";
    private static final String DB_FILE = "data/teller_workflows.db";

    @Bean
    public DataSource dataSource() {
        File dir = new File(DB_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl("jdbc:sqlite:" + DB_FILE);
        return dataSource;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        initializeSchema(jdbcTemplate);
        return jdbcTemplate;
    }

    private void initializeSchema(JdbcTemplate jdbcTemplate) {
        // 1. Sessions table
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS sessions (
                session_id TEXT PRIMARY KEY,
                branch_id TEXT,
                counter_id TEXT,
                teller_id TEXT,
                customer_ref TEXT,
                workflow TEXT,
                status TEXT,
                revision INTEGER,
                data_json TEXT,
                created_at TEXT,
                updated_at TEXT
            );
        """);

        // 2. Workflow Executions table (Temporal-style)
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS workflow_executions (
                execution_id TEXT PRIMARY KEY,
                session_id TEXT,
                workflow_name TEXT,
                status TEXT,
                start_time TEXT,
                close_time TEXT,
                state_json TEXT
            );
        """);

        // 3. Workflow History Events (Event Sourcing & Replay)
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS workflow_history_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                execution_id TEXT,
                event_type TEXT,
                event_data TEXT,
                created_at TEXT
            );
        """);

        // 4. Immutable Audit Logs & Idempotency Ledger
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS audit_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                trace_id TEXT,
                caller TEXT,
                capability_id TEXT,
                idempotency_key TEXT,
                result_status TEXT,
                payload_json TEXT,
                created_at TEXT
            );
        """);

        // 5. Dynamic MCP Security Policies (Externalized RBAC Table)
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS mcp_security_policies (
                capability_id TEXT PRIMARY KEY,
                risk_level TEXT,
                is_side_effect INTEGER,
                requires_idempotency INTEGER,
                allowed_callers TEXT,
                allowed_workflows TEXT,
                updated_at TEXT
            );
        """);

        // 6. Dynamic Mock Customers Table
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS mock_customers (
                cif TEXT PRIMARY KEY,
                display_name TEXT,
                kyc_status TEXT,
                segment TEXT,
                risk_rating TEXT,
                identity_masked TEXT,
                created_at TEXT
            );
        """);

        // 7. Dynamic Mock Accounts Table
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS mock_accounts (
                account_number TEXT PRIMARY KEY,
                cif TEXT,
                account_ref TEXT,
                currency TEXT,
                available_balance INTEGER,
                account_type TEXT,
                term_months INTEGER,
                interest_rate REAL,
                status TEXT
            );
        """);
    }
}
