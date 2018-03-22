import sbt._

object MicroServiceBuild extends Build with MicroService {

  import play.sbt.routes.RoutesKeys._

  import scala.util.Properties._

  val appName = "tai"

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
  override lazy val playSettings: Seq[Setting[_]] = Seq(routesImport ++= Seq("uk.gov.hmrc.tai.binders._", "uk.gov.hmrc.domain._"))

}

private object AppDependencies {
  import play.sbt.PlayImport._
  private val pegdownVersion = "1.4.2"
  private val scalatestVersion = "2.2.2"

  val compile = Seq(
    filters,
    ws,
    "uk.gov.hmrc" %% "bootstrap-play-25" % "1.2.0",
    "uk.gov.hmrc" %% "domain" % "5.1.0",
    "uk.gov.hmrc" %% "json-encryption" % "3.2.0",
    "uk.gov.hmrc" %% "mongo-caching" % "5.0.0" exclude("uk.gov.hmrc","time_2.11")
  )

  trait TestDependencies {
    lazy val scope: String = "test,it"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % "2.3.0",
        "org.scalatest" %% "scalatest" % scalatestVersion % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % scope,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "org.jsoup" % "jsoup" % "1.7.3" % scope,
        "org.scalacheck" %% "scalacheck" % "1.13.4" % scope,
        "org.mockito" % "mockito-core" % "1.9.5")
    }.test
  }

  def apply() = compile ++ Test()

}


