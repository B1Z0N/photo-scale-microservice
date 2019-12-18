package profiles.model;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.JsonObject;

/** Class for transfering config messages over EventBus */
public class OriginURLCodec implements MessageCodec<OriginURL, OriginURL> {

    @Override
    public void encodeToWire(Buffer buffer, OriginURL imgurl) {
        String contractsStr = imgurl.toJson().encode();
        int length = contractsStr.getBytes().length;
        buffer.appendInt(length);
        buffer.appendString(contractsStr);
    }

    @Override
    public OriginURL decodeFromWire(int pos, Buffer buffer) {
        int position = pos;
        int length = buffer.getInt(position);
        String jsonStr = buffer.getString(position += 4, position += length);
        JsonObject json = new JsonObject(jsonStr);

        return new OriginURL(json);
    }

    @Override
    public OriginURL transform(OriginURL imgurl) {
        return imgurl;
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
