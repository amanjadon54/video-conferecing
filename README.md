# Meetup
 is a live video-based meeting between two or more people in different locations using video-enabled devices. Video conferencing allows multiple people to meet and collaborate face to face long distance by transmitting audio, video, text and presentations in real time through the internet.
## Installation:

Install and configure/run the webRTC server.

``` docker pull kurento/kurento-media-server:latest ```

Once the above image is pulled, expose the ports and run it
```sh
docker run --rm -p 8888:8888/tcp
-p 5000-5050:5000-5050/udp
-e KMS_MIN_PORT=5000
-e KMS_MAX_PORT=5050
kurento/kurento-media-server:latest
```

In case you are not familiar with docker, you can configure the above WebRTC server locally as well by following the steps mentioned [here](https://doc-kurento.readthedocs.io/en/stable/user/installation.html#running).

Run this project
```
mvn -U clean spring-boot:run -Dspring-boot.run.jvmArguments="-Dkms.url=ws://{KMS_HOST}:8888/kurento"
```
We can ignore passing the kms.url if the server and application running on the same machine. In other cases, just provide the host and port of the WebRTC server.

## Development
Want to contribute? Great! Feel free to raise PR.
