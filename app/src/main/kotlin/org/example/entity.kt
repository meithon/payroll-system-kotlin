import PayMethodType
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.example.db.BankAccountsTable
import org.example.db.EmployeesTable
import org.example.db.MembersTable
import org.example.db.PaymentsTable
import org.example.db.SalaryTypeEnum
import org.example.db.SalesReceiptsTable
import org.example.db.ServiceChargesTable
import org.example.db.TimeRecordsTable
import org.example.db.salaryTypeToEnum
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

sealed class EntityError(
    message: String,
) : Exception(message) {
    class UserError(
        message: String,
    ) : EntityError(message)

    class DataError(
        message: String,
    ) : EntityError(message)

    class InternalError(
        message: String,
    ) : EntityError(message)
}

data class Employee(
    /** 時給 */
    val hourlyWage: Int,
    /** 日毎の勤務時間 */
    val hoursWorkedByDay: Array<Int>,
    /** 月給 */
    val monthlySalary: Int,
    /** 成果報酬額 */
    val bonus: Int,
    /** 売り上げ金額のレシート */
    val salesReports: Array<SalesReport>,
    /** 給料受け取り方法 */
    val payMethod: PayMethodType,
    /** 組合に入っているか */
    val isMember: Boolean,
    /** 組合費用 */
    val groupFee: Int,
    /** 給料を渡した日付 */
    val payDates: Array<LocalDate>,
) {
    /** 月の労働時間から給料を計算 */
    fun calculateMonthlySalary(hoursWorked: Int): Int = hourlyWage * hoursWorked
}

data class SalesReport(
    /** 売り上げ金額 */
    val sales: Int,
    /** 日付 */
    val date: LocalDate,
)
// 振り込みの種類
// 口座振り込み、小切手を住所に送るか,給与担当者が小切手を渡す

enum class PayMethodType {
    CASH, // 現金
    CHEQUE, // 小切手
    POSTAL, // 郵便為替
}

sealed class SalaryType {
    data class Hourly(
        val hourlyWage: Double,
    ) : SalaryType()

    data class Monthly(
        val monthlySalary: Double,
    ) : SalaryType()

    data class Commission(
        val monthlyRate: Double,
        val commissionRate: Double,
    ) : SalaryType()
}

class SalarySystem {
    constructor() {
        try {
            Database.connect(
                url = "jdbc:postgresql://localhost:5432/postgres",
                driver = "org.postgresql.Driver",
                user = "postgres",
                password = "postgres",
            )
            transaction {
                addLogger(StdOutSqlLogger)
                exec("SELECT 1") {}
            }
        } catch (e: Exception) {
            println("Connection failed: ${e.message}")
        }
    }

    fun addEmployee(
        id: Int,
        name: String,
        address: String,
        salaryType: SalaryType,
    ) {
        val salaryTypeEnum = salaryTypeToEnum(salaryType)

        when (salaryType) {
            is SalaryType.Hourly -> {
                transaction {
                    EmployeesTable.insert {
                        it[EmployeesTable.id] = id
                        it[EmployeesTable.name] = name
                        it[EmployeesTable.address] = address
                        it[EmployeesTable.salaryType] = salaryTypeEnum
                        it[EmployeesTable.hourlyWage] = salaryType.hourlyWage
                    }
                }
            }
            is SalaryType.Monthly -> {
                // Monthly
                transaction {
                    EmployeesTable.insert {
                        it[EmployeesTable.id] = id
                        it[EmployeesTable.name] = name
                        it[EmployeesTable.address] = address
                        it[EmployeesTable.salaryType] = salaryTypeEnum
                        it[EmployeesTable.monthlySalary] = salaryType.monthlySalary
                    }
                }
            }
            is SalaryType.Commission -> {
                // Commission
                transaction {
                    EmployeesTable.insert {
                        it[EmployeesTable.id] = id
                        it[EmployeesTable.name] = name
                        it[EmployeesTable.address] = address
                        it[EmployeesTable.salaryType] = salaryTypeEnum
                        it[EmployeesTable.monthlyRate] = salaryType.monthlyRate
                        it[EmployeesTable.commissionRate] = salaryType.commissionRate
                    }
                }
            }
        }
    }

    fun deleteEmployee(employeeId: Int) {
        transaction {
            EmployeesTable.deleteWhere { EmployeesTable.id eq employeeId }
        }
    }

