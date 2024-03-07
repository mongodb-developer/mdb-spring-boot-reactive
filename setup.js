
db=db.getSiblingDB("txn-demo");
db.dropDatabase();

db.createCollection("accounts",{
  "validator": {
     "$jsonSchema": {
       "bsonType": "object",
       "title": "Student Object Validation",
       "required": [
         "accountNum",
         "balance"
       ],
       "properties": {
         "balance": {
           "bsonType": "double",
           "minimum": 0,
           "description": "\'balance\' cannot be less than 0"
         }
       }
    }
  }
});

db.accounts.createIndex({"accountNum": 1}, {"unique": true});
