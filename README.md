# Reporting API sample

This project is to demonstrate end-to-end process to receive reports from [Reporting API](https://developer.mozilla.org/en-US/docs/Web/API/Reporting_API), to aggregate the data into constellations, and to detect the web frontend issues out of them.

## Components

* [forwarder](./forwarder/): a simple Go application that runs web endpoint to receive JSON formatted reports from Rerporting API and store them into a BigTable cluster
* [beam-collector](./beam-collector/): a Apache Beam project to aggregate raw data to meaningful constellations
* [redash](./redash/): a demo Redash configuration to view a single issue from the constellations

## How to setup

### Prerequisite

1. [Terraform](https://www.terraform.io/)
1. A Google Cloud project linked with a billing account:
  * [Cloud Run](https://cloud.google.com/run)
  * [Cloud Bigtable](https://cloud.google.com/bigtable)
  * [Cloud Dataflow](https://cloud.google.com/dataflow)
  * [Artifact Registry](https://cloud.google.com/artifact-registry)
1. [Go](https://go.dev/)
1. [ko](https://github.com/ko-build/ko)
