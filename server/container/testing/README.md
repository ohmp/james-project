# James deployment validation

## Validating Cassandra guice deployment

The test class is **CassandraGuiceDeploymentTest**.

The default image being used is **linagora/james-project:latest** but can be overriden by the **CASSANDRA_GUICE_IMAGE** environment variable.

Validated protocols: WebAdmin, SMTP, IMAP, JMAP

## Validating JPA guice deployment

The test class is **JpaGuiceDeploymentTest**.

The default image being used is **linagora/james-jpa-guice:latest** but can be overriden by the **JPA_GUICE_IMAGE** environment variable.

Validated protocols: WebAdmin, SMTP, IMAP

## Validating JPA sample deployment

The test class is **JpaSampleDeploymentTest**.

The default image being used is **linagora/james-jpa-sample:latest** but can be overriden by the **JPA_SAMPLE_IMAGE** environment variable.

Validated protocols: WebAdmin, SMTP, IMAP

## Validating Spring deployment

The test class is **SpringDeploymentTest**.

The default image being used is specified by the **SPRING_IMAGE** environment variable.

Validated protocols: SMTP, IMAP
