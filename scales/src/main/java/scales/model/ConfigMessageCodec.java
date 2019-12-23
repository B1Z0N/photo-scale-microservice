package scales.model;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.JsonObject;

/**
 * Class for transfering config messages over EventBus
 */
public class ConfigMessageCodec implements MessageCodec<Config, Config> {

    @Override
    public void encodeToWire(Buffer buffer, Config config) {
        String contractsStr = config.toJson()
                .encode();
        int length = contractsStr.getBytes().length;
        buffer.appendInt(length);
        buffer.appendString(contractsStr);
    }

    @Override
    public Config decodeFromWire(int pos, Buffer buffer) {
        int position = pos;
        int length = buffer.getInt(position);
        String jsonStr = buffer.getString(position += 4, position + length);
        JsonObject json = new JsonObject(jsonStr);

        return new Config(json);
    }

    @Override
    public Config transform(Config config) {
        return config;
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
