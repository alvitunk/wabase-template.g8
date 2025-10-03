package macros

import org.tresql.{Expr, QueryBuilder}

object Macros extends org.wabase.Macros {

  def demo(b: QueryBuilder, expr: Expr): Expr = expr

}