FROM docker.elastic.co/logstash/logstash:6.1.3
RUN rm -f /usr/share/logstash/pipeline/logstash.conf
ADD pipeline/ /usr/share/logstash/pipeline/
