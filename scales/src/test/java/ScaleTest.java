
class ScaleTest extends ScaleGeneralTest {
    @Override
    void actualTests() {
        putPhoto("pretty_woman.jpg");
        putUserpic("pretty_woman.jpg");
        putPhoto("ugly_man.jpg");
        putUserpic("ugly_man.jpg");
    }
}
