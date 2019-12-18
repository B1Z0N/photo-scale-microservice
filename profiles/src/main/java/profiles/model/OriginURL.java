package profiles.model;

import io.vertx.core.json.JsonObject;

import javax.annotation.Nonnull;

/**
 * Data class: representation of profile info in plain java class
 */
public class OriginURL {

    // Constants

    private static final String PHOTO_ID = "photoID";
    private static final String URL = "URL";

    // Variables

    private final JsonObject mJson;
    private final Integer mPhotoID;
    private final String mURL;

    // Constructors

    public OriginURL(@Nonnull JsonObject json) {
        mJson = json;

        mPhotoID = json.getInteger(PHOTO_ID);
        mURL = json.getString(URL);
    }

    public OriginURL(@Nonnull Integer PhotoID, @Nonnull String _URL) {
        mJson = new JsonObject()
                .put(PHOTO_ID, PhotoID)
                .put(URL, _URL);
        mPhotoID = PhotoID;
        mURL = _URL;
    }

    // Accessors

    JsonObject toJson() {
        return mJson;
    }

    public Integer getPhotoID() {
        return mPhotoID;
    }

    public String getUrl() {
        return mURL;
    }

    // Utils

    @Override
    public String toString() {
        return "OriginURL {" +
                "mPhotoID = '" + mPhotoID.toString() + '\'' +
                "mURL = '" + mURL + '\'' +
                '}';
    }
}
