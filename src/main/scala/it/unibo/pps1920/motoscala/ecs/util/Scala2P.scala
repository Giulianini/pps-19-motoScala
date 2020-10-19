package it.unibo.pps1920.motoscala.ecs.util

import alice.tuprolog._


object Scala2P {
  def extractTerm(t: Term, i: Integer): Term = t.asInstanceOf[Struct].getArg(i).getTerm

  implicit def intToTerm(i: scala.Int): Term = Term.createTerm(i.toString)
  implicit def stringToTerm(s: String): Term = Term.createTerm(s)
  implicit def seqToTerm[T](s: Seq[T]): Term = s.mkString("[", ",", "]")
  implicit def tuple2Term(tuple2: (scala.Double, scala.Double)): Term = Term.createTerm(tuple2.toString())
  implicit def termToDouble(t: Term): (scala.Double) = t.toString.toDouble
}