    fun addTimeCard(
        employeeId: Int,
        date: LocalDate,
        hourse: Double,
    ) {
        transaction {
            TimeRecordsTable.insert {
                it[TimeRecordsTable.employeeId] = employeeId
                it[TimeRecordsTable.date] = date
                it[TimeRecordsTable.hours] = hourse
            }
        }
    }

    fun addSalesReceipt(
        employeeId: Int,
        date: LocalDate,
        amount: Double,
    ) {
        transaction {
            SalesReceiptsTable.insert {
                it[SalesReceiptsTable.employeeId] = employeeId
                it[SalesReceiptsTable.date] = date
                it[SalesReceiptsTable.amount] = amount
            }
        }
    }

    fun addServiceCharge(
        employeeId: Int,
        amount: Double,
    ) {
        transaction {
            ServiceChargesTable.insert {
                it[ServiceChargesTable.employeeId] = employeeId
                it[ServiceChargesTable.amount] = amount
            }
        }
    }

    private fun getEmployeeSalaryType(employeeId: Int): SalaryTypeEnum {
        val salaryTypeValue =
            transaction {
                EmployeesTable
                    .select(EmployeesTable.salaryType)
                    .where { EmployeesTable.id eq employeeId }
                    .firstOrNull()
                    ?.get(EmployeesTable.salaryType)
            }
        if (salaryTypeValue == null) {
            throw EntityError.UserError("Employee with id $employeeId not found")
        }
        return salaryTypeValue
    }

    // Name         <name>                 従業員名
    // Address      <address>              住所
    // Hourly       <HourlyRate>           時間給
    // Salaried     <salary>               固定給
    // Commissioned <salary> <rate>        手当
    // PayMethod    <payMethod>            支払い形態
    // Hold                                給与担当者が小切手を渡すまで待つ
    // Direct       <bank> <account>       直接振り込み
    // Mail         <mail>                 小切手を郵送
    // Member       <memberId> Dues <rate> 組合員情報を変更する
    // NoMember                            組合員から外す
    fun changeEmployee(
        employeeId: Int,
        change: ChangeEmployeeField,
    ) {
        when (change) {
            is ChangeEmployeeField.Name -> {
                transaction {
                    EmployeesTable.update({ EmployeesTable.id eq employeeId }) {
                        it[EmployeesTable.name] = change.name
                    }
                }
            }
            is ChangeEmployeeField.Address -> {
                transaction {
                    EmployeesTable.update({ EmployeesTable.id eq employeeId }) {
                        it[EmployeesTable.address] = change.address
                    }
                }
            }
            is ChangeEmployeeField.Hourly -> {
                val salaryType = getEmployeeSalaryType(employeeId)
                if (salaryType != SalaryTypeEnum.Hourly) {
                    throw EntityError.UserError("Employee with id $employeeId is not hourly salary")
                }
                transaction {
                    EmployeesTable.update({ EmployeesTable.id eq employeeId }) {
                        // TODO: Rateが何かわからない
                        it[EmployeesTable.hourlyWage] = change.hourlyRate
                    }
                }
            }
            is ChangeEmployeeField.Salaried -> {
                val salaryType = getEmployeeSalaryType(employeeId)
                if (salaryType != SalaryTypeEnum.Monthly) {
                    throw EntityError.UserError("Employee with id $employeeId is not monthly salary")
                }
                transaction {
                    EmployeesTable.update({ EmployeesTable.id eq employeeId }) {
                        it[EmployeesTable.monthlySalary] = change.salary
                    }
                }
            }
            is ChangeEmployeeField.Commissioned -> {
                val salaryType = getEmployeeSalaryType(employeeId)
                if (salaryType != SalaryTypeEnum.Commission) {
                    throw EntityError.UserError("Employee with id $employeeId is not commission salary")
                }
                transaction {
                    EmployeesTable.update({ EmployeesTable.id eq employeeId }) {
                        it[EmployeesTable.commissionRate] = change.commissionRate
                    }
                }
            }
            is ChangeEmployeeField.PayMethod -> {
                transaction {
                    EmployeesTable.update({ EmployeesTable.id eq employeeId }) {
                        it[EmployeesTable.payMethod] = change.payMethod
                    }
                }
            }
            is ChangeEmployeeField.Hold -> {
                transaction {
                    EmployeesTable.update({ EmployeesTable.id eq employeeId }) {
                        it[EmployeesTable.isHold] = true
                    }
                }
            }
            is ChangeEmployeeField.Direct -> {
                transaction {
                    BankAccountsTable.insert {
                        it[BankAccountsTable.employeeId] = employeeId
                        it[BankAccountsTable.bank] = change.bank
                        it[BankAccountsTable.account] = change.account
                    }
                }
            }
            is ChangeEmployeeField.Mail -> {
                transaction {
                    EmployeesTable.update({ EmployeesTable.id eq employeeId }) {
                        it[EmployeesTable.mail] = change.mail
                    }
                }
            }
            is ChangeEmployeeField.Member -> {
                transaction {
                    MembersTable.update({ MembersTable.employeeId eq employeeId }) {
                        it[MembersTable.dues] = change.dues
                    }
                }
            }
            is ChangeEmployeeField.NoMember -> {
                transaction {
                    MembersTable.deleteWhere { MembersTable.employeeId eq employeeId }
                }
            }
        }
    }

