import java.time.LocalDate

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
  val payMethod: PayMethod,
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

enum class PayMethod {
  CASH, // 現金
  CHEQUE, // 小切手
  POSTAL, // 郵便為替
}

class SalarySystem {
  fun addEmployee(
    employeeId: Int,
    name: String,
    address: String,
    // HourlyRate
    // MonthlySalary
    // MonthlySalary commissionRate
  ) {
  }

  fun deleteEmployee(employeeId: Int) {
  }

  fun timeCard(
    employeeId: Int,
    date: LocalDate,
    hourse: Int,
  ) {
  }

  fun salesReceipt(
    employeeId: Int,
    date: LocalDate,
    sales: Int,
  ) {
  }

  fun serviceCharge(
    employeeId: Int,
    amount: Int,
  ) {
  }

  fun changeEmployee(
    employeeId: Int,
    field: ChangeEmployeeField,
  ) {
  }

  fun payday(date: LocalDate) {
  }
}

sealed class ChangeEmployeeField {
  class Name(
    val name: String,
  ) : ChangeEmployeeField()

  class Address(
    val address: String,
  ) : ChangeEmployeeField()

  class HourlyWage(
    val hourlyWage: Int,
  ) : ChangeEmployeeField()
  // todo
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
// HourlyRate
// MonthlySalary
// MonthlySalary commissionRate
