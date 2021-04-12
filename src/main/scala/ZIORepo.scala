import scalikejdbc._
import zio._

object ZIORepo {
  implicit def zioTxBoundary[R <: blocking.Blocking, A]: TxBoundary[RIO[R, A]] =
    new TxBoundary[RIO[R, A]] {
      def finishTx(result: RIO[R, A], tx: Tx): RIO[R, A] =
        result.foldM(
          e => blocking.effectBlocking(tx.rollback()) *> ZIO.fail(e),
          s => blocking.effectBlocking(tx.commit()).as(s)
        )

      override def closeConnection(
          result: RIO[R, A],
          doClose: () => Unit
      ): RIO[R, A] =
        result.ensuring(blocking.effectBlocking(doClose()).either)
    }
}

class ZIORepo {
  def updateFirstName(id: Int, firstName: String)(implicit
      session: DBSession
  ): RIO[blocking.Blocking, Int] =
    blocking.effectBlocking {
      session.update(
        "update users set first_name = ? where id = ?",
        firstName,
        id
      )
    }

  def updateLastName(id: Int, lastName: String)(implicit
      session: DBSession
  ): RIO[blocking.Blocking, Int] =
    blocking.effectBlocking {
      session.update(
        "update users set last_name = ? where id = ?",
        lastName,
        id
      )
    }

  import ZIORepo._
  val fResult = DB.localTx { implicit session =>
    updateFirstName(3, "John") *> updateLastName(3, "Smith")
  }

}
