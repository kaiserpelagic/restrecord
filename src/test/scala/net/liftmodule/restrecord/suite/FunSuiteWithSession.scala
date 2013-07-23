import org.scalatest._


class FunSuiteWithSession extends FunSuite {
  
  override def withFixture(test: NoArgTest) {
    try {
      // set up
      test()
    }
    finally {
      // tear down
    }
  }
}
