#!/bin/bash
#
# Copyright 2022 Yoshi Yamaguchi
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -ex

SERVICE="forwarder"
PROJECT=$(gcloud config get-value project)
INSTANCE=$(gcloud bigtable instances list --format="value(name)")
export KO_DOCKER_REPO="asia-south1-docker.pkg.dev/${PROJECT}/forwarder"

gcloud run deploy ${SERVICE} \
--image $(ko publish .) \
--platform=managed \
--allow-unauthenticated \
--no-cpu-throttling \
--set-env-vars "BT_PROJECT=${PROJECT}" \
--set-env-vars "BT_INSTANCE=${INSTANCE}"

URL=$(gcloud run services describe ${SERVICE} --format="value(status.address.url)")
curl -X GET ${URL}/_healthz
