FROM java:8-jdk-alpine

COPY app /app
COPY run.sh /run.sh

RUN chmod +x /run.sh \
  && dos2unix -u /run.sh

ENTRYPOINT ["sh", "/run.sh"]