import org.example.db.BankAccountsTable
import org.example.db.EmployeesTable
import org.example.db.MembersTable
import org.example.db.SalaryTypeEnum
import org.example.db.salaryTypeToEnum
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate

sealed class EntityError(
    message: String,
) : Exception(message) {
    class UserError(
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
    fun addEmployee(
        id: Int,
        name: String,
        address: String,
        salaryType: SalaryType,
    ) {
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")

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
        hourse: Int,
    ) {
    }

    fun addSalesReceipt(
        employeeId: Int,
        date: LocalDate,
        sales: Int,
    ) {
    }

    fun addServiceCharge(
        employeeId: Int,
        amount: Int,
    ) {
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
        val employees =
            transaction {
                EmployeesTable.selectAll().map { row ->
                    EmployeeResultRow(
                        id = row[EmployeesTable.id],
                        name = row[EmployeesTable.name],
                    )
                }
            }
        for (employee in employees) {
            println(employee)
        }
    }
}

data class EmployeeResultRow(
    val id: Int,
    val name: String,
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
