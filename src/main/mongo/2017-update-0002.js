function updateUsersForCustomer(customer) {
	collectionString = "db." + customer.slug + ".User";
	print("Updating collection " + collectionString);
	collection = eval(collectionString);
	collection.find({"customerRef":{$exists:0}}).forEach (
		function(user) { 
	    	print(user._id);
	    	collection.updateOne({_id:user._id}, {$set:{'customerRef.customerId':user.customerId, 
	    	                                            'customerRef.customerSlug':user.customerSlug,
	    	                                            'customerRef.customerName':user.customerName},
	    	                                      $unset:{'customerId':'',
	    	                                              'customerSlug':'',
	    	                                      		  'customerName':''}});
		}
	)
}

db.Customer.find({}).forEach(
	function (customer) {
		updateUsersForCustomer(customer);
	}
)