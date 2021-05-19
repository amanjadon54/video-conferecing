# video-conferencing

Steps to run:
1. Install and configgure/run the webRTC server.

    docker pull kurento/kurento-media-server:latest


2. Once the above image is pulled, run it with exposed ports for netowrking & connection
    docker run --rm \
        -p 8888:8888/tcp \
        -p 5000-5050:5000-5050/udp \
        -e KMS_MIN_PORT=5000 \
        -e KMS_MAX_PORT=5050 \
        kurento/kurento-media-server:latest

    In case you are not familiar with docker, you can configure the above server locally as well by following the steps mentioned here:
    https://doc-kurento.readthedocs.io/en/stable/user/installation.html#running

3. Run this project
    mvn -U clean spring-boot:run \
        -Dspring-boot.run.jvmArguments="-Dkms.url=ws://{KMS_HOST}:8888/kurento"

     We can ignore passing the kms.url if the server and appliaction running on the same machine. In other cases, just provide
     the host and port of the WebRTC server.