package integration;

class DeleteTest extends ScaleGeneralTest {
    @Override
    void actualTests() {
        delPhoto("pretty_woman.jpg");
        delUserpic("pretty_woman.jpg");
        delPhoto("ugly_man.jpg");
        delUserpic("ugly_man.jpg");
    }
}
