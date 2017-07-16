package com.github.bhop.sparxer

trait Calculator {

  def add[A: Numeric](x: A, y: A): A = implicitly[Numeric[A]].add(x, y)

  def subtract[A: Numeric](x: A, y: A): A = implicitly[Numeric[A]].subtract(x, y)
}

trait Numeric[A] {

  def add(x: A, y: A): A
  def subtract(x: A, y: A): A
}

object Numeric {

  implicit val intNumeric = new Numeric[Int] {
    def add(x: Int, y: Int): Int = x + y
    def subtract(x: Int, y: Int): Int = x - y
  }

  implicit def optionNumeric[A](implicit A: Numeric[A]) = new Numeric[Option[A]] {

    def add(x: Option[A], y: Option[A]): Option[A] =
      for {
        xx <- x
        yy <- y
      } yield A.add(xx, yy)

    def subtract(x: Option[A], y: Option[A]): Option[A] =
      for {
        xx <- x
        yy <- y
      } yield A.subtract(xx, yy)
  }
}
