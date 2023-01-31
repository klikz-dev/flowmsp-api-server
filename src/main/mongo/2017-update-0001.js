db.Customer.updateMany({"settings" : {$exists : 0}},
                       {$set: {"settings.preplanningAreaRounding"                : 100,
                               "settings.preplanningMaxHydrants"                 : 10,
                               "settings.preplanningMaxAreaForFlowComputation"   : 10000,
                               "settings.preplanningMaxDistanceForHydrantSearch" : 5000}});
