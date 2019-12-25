# About

This is microservice of our *IASA-KA-75-KA-76-Pharos-Production-Distributed-Systems* project called **SagasLife**. This part is responsible for scaling photos to different resolutions(by width).

# Data flows

The main operation is scaling original photo:



![Scaling](misc/img/PUT.png)

# Algorithm

Two main operations is put and delete photo. Both of them accept image name. Requests to this microservice are sent via Kafka topics. All of the topic names can be configured via `conf/config.json`. All errors are being reported to `Sagas` microservice via `sagasTopic` option in config file. 

There are two main topics `userpicsTopic` and `photosTopic` that are responsible for streaming two types of messages: `put:` and `del:` photo. The type of topic is needed to determine the type of AWS S3 bucket, that stores original photo to rescale and in which future scales will be stored.

# Examples

### Scaling new photo

Message in `userpicsTopic` : 

**put:pretty_woman.jpg**

Actions:

1. `put:` means this is scale request, `userpicsTopic` means, that this photo is stored in `userpicsBucket`.
2. Get original `pretty_woman.jpg` photo from AWS
3. Scale it to different sizes defined in config file.
4. Sent it to S3 adding suffixes defined in config. For example three new files will be created: `pretty_woman.jpg.sm`, `pretty_woman.jpg.md`, `pretty_woman.jpg.lg`, that means small, medium and large sizes respectively.
5. Report operation status to `Sagas`

### Deletion of scales

Message in `photosTopic` :  

**del:pretty_woman.jpg**

Actions:

1. `del:` means this is scale request, `photosTopic` means, that this photo is stored in `photosBucket`.
2. Sent removal request to AWS s3 adding suffixes defined in config. For example three files will be removed : `pretty_woman.jpg.sm`, `pretty_woman.jpg.md`, `pretty_woman.jpg.lg` that means small, medium and large sizes respectively.
3. Report operation status to `Sagas`

# Customization

All this settings defined in `conf/config.json` - Single Source Of Truth. Customize it to suit your needs, even without reloading the project.

1. `sizes`: add new sizes to scale into
2. `aws`
   - `photosBucket`: name of ordinary photos S3 bucket
   - `userpicsBucket`: name of userpics S3 bucket
   - `region`: region of bucket, like it is defined in aws links(e. g. `us-east-2`)
3. `extension`: image extension(e. g. `jpg`)
4. `kafka`
   - `host`
   - `port`
   - `photosTopic`: name of ordinary photos topic
   - `userpicsTopic`: name of userpics topic
   - `sagasTopic`: name of `Sagas` topic
   - `deleteRequest`: prefix of delete request(like `del:`) 
   - `scaleRequest`: prefix of scale request(like `put:`)
   
# Running

Use java 12. Add new configuration with such settings:
1. Main class: `scales.Launcher`
2. Program arguments: `run scales.verticles.MainVerticle -cluster`
3. Classpath of module: `photo-scale-microservice.scales.main`

Then provide such environment variables: `AWS_S3_ACCESS_KEY` and `AWS_S3_SECRET_KEY`, to use your AWS S3. All of these you could get as described [here](https://support.infinitewp.com/support/solutions/articles/212258-where-are-my-amazon-s3-credentials-).

For more info on configuring run, read [this](https://github.com/IASA-HUB/vertx-starter-pack/wiki/How-to-make-things-work) one.

# Running kafka

To run project you need to start kafka with configuration from `conf/config.json` file. Go to `misc/kafk..` and there are 5 `.sh` files:

1. `start1ZooKeeper.sh`
2. `start2Kafka.sh`
3. `createTopic.sh topicName`
4. `listenTopic.sh topicName`
5. `send2Topic.sh topicName`

Here is my normal usage of this commands(assuming topics are previously created):
1. `./start1ZooKeeper.sh`
2. `./start2Kafka.sh`
3. `./send2Topic.sh userpicsTopic`
4. `./send2Topic.sh photosTopic`
5. `./listenTopic.sh sagasTopic`



P. S. You'll need a whole lot of terminals(or a virtual terminals, like [terminator is](https://terminator-gtk3.readthedocs.io/en/latest/#). Highly recommend!)