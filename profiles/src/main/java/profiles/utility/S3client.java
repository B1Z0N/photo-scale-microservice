package profiles.utility;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class S3client {

    private static AmazonS3 mClient;

    public S3client(String accessKey, String secretKey, Regions region) {
        AWSCredentials credentials = new BasicAWSCredentials(
                accessKey,
                secretKey
        );
        mClient = AmazonS3ClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(region)
                .build();
    }

    public void listBuckets() {
        List<Bucket> buckets = mClient.listBuckets();
        for (Bucket bucket : buckets) {
            System.out.println(bucket.getName());
        }
    }

    public void listFiles(String bucketName) {
        ObjectListing objectListing = mClient.listObjects(bucketName);
        for(S3ObjectSummary os : objectListing.getObjectSummaries()) {
            System.out.println(os.getKey());
        }
    }

    public PutObjectResult upload(String bucketName, File orig, String destName) {
        return mClient.putObject(
                bucketName,
                destName,
                orig
        );
    }

    public File download(String bucketName, String origin) throws IOException {
        S3Object s3object = mClient.getObject(bucketName, origin);
        S3ObjectInputStream inputStream = s3object.getObjectContent();

        File file = File.createTempFile("aws", "s3");
        FileUtils.copyInputStreamToFile(inputStream, file);

        return file;
    }

    public void delete(String bucketName, String path) {
        mClient.deleteObject(bucketName, path);
    }

    public void multiDelete(String bucketName, ArrayList<String> pathes) {
        DeleteObjectsRequest delObjReq = new DeleteObjectsRequest(bucketName)
                .withKeys(pathes.toArray(new String[]{}));
        mClient.deleteObjects(delObjReq);
    }
}
