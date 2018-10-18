FROM alpine/git:1.0.4

ADD . /origin
WORKDIR /origin

ENTRYPOINT ["/origin/dockerfiles/merge/merge.sh"]
