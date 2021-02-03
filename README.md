# Elastic Spring Customer RESTful - APM Demo

This GitHub demo is a Spring Boot application which accesses relational database with Spring Data JPA through a hypermedia-based RESTful front end. You can use the repo instructions to deploy to K8s and inject automatically an Elastic Agent for APM Monitoring with Elastic Observability.

There is no code changes required simply use an Init Container on K8s to instrument your application on Elastic APM server as shown by this GitHub repo below.

## Prerequisites

* A K8s cluster (I use GKE in this demo)
* Elastic Cloud (ESS) instance with APM

## Steps

- Ensure you are connected to your K8s cluster as shown below

```bash
$ kubectl get nodes
NAME                                     STATUS   ROLES    AGE   VERSION
gke-eck-pas-default-pool-1b49f53c-p6vv   Ready    <none>   19d   v1.16.15-gke.6000
gke-eck-pas-default-pool-42e4d52d-f5df   Ready    <none>   19d   v1.16.15-gke.6000
gke-eck-pas-default-pool-ca35a196-ghm2   Ready    <none>   19d   v1.16.15-gke.6000
```

- This application has been created as a fully compliant OCI image and loaded into Dockerhub registry which means you can deploy this from the location below

![alt tag](https://i.ibb.co/rmMHQmK/K8s-init-apm-2.png)

```text
Image: pasapples/elastic-customer-api
```

-  Create a secret and config map in your K8s cluster to hold the APM server URL and your APM token which you can retrieve from Elastic Cloud as shown below> be sure you replace APM_TOKEN and APM_URL with your own details

APM_TOKEN and APM_URL can be retried from Elastic Cloud (ESS) clicking on APM for your deployment

![alt tag](https://i.ibb.co/60Q6Pg2/K8s-init-apm-1.png)

```bash
$ kubectl create secret generic apm-token-secret --from-literal=secret_token=APM_TOKEN

$ kubectl create configmap apm-agent-details --from-literal=server_urls=APM_URL
```

- Deploy the application to K8s using the deployment YAML below. It exists in this repo as "**deploy-with-apm.yml**"

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: apm-elastic-customer-api
spec:
  selector:
    matchLabels:
      app: apm-elastic-customer-api
  replicas: 1
  template:
    metadata:
      labels:
        app: apm-elastic-customer-api
    spec:
      volumes:
        - name: elastic-apm-agent
          emptyDir: { }
      initContainers:
        - name: elastic-java-agent
          image: docker.elastic.co/observability/apm-agent-java:1.20.0
          volumeMounts:
            - mountPath: /elastic/apm/agent
              name: elastic-apm-agent
          command: [ 'cp', '-v', '/usr/agent/elastic-apm-agent.jar', '/elastic/apm/agent' ]
      containers:
        - name: apm-elastic-customer-api
          image: pasapples/elastic-customer-api:1.0
          volumeMounts:
            - mountPath: /elastic/apm/agent
              name: elastic-apm-agent
          imagePullPolicy: Always
          ports:
            - containerPort: 8080
          env:
            - name: ELASTIC_APM_ENABLE_LOG_CORRELATION
              value: "true"
            - name: ELASTIC_APM_CAPTURE_JMX_METRICS
              value: >-
                object_name[java.lang:type=GarbageCollector,name=*] attribute[CollectionCount:metric_name=collection_count] attribute[CollectionTime:metric_name=collection_time],
                object_name[java.lang:type=Memory] attribute[HeapMemoryUsage:metric_name=heap]
            - name: ELASTIC_APM_SERVER_URLS
              valueFrom:
                configMapKeyRef:
                  name: apm-agent-details
                  key: server_urls
            - name: ELASTIC_APM_SERVICE_NAME
              value: "elastic-customer-api-rest"
            - name: ELASTIC_APM_APPLICATION_PACKAGES
              value: "com.example.demo"
            - name: ELASTIC_APM_SECRET_TOKEN
              valueFrom:
                secretKeyRef:
                  name: apm-token-secret
                  key: secret_token
            - name: JAVA_TOOL_OPTIONS
              value: -javaagent:/elastic/apm/agent/elastic-apm-agent.jar

---
apiVersion: v1
kind: Service
metadata:
  name: apm-elastic-customer-api-lb
  labels:
    name: apm-elastic-customer-api-lb
spec:
  ports:
    - port: 80
      targetPort: 8080
      protocol: TCP
  selector:
    app: apm-elastic-customer-api
  type: LoadBalancer
```

Deploy command:

```bash
$ kubectl apply -f deploy-with-apm.yml
```

- Verify everything is up and running. You may need to wait a minute or two for the POD to be in a running state

```bash 
$ kubectl get all
NAME                                            READY   STATUS    RESTARTS   AGE
pod/apm-elastic-customer-api-589df89779-7mdlg   1/1     Running   0          4h11m

NAME                                  TYPE           CLUSTER-IP     EXTERNAL-IP     PORT(S)        AGE
service/apm-elastic-customer-api-lb   LoadBalancer   10.136.7.174   35.244.119.25   80:31441/TCP   6h50m
service/kubernetes                    ClusterIP      10.136.0.1     <none>          443/TCP        19d

NAME                                       READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/apm-elastic-customer-api   1/1     1            1           6h50m

NAME                                                  DESIRED   CURRENT   READY   AGE
replicaset.apps/apm-elastic-customer-api-589df89779   1         1         1       4h11m
```

- Check the POD logs to ensure it has connected with APM successfully

```bash
$ kubectl logs apm-elastic-customer-api-589df89779-7mdlg
Picked up JAVA_TOOL_OPTIONS: -javaagent:/elastic/apm/agent/elastic-apm-agent.jar
WARNING: sun.reflect.Reflection.getCallerClass is not supported. This will impact performance.
2021-02-02 05:45:19,607 [main] INFO  co.elastic.apm.agent.util.JmxUtils - Found JVM-specific OperatingSystemMXBean interface: com.sun.management.OperatingSystemMXBean
2021-02-02 05:45:19,700 [main] INFO  co.elastic.apm.agent.configuration.StartupInfo - Starting Elastic APM 1.20.0 as elastic-customer-api-rest on Java 11.0.10 Runtime version: 11.0.10+9 VM version: 11.0.10+9 (AdoptOpenJDK) Linux 4.19.112+
2021-02-02 05:45:19,701 [main] INFO  co.elastic.apm.agent.configuration.StartupInfo - VM Arguments: [-javaagent:/elastic/apm/agent/elastic-apm-agent.jar]
2021-02-02 05:45:22,201 [main] INFO  co.elastic.apm.agent.impl.ElasticApmTracer - Tracer switched to RUNNING state
2021-02-02 05:45:23,184 [elastic-apm-server-healthcheck] INFO  co.elastic.apm.agent.report.ApmServerHealthChecker - Elastic APM server is available: {  "build_date": "2021-01-12T21:51:32Z",  "build_sha": "42a349a4ec9d2dd16e08b8af125647294e7a7e4b",  "version": "7.10.2"}
2021-02-02 05:45:23,200 [elastic-apm-remote-config-poller] INFO  co.elastic.apm.agent.configuration.ApmServerConfigurationSource - Received new configuration from APM Server: {}

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v2.4.2)

...

```
- Generate some traffic by running the script "generate-traffic.sh" below. 

generate-traffic.sh

```bash
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
```

Note: This script will continue running and accessing the application. Leave it running so that data can flow into APM

- Head to APM to view application traces, service map, errors, database calls and more 

![alt tag](https://i.ibb.co/2YN8bsW/K8s-init-apm-3.png)

![alt tag](https://i.ibb.co/gJRS1Sz/K8s-init-apm-4.png)

![alt tag](https://i.ibb.co/8sTYYRn/K8s-init-apm-5.png)

![alt tag](https://i.ibb.co/vh6Jncc/K8s-init-apm-6.png)

![alt tag](https://i.ibb.co/60KXkPk/K8s-init-apm-7.png)

![alt tag](https://i.ibb.co/9s1GwWV/K8s-init-apm-8.png)

![alt tag](https://i.ibb.co/frJKNW5/K8s-init-apm-9.png)

![alt tag](https://i.ibb.co/5vNd4CQ/K8s-init-apm-10.png)

<hr />
Pas Apicella [pas.apicella at elastic.co] is an Solution Architect at Elastic APJ  