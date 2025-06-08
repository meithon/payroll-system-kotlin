package org.example.db

import PayMethodType
import SalaryType
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDate
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.transactions.transaction

fun salaryTypeToEnum(salaryType: SalaryType): SalaryTypeEnum =
    when (salaryType) {
        is SalaryType.Hourly -> SalaryTypeEnum.Hourly
        is SalaryType.Monthly -> SalaryTypeEnum.Monthly
        is SalaryType.Commission -> SalaryTypeEnum.Commission
    }

enum class SalaryTypeEnum {
    Hourly,
    Monthly,
    Commission,
}

object EmployeesTable : Table("employees") {
    val id: Column<Int> = integer("id").autoIncrement()
    val name: Column<String> = varchar("name", 255)
    val address = varchar("address", 255)
    val salaryType = enumerationByName("salaryType", 20, SalaryTypeEnum::class)
    val hourlyWage = double("hourlyWage").nullable()
    val monthlySalary = double("monthlySalary").nullable()
    val monthlyRate = double("monthlyRate").nullable()
    val commissionRate = double("commissionRate").nullable()
    val payMethod = enumerationByName("payMethod", 20, PayMethodType::class).nullable()
    val mail = varchar("mail", 255).nullable()
    val isHold = bool("hold").default(false)

    override val primaryKey = PrimaryKey(id)
}

object MembersTable : Table("members") {
    val id: Column<Int> = integer("id").autoIncrement()
    val employeeId: Column<Int> = integer("employeeId")
    val dues: Column<Double> = double("dues")

    override val primaryKey = PrimaryKey(id)
}

object TimeRecordsTable : Table("timerecords") {
    val id: Column<Int> = integer("id").autoIncrement()
    val employeeId: Column<Int> = integer("employeeId")
    val date: Column<LocalDate> = date("date").defaultExpression(CurrentDate)
    val hours: Column<Double> = double("hours")

    override val primaryKey = PrimaryKey(id)
}

object SalesReceiptsTable : Table("salesreceipts") {
    val id: Column<Int> = integer("id").autoIncrement()
    val employeeId: Column<Int> = integer("employeeId")
    val date: Column<LocalDate> = date("date")
    val amount: Column<Double> = double("amount")

    override val primaryKey = PrimaryKey(id)
}

object ServiceChargesTable : Table("serrvicecharges") {
    val id: Column<Int> = integer("id").autoIncrement()
    val employeeId: Column<Int> = integer("employeeId")
    val date: Column<LocalDate> = date("date").defaultExpression(CurrentDate)
    val amount: Column<Double> = double("amount")

    override val primaryKey = PrimaryKey(id)
}

object BankAccountsTable : Table("bankAccounts") {
    val id: Column<Int> = integer("id").autoIncrement()
    val employeeId: Column<Int> = integer("employeeId")
    val bank: Column<String> = varchar("bank", 255)
    val account: Column<String> = varchar("account", 255)

    override val primaryKey = PrimaryKey(id)
}

object PaymentsTable : Table("payments") {
    val id: Column<Int> = integer("id").autoIncrement()
    var date: Column<LocalDate> = date("date").defaultExpression(CurrentDate)

    override val primaryKey = PrimaryKey(id)
}

fun init() {
    Database.connect(
        url = "jdbc:postgresql://localhost:5432/postgres",
        driver = "org.postgresql.Driver",
        user = "postgres",
        password = "postgres",
    )

    transaction {
        SchemaUtils.create(
            EmployeesTable,
            MembersTable,
            TimeRecordsTable,
            SalesReceiptsTable,
            ServiceChargesTable,
            BankAccountsTable,
            PaymentsTable,
        )
        var tables =
            exec(
                """
            SELECT tablename FROM pg_catalog.pg_tables 
            WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
            """,
            ) { rs ->
                val tables = mutableListOf<String>()
                while (rs.next()) {
                    tables.add(rs.getString("tablename"))
                }
                tables
            } ?: emptyList()

        println(tables)
    }
}
