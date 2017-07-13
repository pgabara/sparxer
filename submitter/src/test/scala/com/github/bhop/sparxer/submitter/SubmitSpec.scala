package com.github.bhop.sparxer.submitter

import org.scalatest._

class SubmitSpec extends WordSpec with Matchers {

  "A Submitter" when {
    "submitting new job" should {
      "return the result" in {
        Submit.submit(s => s"Job submitted: $s") should equal { "Job submitted: running job" }
      }
    }
  }
}