    fun payday(date: LocalDate) {
        // 取得処理
        var rows =
            transaction {
                val employeeRows = EmployeesTable.selectAll().toList()
                val memberMap = MembersTable.selectAll().groupBy { it[MembersTable.employeeId] }
                val timeRecordMap = TimeRecordsTable.selectAll().groupBy { it[TimeRecordsTable.employeeId] }
                val salesReceiptMap = SalesReceiptsTable.selectAll().groupBy { it[SalesReceiptsTable.employeeId] }
                val serviceChargeMap = ServiceChargesTable.selectAll().groupBy { it[ServiceChargesTable.employeeId] }
                val bankAccountMap = BankAccountsTable.selectAll().groupBy { it[BankAccountsTable.employeeId] }

                employeeRows.map { row ->
                    EmployeeWithAll(
                        id = row[EmployeesTable.id],
                        name = row[EmployeesTable.name],
                        address = row[EmployeesTable.address],
                        salaryType = row[EmployeesTable.salaryType],
                        hourlyWage = row[EmployeesTable.hourlyWage],
                        monthlySalary = row[EmployeesTable.monthlySalary],
                        monthlyRate = row[EmployeesTable.monthlyRate],
                        commissionRate = row[EmployeesTable.commissionRate],
                        payMethod = row[EmployeesTable.payMethod],
                        mail = row[EmployeesTable.mail],
                        isHold = row[EmployeesTable.isHold],
                        members =
                            memberMap[row[EmployeesTable.id]]?.map { m ->
                                Member(m[MembersTable.id], m[MembersTable.dues])
                            } ?: emptyList(),
                        timeRecords =
                            timeRecordMap[row[EmployeesTable.id]]?.map { t ->
                                TimeRecord(t[TimeRecordsTable.id], t[TimeRecordsTable.date], t[TimeRecordsTable.hours])
                            } ?: emptyList(),
                        salesReceipts =
                            salesReceiptMap[row[EmployeesTable.id]]?.map { s ->
                                SalesReceipt(s[SalesReceiptsTable.id], s[SalesReceiptsTable.date], s[SalesReceiptsTable.amount])
                            } ?: emptyList(),
                        serviceCharges =
                            serviceChargeMap[row[EmployeesTable.id]]?.map { sc ->
                                ServiceCharge(sc[ServiceChargesTable.id], sc[ServiceChargesTable.date], sc[ServiceChargesTable.amount])
                            } ?: emptyList(),
                        bankAccounts =
                            bankAccountMap[row[EmployeesTable.id]]?.map { b ->
                                BankAccount(b[BankAccountsTable.id], b[BankAccountsTable.bank], b[BankAccountsTable.account])
                            } ?: emptyList(),
                    )
                }
                // allに従業員ごとのリストが格納されます
            }
        var lastPayDate =
            transaction {
                PaymentsTable
                    .select(PaymentsTable.date)
                    .orderBy(PaymentsTable.date, SortOrder.DESC)
                    .limit(1)
                    .firstOrNull()
                    ?.get(PaymentsTable.date)
            } ?: rows
                .flatMap { row ->
                    row.timeRecords.map { it.date } + row.salesReceipts.map { it.date }
                }.minOrNull()
                .let {
                    Clock.System
                        .now()
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .date
                }
        var now =
            Clock.System
                .now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date

        var monthlyRate = calcMonthlyWorkRatioKtx(lastPayDate, now)

        for (row in rows) {
            var amount: Double =
                when (row.salaryType) {
                    SalaryTypeEnum.Hourly -> {
                        var overtimeRete = 1.5
                        var amount = 0.0

                        var hourlyWage = row.hourlyWage
                        if (hourlyWage == null) {
                            throw EntityError.DataError("hourlyWage is null")
                        }

                        for (record in row.timeRecords) {
                            amount += record.hours * hourlyWage
                            if (record.hours > 8) {
                                var overtimeHours = record.hours - 8
                                amount += overtimeHours * hourlyWage * overtimeRete
                            } else {
                                amount += record.hours * hourlyWage
                            }
                        }
                        amount
                    }
                    SalaryTypeEnum.Monthly -> {
                        var monthlySalary = row.monthlySalary
                        if (monthlySalary == null) {
                            throw EntityError.DataError("monthlySalary is null")
                        }
                        var amount = monthlyRate * monthlySalary
                        amount
                    }
                    SalaryTypeEnum.Commission -> {
                        var commissionRate = row.commissionRate
                        require(commissionRate != null) { "commissionRate is null" }
                        require(row.monthlySalary != null) { "monthlySalary is null" }

                        var amount = 0.0
                        for (salaryReceipt in row.salesReceipts) {
                            amount += salaryReceipt.amount * commissionRate
                        }
                        amount += monthlyRate * row.monthlySalary

                        amount
                    }
                }
            println("${row.name} ${row.payMethod} $amount")
        }
    }
}

