#!/bin/bash
export IP=`kubectl get service apm-elastic-customer-api-lb -o=jsonpath='{.status.loadBalancer.ingress[0].ip}{"\n"}'`

echo "Using IP as $IP"
echo ""

while :
do
	echo "Press [CTRL+C] to stop.."
        http http://$IP/customers
        http http://$IP/customers/1
        http http://$IP/customers/2
        http http://$IP/customers/sgwhwh
        http "http://$IP/customers?page=0&size=2"
        http POST http://$IP/customers < new-customer-error.json

	sleep 5
done