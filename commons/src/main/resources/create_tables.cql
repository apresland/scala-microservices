CREATE KEYSPACE IF NOT EXISTS twitter WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '1'};

CREATE TABLE IF NOT EXISTS twitter.tweets (
    id text,
    content text,
    PRIMARY KEY (id)
);


