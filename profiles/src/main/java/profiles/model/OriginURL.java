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
    private static final String PHOTO_TYPE = "photoType";

    // Variables

    private final JsonObject mJson;
    private final Integer mPhotoID;
    private final String mURL;
    public enum photoType {
        PHOTO,
        USERPIC
    };
    private final photoType mType;

    // Constructors

    public OriginURL(@Nonnull JsonObject json) throws AssertionError {
        mJson = json;

        mPhotoID = json.getInteger(PHOTO_ID);
        mURL = json.getString(URL);
        String type = json.getString(PHOTO_TYPE);
        if (type == "photo") {
            mType = photoType.PHOTO;
        } else if (type == "userpic") {
            mType = photoType.USERPIC;
        } else {
            throw new AssertionError("Photo type should be either 'photo' or 'userpic'");
        }
    }

    public OriginURL(@Nonnull Integer PhotoID, @Nonnull String _URL, photoType type) {
        mJson = new JsonObject()
                .put(PHOTO_ID, PhotoID)
                .put(URL, _URL);
        mPhotoID = PhotoID;
        mURL = _URL;
        mType = type;
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

    public photoType getType() {
        return mType;
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
