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


