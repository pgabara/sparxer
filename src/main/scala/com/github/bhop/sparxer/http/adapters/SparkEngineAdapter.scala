package com.github.bhop.sparxer.http.adapters

import akka.Done
import com.github.bhop.sparxer.adapter.domain.JobConfig
import monix.eval.Task

trait SparkEngineAdapter {
  def submit(config: JobConfig): Task[Done]
}
