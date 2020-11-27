NetlixOSS is a good example of middleware capabilities for microservices which has seen a lot of
developer interest over the years. Furthermore, the influence of new technologies like Kubernetes and Istio on
the developer landscape is having similar impact on NetflixOSS which therefore is a useful exemplar of evolution.

Example copies (with permission) from https://github.com/kenfinnigan/ejm-samples which are examples for
the book Enterprise Java Microservices (https://www.manning.com/books/enterprise-java-microservices). Some files
edited so they will build from here without needing the rest of the original book's example code. Licence for this code
is as per the original repository.

----

cd hystrix-dashboard

mvn clean package
java -jar target/hystrix-dashboard-thorntail.jar

After the dashboard is started, open a browser and navigate to
http://localhost :8090/

Then add http://localhost:8080/hystrix.stream to stream monitor.

Then cd ../chapter8/stock-client

mvn thorntail:run

browser http://localhost:8080/single/AAPL

Then browser http://localhost:8080/single/AAPL/4

Then curl http://localhost:8080/single/AAPL/?[1-100]
