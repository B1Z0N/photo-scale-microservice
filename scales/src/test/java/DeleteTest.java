
public class DeleteTest extends ScaleGeneralTest {
    @Override
    public void actualTests() {
        delPhoto("pretty_woman.jpg");
        delUserpic("pretty_woman.jpg");
        delPhoto("ugly_man.jpg");
        delUserpic("ugly_man.jpg");
    }
}
