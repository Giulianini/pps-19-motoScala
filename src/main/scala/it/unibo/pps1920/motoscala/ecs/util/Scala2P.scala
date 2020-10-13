package it.unibo.pps1920.motoscala.ecs.util

import alice.tuprolog._


object Scala2P {
  def extractTerm(t: Term, i: Integer): Term = t.asInstanceOf[Struct].getArg(i).getTerm

  implicit def stringToTerm(s: String): Term = Term.createTerm(s)
  implicit def seqToTerm[T](s: Seq[T]): Term = s.mkString("[", ",", "]")
  implicit def termToTuple(t: Term): (scala.Double, scala.Double) = {
    val s = t.toString.stripPrefix("','(").reverse.stripPrefix(")").reverse.split(",")
    (s(0).toDouble, s(1).toDouble)
  }

  def mkPrologEngine(theory: Theory): Term => LazyList[Term] = {
    goal =>
      new Iterable[Term] {
        val engine = new Prolog
        engine.setTheory(theory)

        override def iterator: Iterator[Term] = new Iterator[Term] {
          var solution: SolveInfo = engine.solve(goal)

          override def hasNext: Boolean = solution.isSuccess || solution.hasOpenAlternatives
          override def next(): Term = try {solution.getSolution } finally {
            if (solution.hasOpenAlternatives) {solution = engine.solveNext }
          }
        }
      }.to(LazyList)
  }
}
