// import org.jetbrains.exposed.dao.id.IntIdTable
// import org.jetbrains.exposed.sql.javatime.date
//
// // 給料の受け取り方法 Enum（例）
// enum class PayMethod { CASH, BANK_TRANSFER }
//
// // 従業員テーブル
// object Employees : IntIdTable() {
//     val hourlyWage = integer("hourlyWage")
//     val monthlySalary = integer("monthlySalary")
//     val bonus = integer("bonus")
//     val payMethod = enumerationByName("payMethod", 20, PayMethod::class)
//     val isMember = bool("isMember")
//     val groupFee = integer("groupFee")
// }
//
// // 勤務時間テーブル（1従業員に複数日分）
// object EmployeeWorkHours : IntIdTable() {
//     val employee = reference("employeeId", Employees)
//     val day = date("day")
//     val hoursWorked = integer("hoursWorked")
// }
//
// // 売上レポート用テーブル
// object EmployeeSalesReports : IntIdTable() {
//     val employee = reference("employeeId", Employees)
//     val reportData = text("reportData")
//     // 必要に応じてSalesReportの内容を適切に分割
// }
//
// // 給料支払日
// object EmployeePayDates : IntIdTable() {
//     val employee = reference("employeeId", Employees)
//     val payDate = date("payDate")
// }
//
// fun init() {
//     Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")
//
//     transaction {
//         // テーブル作成（まとめて複数可）
//         SchemaUtils.create(
//             Employees,
//             EmployeeWorkHours,
//             EmployeeSalesReports,
//             EmployeePayDates,
//         )
//     }
// }
