package profiles.model;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.JsonObject;

import java.awt.*;

/** Class for transfering config messages over EventBus */
public class ImageScalesURLsCodec implements MessageCodec<ImageScalesURLs, ImageScalesURLs> {

    @Override
    public void encodeToWire(Buffer buffer, ImageScalesURLs imgurls) {
        String contractsStr = imgurls.toJson().encode();
        int length = contractsStr.getBytes().length;
        buffer.appendInt(length);
        buffer.appendString(contractsStr);
    }

    @Override
    public ImageScalesURLs decodeFromWire(int pos, Buffer buffer) {
        int position = pos;
        int length = buffer.getInt(position);
        String jsonStr = buffer.getString(position += 4, position += length);
        JsonObject json = new JsonObject(jsonStr);

        return new ImageScalesURLs(json);
    }

    @Override
    public ImageScalesURLs transform(ImageScalesURLs imgurls) {
        return imgurls;
    }

    @Override
    public String name() {
        return getClass().getSimpleName();
    }

    @Override
    public byte systemCodecID() {
        return -1;
    }
}
