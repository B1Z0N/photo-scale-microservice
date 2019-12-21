package profiles.model;

import io.vertx.core.json.JsonObject;

import javax.annotation.Nonnull;

/**
 * Data class: representation of
 */
public class OriginID {

    // Constants

    private static final String PHOTO_ID = "photoID";
    private static final String PHOTO_TYPE = "photoType";

    // Variables

    private final JsonObject mJson;
    private final String mPhotoID;

    public enum photoType {
        PHOTO,
        USERPIC
    }

    ;
    private final photoType mType;

    // Constructors

    public OriginID(@Nonnull JsonObject json) throws AssertionError {
        mJson = json;

        mPhotoID = json.getString(PHOTO_ID);
        mType = photoTypeConversion(json.getString(PHOTO_TYPE));
    }

    // photoType conversion functions

    public String photoTypeConversion(photoType type) {
        if (type == photoType.PHOTO) {
            return "photo";
        } else {
            return "userpic";
        }
    }

    public photoType photoTypeConversion(String type) throws AssertionError {
        if (type == "photo") {
            return photoType.PHOTO;
        } else if (type == "userpic") {
            return photoType.USERPIC;
        } else {
            throw new AssertionError("Photo type should be either 'photo' or 'userpic'");
        }
    }

    public OriginID(@Nonnull String photoID, photoType type) throws AssertionError {
        mJson = new JsonObject()
                .put(PHOTO_ID, photoID)
                .put(PHOTO_TYPE, photoTypeConversion(type));
        mPhotoID = photoID;
        mType = type;
    }

    // Accessors

    JsonObject toJson() {
        return mJson;
    }

    public String getID() {
        return mPhotoID;
    }

    public photoType getType() {
        return mType;
    }

    public String getTypeString() {
        return photoTypeConversion(mType);
    }

    // Utils

    @Override
    public String toString() {
        return "OriginID {" +
                "mPhotoID = '" + mPhotoID.toString() + '\'' +
                "mType = '" + getTypeString() + '\'' +
                '}';
    }
}
