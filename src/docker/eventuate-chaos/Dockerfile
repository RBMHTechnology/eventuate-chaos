FROM java:8

# load applications from the /app folder
# to be used like: 'docker run -v $(pwd):/app ...'
VOLUME "/app"
WORKDIR "/app/target/universal/stage"

ENTRYPOINT ["bin/eventuate-chaos"]
CMD ["-h"]
