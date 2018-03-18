CREATE KEYSPACE IF NOT EXISTS twitter WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '1'};

CREATE TABLE IF NOT EXISTS twitter.tweets (
  id text,
  time timestamp,
  user text,
  content text,
  PRIMARY KEY (id, time)
);

CREATE TABLE IF NOT EXISTS twitter.metrics (
  id text,
  time timestamp,
  sentiment int,
  PRIMARY KEY (id, time)
);

CREATE TABLE IF NOT EXISTS twitter.sentiments (
  time timestamp,
  hostile int,
  negative int,
  neutral int,
  positive int,
  PRIMARY KEY (hostile, time)
) WITH CLUSTERING ORDER BY (time DESC);

CREATE TABLE IF NOT EXISTS twitter.hashtags (
  id timeuuid,
  time timestamp,
  tag0 tuple<text,int>,
  tag1 tuple<text,int>,
  tag2 tuple<text,int>,
  tag3 tuple<text,int>,
  tag4 tuple<text,int>,
  tag5 tuple<text,int>,
  tag6 tuple<text,int>,
  tag7 tuple<text,int>,
  tag8 tuple<text,int>,
  tag9 tuple<text,int>,
  PRIMARY KEY (id, time)
) WITH CLUSTERING ORDER BY (time DESC);



