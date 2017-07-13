package com.github.bhop.sparxer.submitter

object Submit {

  def submit(job: String => String): String = job("running job")
}
