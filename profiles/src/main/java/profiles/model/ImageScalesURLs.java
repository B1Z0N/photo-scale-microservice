package profiles.model;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Data class: representation of profile info in plain java class
 */
public class ImageScalesURLs {

    // Constants

    private static final String PHOTO_ID = "photoID";
    private static final String URLs = "URLs";
    private static final String WIDTH = "width";
    private static final String SCALE_URL = "URL";

    // Variables

    private final JsonObject mJson;
    private final Integer mPhotoID;
    private final HashMap<Integer, String> mWidthURLs;

    // Constructors

    ImageScalesURLs(@Nonnull JsonObject json) {
        mJson = json;

        mPhotoID = json.getInteger(PHOTO_ID);

        mWidthURLs = fromJsonWidthURLs(json.getJsonArray(URLs));
    }

    public ImageScalesURLs(@Nonnull Integer PhotoID, @Nonnull HashMap<Integer, String> WidthURLs) {
        mJson = new JsonObject()
                .put(PHOTO_ID, PhotoID)
                .put(URLs, toJsonWidthURLs(WidthURLs));
        mPhotoID = PhotoID;
        mWidthURLs = WidthURLs;
    }

    // Accessors

    JsonObject toJson() {
        return mJson;
    }

    public Integer getPhotoID() {
        return mPhotoID;
    }

    public HashMap<Integer, String> getWidthURLs() {
        return mWidthURLs;
    }

    // Utils

    @Override
    public String toString() {
        return "ImageScalesURLs {" +
                "mPhotoID = '" + mPhotoID.toString() + '\'' +
                "mWidthURLs = '" + mWidthURLs.toString() + '\'' +
                '}';
    }

    private HashMap<Integer, String> fromJsonWidthURLs(@Nonnull JsonArray jarr) {
        HashMap<Integer, String> map = new HashMap<>();
        for (int i = 0; i < jarr.size(); i++) {
            JsonObject current = jarr.getJsonObject(i);
            map.put(current.getInteger(WIDTH), current.getString(SCALE_URL));
        }

        return map;
    }

    private JsonArray toJsonWidthURLs(@Nonnull HashMap<Integer, String> map) {
        JsonArray jarr = new JsonArray();
        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            jarr.add(
                    new JsonObject().put(
                            pair.getKey().toString(),
                            pair.getValue().toString()
                    ));
            it.remove(); // avoids a ConcurrentModificationException
        }

        return jarr;
    }
}
