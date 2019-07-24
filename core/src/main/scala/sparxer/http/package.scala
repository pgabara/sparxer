package sparxer

import sparxer.submit.SparkSubmit
import sparxer.registry._
import zio.clock.Clock

package object http {
  import sparxer.http.Auth.AuthError
  import sparxer.registry.UserRegistry

  type JobEnv    = JobRegistry with StatusRegistry
  type SubmitEnv = JobRegistry with Reference with SparkSubmit
  type AuthEnv   = UserRegistry with Clock

  type HttpEnv = JobEnv with SubmitEnv with AuthEnv

  def toException(e: StatusRegistryError): Exception =
    new IllegalStateException(e.reason)

  def toException(e: JobRegistryError): Exception =
    new IllegalStateException(e.reason)

  def toException(e: UserRegistryError): Exception =
    new IllegalStateException(e.reason)

  def toException(e: AuthError): Exception =
    new IllegalStateException(e.reason)
}
