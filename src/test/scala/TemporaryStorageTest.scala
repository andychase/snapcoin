import TemporaryStorage.DebugStorage
import org.specs2.mutable.Specification

class TemporaryStorageTest  extends Specification {
    "Temporary storage" should {
        "validate" in {
            new DebugStorage().validateCredentials must beTrue
        }
    }
}
