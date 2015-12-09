FROM java:8
MAINTAINER Gregor Uhlenheuer <kongo2002@gmail.com>

# get scala + sbt
RUN wget -nv "http://downloads.typesafe.com/scala/2.11.7/scala-2.11.7.deb" && \
    dpkg -i scala-2.11.7.deb && \
    apt-get update && \
    apt-get install scala && \
    rm scala-2.11.7.deb && \
    \
    wget -nv "https://dl.bintray.com/sbt/debian/sbt-0.13.9.deb" && \
    dpkg -i sbt-0.13.9.deb && \
    apt-get update && \
    apt-get install sbt && \
    rm sbt-0.13.9.deb && \
    \
    sbt version

# load applications from the /app folder
# to be used like: 'docker run -v $(pwd):/app ...'
VOLUME "/app"
WORKDIR "/app"

ADD build.sbt /app/build.sbt
RUN cd /app && sbt test:compile && rm build.sbt

ENTRYPOINT ["sbt"]
CMD ["version"]