data class EmployeeWithAll(
    val id: Int,
    val name: String,
    val address: String,
    val salaryType: SalaryTypeEnum,
    val hourlyWage: Double?,
    val monthlySalary: Double?,
    val monthlyRate: Double?,
    val commissionRate: Double?,
    val payMethod: PayMethodType?,
    val mail: String?,
    val isHold: Boolean,
    val members: List<Member>,
    val timeRecords: List<TimeRecord>,
    val salesReceipts: List<SalesReceipt>,
    val serviceCharges: List<ServiceCharge>,
    val bankAccounts: List<BankAccount>,
)

data class Member(
    val id: Int,
    val dues: Double,
)

data class TimeRecord(
    val id: Int,
    val date: LocalDate,
    val hours: Double,
)

data class SalesReceipt(
    val id: Int,
    val date: LocalDate,
    val amount: Double,
)

data class ServiceCharge(
    val id: Int,
    val date: LocalDate,
    val amount: Double,
)

data class BankAccount(
    val id: Int,
    val bank: String,
    val account: String,
)

sealed class ChangeEmployeeField {
    data class Name(
        val name: String,
    ) : ChangeEmployeeField()

    data class Address(
        val address: String,
    ) : ChangeEmployeeField()

    data class Salaried(
        val salary: Double,
    ) : ChangeEmployeeField()

    data class Commissioned(
        val salary: Double,
        val commissionRate: Double,
    ) : ChangeEmployeeField()

    data class PayMethod(
        val payMethod: PayMethodType,
    ) : ChangeEmployeeField()

    data class Hourly(
        val hourlyRate: Double,
    ) : ChangeEmployeeField()

    object Hold : ChangeEmployeeField()

    data class Direct(
        val bank: String,
        val account: String,
    ) : ChangeEmployeeField()

    data class Mail(
        val mail: String,
    ) : ChangeEmployeeField()

    data class Member(
        val memberId: Int,
        val dues: Double,
    ) : ChangeEmployeeField()

    object NoMember : ChangeEmployeeField()
}

sealed class Salary {
    class HourlyRate(
        val hourlyRate: Int,
    ) : Salary()

    class MonthlySalary(
        val monthlySalary: Int,
    ) : Salary()

    class CommissionRate(
        val commissionRate: Int,
    ) : Salary()
}

fun calcMonthlyWorkRatioKtx(
    start: LocalDate,
    end: LocalDate,
): Double {
    require(!(end < start)) { "endはstart以降の日付である必要があります" }

    var totalRatio = 0.0
    var current = LocalDate(start.year, start.month, 1)

    while (current <= end) {
        // うるう年判定
        val isLeap = current.year % 4 == 0 && (current.year % 100 != 0 || current.year % 400 == 0)
        // 月の日数（うるう年を考慮）
        val monthLength = current.month.length(isLeap)
        // 最初の月の1日目 or それ以外→1日
        val firstDay = if (current.year == start.year && current.month == start.month) start.dayOfMonth else 1
        // 最後の月のend日 or それ以外→月末
        val lastDay = if (current.year == end.year && current.month == end.month) end.dayOfMonth else monthLength

        val workedDays = lastDay - firstDay + 1
        val ratio = workedDays.toDouble() / monthLength

        totalRatio += ratio
        current = current.plus(DatePeriod(months = 1))
    }
    return totalRatio
}
