package icon.xcall.lib.util;

import com.eclipsesource.json.JsonArray;

public class XCallMessage {
    public JsonArray data = new JsonArray();

    public byte[] toBytes() {
        return data.toString().getBytes();
    }
}