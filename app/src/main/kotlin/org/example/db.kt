package org.example.db

// import org.jetbrains.exposed.sql.Table
// // import org.jetbrains.exposed.dao.id.IntIdTable
// import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
// import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.*
import SalaryType
import PayMethodType

// 給料の受け取り方法 Enum（例）

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

// 従業員テーブル

object EmployeesTable : Table() {
    val id: Column<Int> = integer("id").autoIncrement()
    val name: Column<String> = varchar("name", 255)
    val address = varchar("address", 255)
    val salaryType = enumerationByName("salaryType", 20, SalaryTypeEnum::class)
    val hourlyWage = double("hourlyWage").nullable()
    val monthlySalary = double("monthlySalary").nullable()
    val monthlyRate = double("monthlyRate").nullable()
    val commissionRate = double("commissionRate").nullable()
    val payMethod = enumerationByName("payMethod", 20, PayMethodType::class)
    val mail = varchar("mail", 255).nullable()
    val isHold = bool("hold").default(false)

    override val primaryKey = PrimaryKey(id)
}

object MembersTable : Table() {
    val id: Column<Int> = integer("id").autoIncrement()
    val employeeId: Column<Int> = integer("employeeId")
    val dues: Column<Double> = double("dues")

    override val primaryKey = PrimaryKey(id)
}

object BankAccountsTable : Table() {
    val id: Column<Int> = integer("id").autoIncrement()
    val employeeId: Column<Int> = integer("employeeId")
    val bank: Column<String> = varchar("bank", 255)
    val account: Column<String> = varchar("account", 255)

    override val primaryKey = PrimaryKey(id)
}

fun init() {
    Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")

    transaction {
        SchemaUtils.create(
            EmployeesTable,
        )
    }
}
