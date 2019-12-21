package profiles.model;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.JsonObject;

/** Class for transfering config messages over EventBus */
public class OriginIDCodec implements MessageCodec<OriginID, OriginID> {

    @Override
    public void encodeToWire(Buffer buffer, OriginID imgurl) {
        String contractsStr = imgurl.toJson().encode();
        int length = contractsStr.getBytes().length;
        buffer.appendInt(length);
        buffer.appendString(contractsStr);
    }

    @Override
    public OriginID decodeFromWire(int pos, Buffer buffer) {
        int position = pos;
        int length = buffer.getInt(position);
        String jsonStr = buffer.getString(position += 4, position += length);
        JsonObject json = new JsonObject(jsonStr);

        return new OriginID(json);
    }

    @Override
    public OriginID transform(OriginID imgurl) {
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
