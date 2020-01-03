//
// Login as the default administrator using the default authentication database
// mongo --username "root" --password "password" --authenticationDatabase "admin" admin < mongodb.js
//
use resource-server;

db.dropDatabase();

db.dropUser("resourceadmin");
db.createUser({user: "resourceadmin", pwd: "password", roles: ["readWrite", "dbAdmin"]});

db.createCollection("credentials");
db.credentials.createIndex({"uid": 1});
db.credentials.createIndex({"data.owner": 1}, {unique: true});
db.credentials.insert({"comment": "This is a test document"});

db.createCollection("resources");
db.resources.createIndex({"uid": 1});
db.resources.createIndex({"data.owner": 1});
db.resources.createIndex({"data.register": 1});
db.resources.insert({"comment": "This is a test document"});

//
// Login as the administrator for the application database
// mongo --username "resourceadmin" --password "password" --authenticationDatabase "resource-server" resource-server
//
// show the collections in the current database
show collections;
// find all the documents in the collection
db.credentials.find();
db.credentials.find().pretty();
db.resources.find();
db.resources.find().pretty();
