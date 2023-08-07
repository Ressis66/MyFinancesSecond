package ru.finances.domain

import slick.lifted.ForeignKeyQuery

import java.util.concurrent.TimeUnit
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}

object DomHandler {

  import slick.jdbc.PostgresProfile.api._

  import scala.concurrent.ExecutionContext.Implicits.global

  case class Budget(
                     id: Long,
                     title: String,
                     amount: BigDecimal,
                     description: String,
                     usID: Long
                   )


  case class FullBudget(
                         id: Long,
                         title: String,
                         amount: BigDecimal,
                         description: String,
                         lineItems: List[LineItem],
                         user: User
                       )


  case class Expense(id: Long,
                     amount: Long,
                     description: String,
                     linID: Long
                    )

  case class FullExpense(id: Long,
                         amount: Long,
                         description: String,
                         lineItem: LineItem
                        )

  case class LineItem(id: Long,
                      budgetCategory: String,
                      projectedAmount: Long,
                      totalAmountSpent: Long,
                      budID: Long
                     )

  case class FullLineItem(id: Long,
                          budgetCategory: String,
                          projectedAmount: Long,
                          totalAmountSpent: Long,
                          expenses: List[Expense],
                          budget: Budget
                         )

  case class User(
                   id: Long,
                   name: String
                 )


  class Budgets(tag: Tag) extends Table[Budget](tag, Some("budgets"),"Budgets") {
    def id = column[Long]("id", O.PrimaryKey)

    def title = column[String]("title")

    def amount = column[BigDecimal]("amount")

    def description = column[String]("description")

    def usID = column[Long]("US_ID")

    override def * = (id, title, amount, description, usID) <> (Budget.tupled, Budget.unapply)

