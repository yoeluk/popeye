package com.popeyepdf

/**
 * Created by yoelusa on 29/03/15.
 */
import org.scalatest.{Suite, BeforeAndAfterAll}
import akka.testkit.TestKit

trait StopSystemAfterAll extends BeforeAndAfterAll {
  this: TestKit with Suite =>
  override protected def afterAll() {
    super.afterAll()
    system.shutdown()
  }
}