# Reporting API sample

This project is to demonstrate end-to-end process to receive reports from [Reporting API](https://developer.mozilla.org/en-US/docs/Web/API/Reporting_API), to aggregate the data into constellations, and to detect the web frontend issues out of them.

## Components

* [forwarder](./forwarder/): a simple Go application that runs web endpoint to receive JSON formatted reports from Rerporting API and store them into a BigTable cluster
* [beam-collector](./beam-collector/): a Apache Beam project to aggregate raw data to meaningful constellations
* [redash](./redash/): a demo Redash configuration to view a single issue from the constellations

## How to setup

### Prerequisite

1. A Google Cloud project linked with a billing account:

* [Cloud Run](https://cloud.google.com/run)
* [Cloud Bigtable](https://cloud.google.com/bigtable)
* [Cloud Dataflow](https://cloud.google.com/dataflow)
* [Artifact Registry](https://cloud.google.com/artifact-registry)

1. [Go](https://go.dev/)
1. [ko](https://github.com/ko-build/ko)

### Set up

#### Enable all services

```console
gcloud services enable bigtable.googleapis.com
gcloud services enable bigtableadmin.googleapis.com
gcloud services enable artifactregistry.googleapis.com
gcloud services enable run.googleapis.com
gcloud services enable dataflow.googleapis.com
```

#### BigTable

This sample project expects the following BigTable schema.

* table name: security_report
* column family: description

The following `gcloud` commands let you create a new BigTable instance and
a table in it.

```console
gcloud bigtable instances create reporting-api-instance \
    --display-name="Reporting API Instance" \
    --cluster-config=id=reporting-api-cluster,zone=asia-east1-b

gcloud bigtable instances tables create security_report \
    --instance=my-instance --column-families="description"
```

#### Deploy report forwarder

The sample report forwarder in `/forwarder` directory is implemented in Go. The application is expected to run on Cloud Run. The temporary artifact is built and uploaded to Artifact Registry.

First we create a container registry.

```console
gcloud artifacts repositories create test-registry \
    --repository-format=docker \
    --location=asia-east1
```

Now you are able to build this Go project with the tool `ko`.

```console
gcloud config set compute/zone asia-east1-b
gcloud config set run/region asia-east1
export KO_DOCKER_REPO=asia-east1-docker.pkg.dev/stargazing-testing/test-registry
cd forwarder
./deploy.sh
```

You should observe logs similar to the followings:

```console
$ ./deploy.sh
+ SERVICE=forwarder
++ gcloud config get-value project
Your active configuration is: [reporting-api]
+ PROJECT=sample-project
++ gcloud bigtable instances list '--format=value(name)'
+ INSTANCE=reporting-api-instance
++ ko publish .
2023/06/05 14:55:06 Using base gcr.io/distroless/base-debian11@sha256:73deaaf6a207c1a33850257ba74e0f196bc418636cada9943a03d7abea980d6d for github.com/GoogleCloudPlatform/reporting-api-processor/forwarder
...
Deploying container to Cloud Run service [forwarder] in project [sample-project] region [asia-east1]
✓ Deploying... Done.
  ✓ Creating Revision...
  ✓ Routing traffic...
  ✓ Setting IAM Policy...
Done.
Service [forwarder] revision [forwarder-00003-voy] has been deployed and is serving 100 percent of traffic.
Service URL: https://forwarder-12345abcde-de.a.run.app
++ gcloud run services describe forwarder '--format=value(status.address.url)'
+ URL=https://forwarder-12345abcde-de.a.run.app
+ curl -X GET https://forwarder-12345abcde-de.a.run.app/_healthz
OK
```
