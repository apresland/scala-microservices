CREATE KEYSPACE IF NOT EXISTS twitter WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '1'};

CREATE TABLE IF NOT EXISTS twitter.tweets (
  id text,
  date timestamp,
  user text,
  content text,
  PRIMARY KEY (id, date)
) WITH CLUSTERING ORDER BY (date DESC);

CREATE TABLE IF NOT EXISTS twitter.sentiments (
  date timestamp,
  hostile int,
  negative int,
  neutral int,
  positive int,
  PRIMARY KEY (hostile, date)
) WITH CLUSTERING ORDER BY (date DESC);

CREATE TABLE IF NOT EXISTS twitter.statistics (
  id text,
  date timestamp,
  user text,
  PRIMARY KEY (user, date)
) WITH CLUSTERING ORDER BY (date DESC);

CREATE TABLE IF NOT EXISTS twitter.hashtags (
  hashtag text,
  date timestamp,
  counts int,
  PRIMARY KEY (hashtag, date)
) WITH CLUSTERING ORDER BY (date DESC);

