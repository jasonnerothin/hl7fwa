#!/bin/bash -x

docker run --name posttest -d -p 5432:5432 -e POSTGRES_PASSWORD=password postgres:alpine

# docker exec -it ece419bee3cb /bin/bash
# psql -U postgres

# CREATE DATABASE claimsdb ;
# \c claimsdb
CREATE TABLE claims (
    id INT PRIMARY KEY
    , patient_id INTEGER NOT NULL
    , amount DECIMAL NOT NULL
    , created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
)
;