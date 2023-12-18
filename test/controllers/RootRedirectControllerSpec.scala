/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package controllers

import models.NormalMode
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}

/**
  * @author Yuriy Tumakha
  */
class RootRedirectControllerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with Injecting {

  private val fakeRequest = FakeRequest("GET", "/")
  private val controller = inject[RootRedirectController]

  "RootRedirectController" should {
    "return redirect to start page" in {
      val result = controller.rootRedirect(fakeRequest)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.LoginController.onPageLoad(NormalMode).url)
    }
  }

}
