db.Customer.updateMany({"settings.minimumNewHydrantDistance" : {$exists : 0}},
    {$set: {"settings.minimumNewHydrantDistance" : 100}});
