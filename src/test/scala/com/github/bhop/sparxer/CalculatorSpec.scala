package com.github.bhop.sparxer

import org.scalatest._

class CalculatorSpec extends WordSpec with Matchers with Calculator {

  "A Calculator" when {

    "adding" should {
      "return the sum of two integers" in {
        add(5, 10) should equal { 15 }
      }

      "return the sum of two optional integers" in {
        add(Option(5), Option(5)) should equal { Some(10) }
      }

      "return none if one of optional integers is empty" in {
        add(Option(5), None) should equal { None }
      }
    }

    "subtracting" should {
      "return the difference of two integers" in {
        subtract(5, 10) should equal { -5 }
      }

      "return the difference of two optional integers" in {
        subtract(Option(5), Option(5)) should equal { Some(0) }
      }

      "return none if one of optional integers is empty" in {
        subtract(Option(5), None) should equal { None }
      }
    }
  }
}
