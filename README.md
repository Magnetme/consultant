[![Magnet.me Logo](https://cdn.magnet.me/images/logo-2015-full.svg)](https://magnet.me?ref=github-consultant "Discover the best companies, jobs and internships at Magnet.me")

# Consultant
###### Fetches your service's configuration from Consul, and subscribes to any changes.

## What's Consultant?
Consultant is a Java library which allows you service to retrieve its configuration from Consul's Key/Value store. In addition to this, Consultant subscribes to any changes relevant to your service. In addition to reading the configuration from a traditional `Properties` object, you can also use Consultant's integration with Netflix's Governator instead.

## How to use Consultant?
In order use Consultant, you'll have to create a `Consultant` object first. This can be done using a `Builder`:

```java
Consultant consultant = Consultant.builder()
    .identifyAs("oauth")
    .build();
```

With the `identifyAs()` method you tell Consultant the identity of your service. Using this identity the correct configuration can be fetched from Consul's Key/Value store. You must at the very least specify the service's name. You can also optionally specify the name of the datacenter where the service is running, the hostname of the machine the service is running on, and instance name to describe the role of this particular instance.

Alternative you can also define the this identity through environment variables:

| Environment variable | Corresponds to | Required |
|:---------------------|:---------------|:---------|
| SERVICE_NAME  | Name of the service | Yes |
| SERVICE_DC    | Name of the datacenter where the service is running | No |
| SERVICE_HOST  | The name of the host where this service is running on | No |
| SERVICE_INSTANCE | The name of this particular instance of the service | No |
 
### Specifying an alternative Consul address
Consultant defaults Consul's REST API address to `http://localhost:8500`. If you wish to specify an alternative address to Consul's REST API, you can do so by using the `Builder`:

```java
Consultant consultant = Consultant.builder()
    .identifyAs("oauth")
    .withConsulHost("http://some-other-host")
    .build();
```

Or alternatively you can also define the this through an environment variable:

| Environment variable | Corresponds to |
|:---------------------|:---------------|
| CONSUL_HOST  | Address of Consul's REST API |

### Validating configurations

If you wish to impose any kind of validation on configurations (before it's exposed to your service), you can solve this using the `Builder`:

```java
Consultant consultant = Consultant.builder()
    .identifyAs("oauth")
    .validateConfigWith((config) -> {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(config.getProperty("database.password")));
    })
    .build();
```

### Retrieving the configuration from Consultant

You can retrieve the current configuration from Consultant by calling the `getProperties()` on the `Consultant` class:

```java
Consultant consultant = Consultant.builder()
    .identifyAs("oauth")
    .build();

Properties properties = consultant.getProperties();
```

Note that this `Properties` object is effectively a singleton, and is updated in-place by Consultant at run-time.

### Listening for updates to the configuration

If you wish to be notified of updates to the configuration you can specify a callback in the `Builder`:

```java
Consultant consultant = Consultant.builder()
    .identifyAs("oauth")
    .onValidConfig((config) -> {
        log.info("Yay, there's a new config available!");
    })
    .build();
```

## Licensing

Consultant is available under the Apache 2 License, and is provided as is.