    def user = foreignKey("US_FK", usID, users)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)

  }

  val budgets = TableQuery[Budgets]


  class Expenses(tag: Tag) extends Table[Expense](tag,Some("budgets"), "Expenses") {
    def id = column[Long]("id", O.PrimaryKey)

    def amount = column[Long]("amount")

    def description = column[String]("description")

    def linID = column[Long]("LIN_ID")

    override def * = (id, amount, description, linID) <> (Expense.tupled, Expense.unapply)

    def lineItem = foreignKey("LIN_FK", linID, linItems)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)


  }

  val expenses = TableQuery[Expenses]

  class LinItems(tag: Tag) extends Table[LineItem](tag,Some("budgets"), "LineItems") {
    def id = column[Long]("id", O.PrimaryKey)

    def budgetCategory = column[String]("BUDGETCATEGORY")

    /*def budgetCategory = foreignKey("BUDGETCATEGORY_FK", budgetCategoryID, budgetsCategories)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)*/

    def projectedAmount = column[Long]("projectedAmount")

    def totalAmountSpent = column[Long]("totalAmountSpent")

    def budID = column[Long]("BUD_ID")

    override def * = (id, budgetCategory, projectedAmount, totalAmountSpent, budID) <> (LineItem.tupled, LineItem.unapply)

    def budget: ForeignKeyQuery[Budgets, Budget] = foreignKey("BUD_FK", budID, budgets)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)


  }

  val linItems = TableQuery[LinItems]

  class Users(tag: Tag) extends Table[User](tag, Some("budgets"), "User") {
    def id = column[Long]("id", O.PrimaryKey)

    def name = column[String]("name")

    override def * = (id, name) <> (User.tupled, User.unapply)
  }

  val users = TableQuery[Users]

  lazy val insertBudget = budgets returning budgets.map(_.id)
 /* lazy val insertBudgetCategory = budgetsCategories returning budgetsCategories.map(_.id)*/
  lazy val insertExpense = expenses returning expenses.map(_.id)
  lazy val insertLineItem = linItems returning linItems.map(_.id)
  lazy val insertUser = users returning users.map(_.id)


  val db = Database.forConfig("postgres")

  val createTables = (budgets.schema  ++ expenses.schema
    ++ linItems.schema ++ users.schema).create


  def filterUser(name: String): Future[User] = {
    db.run(users.filter(_.name.like(s"%${name}%")).result.head)

  }

  def deleteUser(id: Long): Future[Int] = {
    db.run(users.filter(_.id === id).delete)
  }

  def findAllUsers() = {
    val resultFuture: Future[Seq[User]] = db.run(users.result)
    resultFuture.onComplete {
      case Success(values) => println(s"Fetched: ${values.mkString(",")}")
      case Failure(exception) => println(s"Query failed, reason: $exception")
    }
  }

  def insertUser(user: User): Future[Int]= {
    val queryDescription = users += user
    db.run(queryDescription)
  }

  def filterBudget(id: Long): Future[FullBudget] = {
    val query = for {
      /*(n, pl) <- budgets join users on (_.usID === _.id)*/
      ((budget, user), lineItems) <- budgets joinLeft users on (_.usID === _.id) joinLeft linItems on (_._1.id === _.budID)
    } yield (budget, user, lineItems)
    println("!!!!!!!!!!!!!!!!!!")
    db.run(query.result).map { row =>
      row.map { r =>
        val id = r._1.id
        val title = r._1.title
        val amount = r._1.amount
        val description = r._1.description
        val user = r._2.head
        val lineItems = r._3.toSeq.toList
        FullBudget(id, title, amount, description, lineItems, user)
      }.toList.filter(_.id == id).head
    }

  }
  def filterBudgetsWithUser(user: User) = {
    val query = for {
      /*(n, pl) <- budgets join users on (_.usID === _.id)*/
      ((budget, user), lineItems) <- budgets joinLeft users on (_.usID === _.id) joinLeft linItems on (_._1.id === _.budID)
    } yield (budget, user, lineItems)
    println("!!!!!!!!!!!!!!!!!!")
    db.run(query.result).map { row =>
      row.map { r =>
        val id = r._1.id
        val title = r._1.title
        val amount = r._1.amount
        val description = r._1.description
        val user = r._2.head
        val lineItems = r._3.toSeq.toList
        FullBudget(id, title, amount, description, lineItems, user)
      }.toList.filter(_.user == user)
    }
  }

  def createBudget(budget: Budget): Future[Int] = {
    val queryDescription = budgets += budget
   db.run(queryDescription)

  }

  def updateBudget(budget: Budget): Future[Int] = {
    val updatedBudget = Budget(budget.id, budget.title, budget.amount, budget.description, budget.usID)
    db.run(budgets.insertOrUpdate(updatedBudget))
  }

  def deleteBudget(id: Long): Future[Int] = {
    db.run(budgets.filter(_.id === id).delete)
  }

  def filterLineItem(id: Long) = {
    val query = for {
      ((lineItem, budget), expenses)<- linItems joinLeft budgets on (_.budID === _.id)  joinLeft expenses on (_._1.id===_.linID)
    } yield (lineItem, budget,  expenses)
    db.run(query.result).map { row =>
      row.map { r =>
        val id = r._1.id
        val budgetCategory = r._1.budgetCategory
        val projectedAmount = r._1.projectedAmount
        val totalAmountSpent = r._1.totalAmountSpent
        val expenses = r._3.toList
        val budget = r._2.head
        FullLineItem(id, budgetCategory, projectedAmount, totalAmountSpent,  expenses, budget)
      }.toList.filter(_.id == id).head

    }
  }

  def createLineItem(lineItem: LineItem): Future[Int]  = {
    val queryDescription = linItems += lineItem
     db.run(queryDescription)

  }

  def updateLineItem(lineItem: LineItem): Future[Int] = {
    val updatedLineItem = LineItem(lineItem.id, lineItem.budgetCategory, lineItem.projectedAmount, lineItem.totalAmountSpent, lineItem.budID)
    db.run(linItems.insertOrUpdate(updatedLineItem))
  }

  def deleteLineItem(id: Long): Future[Int] = {
    db.run(linItems.filter(_.id === id).delete)
  }

  def filterExpense(id: Long) = {
    val query = for {
      (expense, lineItem) <- expenses joinLeft linItems on (_.linID === _.id)
    } yield (expense, lineItem)
    db.run(query.result) map { row =>
      row.map { r =>
        val id = r._1.id
        val amount = r._1.amount
        val description = r._1.description
        val lineItem = r._2.head
        FullExpense(id, amount, description, lineItem)
      }.toList.filter(_.id == id).head
    }
  }


  def createExpense(expense: Expense): Future[Int] = {
    val queryDescription = expenses += expense
    linItems.filter(_.id===expense.linID).map(_.totalAmountSpent+expense.amount)
    db.run(queryDescription)

  }

  def updateExpense(expense: Expense) = {
    val exp: Expense = Await.result(db.run(expenses.filter(_.id === expense.id).result.head), (5, TimeUnit.SECONDS))
    linItems.filter(_.id===expense.linID).map(_.totalAmountSpent-exp.amount)
    val updatedExpense = Expense(expense.id, expense.amount, expense.description, expense.linID)
    linItems.filter(_.id===expense.linID).map(_.totalAmountSpent+expense.amount)
    db.run(expenses.insertOrUpdate(updatedExpense))
  }

  def deleteExpense(id: Long) = {
    val exp: Expense = Await.result(db.run(expenses.filter(_.id === id).result.head), (5, TimeUnit.SECONDS))
    linItems.filter(_.id===exp.linID).map(_.totalAmountSpent-exp.amount)
    db.run(expenses.filter(_.id === id).delete)
  }

}

